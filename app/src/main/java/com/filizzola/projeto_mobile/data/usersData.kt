package com.filizzola.projeto_mobile.data

import android.util.Log
import com.filizzola.projeto_mobile.supabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String,
    val passwordHash: String? = null,
    var uTaskList: ArrayList<Tarefa>
)

@Serializable
data class TarefaSupabase(
    val id: String,
    val titulo: String,
    val status: String,
    val desc: String? = null,
    val userId: String // Chave estrangeira para associar a tarefa ao usuário
)

object UserRepository {
    val allUsers: MutableList<User> = mutableListOf()
    private val gson = Gson()

    private suspend fun fetchTasksForUser(userId: String): ArrayList<Tarefa> {
        val tasksTag = "FetchTasks"
        try {
            val tasksFromSupabase = supabase.from("Tasks").select() {
                filter {
                    eq("userId", userId)
                }
            }.decodeList<Tarefa>()
            Log.d(tasksTag, "Tarefas buscadas com sucesso para o usuário $userId: ${tasksFromSupabase.size} tarefas encontradas.")
            return ArrayList(tasksFromSupabase)
        } catch (e: Exception) {
            Log.e(tasksTag, "Erro ao buscar tarefas para o usuário $userId no Supabase.", e)
            return arrayListOf()
        }
    }

    // --- NOVA FUNÇÃO DE SYNC (ESSENCIAL PARA OFFLINE -> ONLINE) ---
    suspend fun syncUserData(userId: String): List<Tarefa>? {
        val syncTag = "SyncProcess"
        val debugTag = "SupabaseDebug" // Tag para filtrar no Logcat
        try {
            // 1. Pega tarefas que estão na memória local (carregadas do disco pelo MainActivity)
            val user = allUsers.find { it.id == userId }
            val localTasks = user?.uTaskList ?: arrayListOf()

            // 2. Envia (Sobe) as tarefas locais para o Supabase usando UPSERT
            // Upsert atualiza se o ID já existe ou cria se não existe.
            if (localTasks.isNotEmpty()) {
                val tasksForSupabase = localTasks.map { task ->
                    TarefaSupabase(
                        id = task.id,
                        titulo = task.titulo,
                        desc = task.desc,
                        status = task.status,
                        userId = userId
                    )
                }

                Log.d(debugTag, "SYNC: Tentando fazer UPSERT de ${tasksForSupabase.size} tarefas: $tasksForSupabase")

                // onConflict="id" garante que não duplique
                supabase.from("Tasks").upsert(tasksForSupabase) {
                    onConflict = "id"
                }
                Log.d(syncTag, "SYNC: Upload de tarefas locais realizado com sucesso.")
                Log.d(debugTag, "SYNC: Sucesso no UPSERT.")
            } else {
                Log.d(debugTag, "SYNC: Nenhuma tarefa local para enviar.")
            }

            // 3. Baixa a versão oficial do servidor (que pode ter atualizações de outros lugares)
            Log.d(debugTag, "SYNC: Baixando dados remotos (Pull)...")
            val remoteTasks = fetchTasksForUser(userId)

            // 4. Atualiza a memória RAM com a versão final mesclada
            val updatedUser = User(
                id = userId,
                username = "Usuário Sincronizado",
                email = "",
                uTaskList = remoteTasks
            )

            val existingUserIndex = allUsers.indexOfFirst { it.id == userId }
            if (existingUserIndex != -1) {
                allUsers[existingUserIndex] = updatedUser
            } else {
                allUsers.add(updatedUser)
            }

            Log.d(syncTag, "Sincronização completa. Retornando lista atualizada.")
            return remoteTasks // Retorna a lista para ser salva no disco

        } catch (e: Exception) {
            Log.e(syncTag, "Erro no Sync (Provavelmente sem internet): ${e.message}")
            Log.e(debugTag, "SYNC FALHOU: ${e.message}", e)
            return null // Retorna null para indicar falha na conexão
        }
    }

    suspend fun login(email: String, password: String): User? {
        val loginTag = "LoginProcess"
        try {
            // 1. Autentica o usuário no Supabase
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val session = supabase.auth.currentSessionOrNull()
            if (session?.user != null) {
                val userId = session.user!!.id
                val userTasks = fetchTasksForUser(userId)
                val authenticatedUser = User(
                    id = userId,
                    username = session.user!!.userMetadata?.get("username")?.toString()?.removeSurrounding("\"") ?: "Usuário",
                    email = email,
                    uTaskList = userTasks
                )

                val existingUserIndex = allUsers.indexOfFirst { it.id == userId }
                if (existingUserIndex != -1) {
                    allUsers[existingUserIndex] = authenticatedUser
                } else {
                    allUsers.add(authenticatedUser)
                }

                Log.d(loginTag, "Login bem-sucedido e tarefas carregadas para o usuário: ${authenticatedUser.username}")
                return authenticatedUser
            } else {
                Log.e(loginTag, "Login falhou: sessão do Supabase não encontrada após a autenticação.")
                return null
            }
        } catch (e: Exception) {
            Log.e(loginTag, "Login Falhou. Erro do Supabase: ${e.message}", e)
            return null
        }
    }

    suspend fun logout() {
        val uId = supabase.auth.currentSessionOrNull()?.user?.id ?: "Desconhecido"
        try {
            supabase.auth.signOut()
            Log.d("LogoutProcess", "Usuário ${uId} deslogado com sucesso.")
        } catch (e: Exception) {
            Log.e("LogoutProcess", "Erro ao deslogar (provavelmente offline).", e)
        }
    }

    // --- MÉTODOS DE AÇÃO (ATUALIZADOS PARA LOCAL FIRST) ---

    suspend fun toggleTaskStatusForUser(userId: String, taskId: String) {
        val user = allUsers.find { it.id == userId } ?: return
        val task = user.uTaskList.find { it.id == taskId } ?: return

        val newStatus = if (task.status == "A fazer") "Feito" else "A fazer"
        val updatedTask = task.copy(status = newStatus)

        // Reutiliza a lógica de update que já trata offline/online
        updateTaskForUser(userId, updatedTask)
    }

    suspend fun createUser(newUsername: String, newEmail: String, newPassword: String) {
        if (newUsername.isNotBlank() && newEmail.isNotBlank() && newPassword.isNotBlank()) {
            val createTag = "Criado"
            val newId = UUID.randomUUID().toString()

            val newUser = User(
                id = newId,
                username = newUsername,
                email = newEmail,
                uTaskList = arrayListOf()
            )
            try {
                val metadata = buildJsonObject {
                    put("username", kotlinx.serialization.json.JsonPrimitive(newUsername))
                    put("uTaskList", buildJsonArray { })  // Empty array for tasks
                }
                supabase.auth.signUpWith(Email){
                    email = newEmail;
                    password = newPassword;
                    data = metadata
                }
                withContext(Dispatchers.Main) {
                    allUsers.add(newUser)
                    Log.d(createTag, "User ${newUser.username} added with ${newUser.id} ID")
                }
            } catch (e: Exception) {

                Log.e("SupabaseInsertError", "Erro ao inserir usuário no Supabase", e)
                throw e
            }

            allUsers.add(newUser)
            Log.d(createTag, "User ${newUser.username} added with ${newUser.id} ID")
        } else {
            Log.e("CreationError", "Error: all inputs should be filled")
        }
    }

    suspend fun addTaskToUser(userId: String, task: Tarefa) {
        val addTaskTag = "AddTask"
        val debugTag = "SupabaseDebug"

        // 1. Atualiza RAM primeiro (Instantâneo)
        val user = allUsers.find { it.id == userId }
        user?.uTaskList?.add(task)
        Log.d(addTaskTag, "Tarefa adicionada na memória.")

        // 2. Tenta Supabase (Online)
        try {
            val taskForSupabase = TarefaSupabase(
                id = task.id,
                titulo = task.titulo,
                desc = task.desc,
                status = task.status,
                userId = userId
            )

            Log.d(debugTag, "ADD: Enviando tarefa para Supabase... $taskForSupabase")
            supabase.from("Tasks").insert(taskForSupabase)
            Log.d(debugTag, "ADD: Tarefa inserida com SUCESSO no Supabase.")

        } catch (e: Exception) {
            // Se falhar (Offline), apenas loga. O dado está na RAM e será salvo no disco pelo MainActivity
            Log.e(addTaskTag, "Sem internet: Tarefa salva apenas localmente por enquanto.", e)
            Log.e(debugTag, "ADD: Falha ao enviar para Supabase (Offline?): ${e.message}")
        }
    }

    suspend fun updateTaskForUser(userId: String, updatedTask: Tarefa) {
        val updateTag = "TaskUpdate"
        val debugTag = "SupabaseDebug"

        // 1. Atualiza RAM primeiro
        val user = allUsers.find { it.id == userId }
        val taskIndex = user?.uTaskList?.indexOfFirst { it.id == updatedTask.id } ?: -1
        if (taskIndex != -1) {
            user?.uTaskList?.set(taskIndex, updatedTask)
        }

        // 2. Tenta Supabase
        try {
            val updates = buildJsonObject {
                put("titulo", kotlinx.serialization.json.JsonPrimitive(updatedTask.titulo))
                put("desc", kotlinx.serialization.json.JsonPrimitive(updatedTask.desc))
                put("status", kotlinx.serialization.json.JsonPrimitive(updatedTask.status))
            }

            Log.d(debugTag, "UPDATE: Atualizando tarefa ${updatedTask.id} no Supabase: $updates")

            supabase.from("Tasks").update(updates) {
                filter { eq("id", updatedTask.id); eq("userId", userId) }
            }

            Log.d(debugTag, "UPDATE: Sucesso ao atualizar no Supabase.")

        } catch (e: Exception) {
            Log.e(updateTag, "Sem internet: Atualização salva localmente.", e)
            Log.e(debugTag, "UPDATE: Falha ao enviar atualização (Offline?): ${e.message}")
        }
    }

    suspend fun deleteTaskForUser(userId: String, taskId: String) {
        val deleteTag = "TaskDelete"
        val debugTag = "SupabaseDebug"

        // 1. Remove da RAM primeiro
        val user = allUsers.find { it.id == userId }
        user?.uTaskList?.removeAll { it.id == taskId }

        // 2. Tenta Supabase
        try {
            Log.d(debugTag, "DELETE: Removendo tarefa $taskId do Supabase...")

            supabase.from("Tasks").delete {
                filter { eq("id", taskId); eq("userId", userId) }
            }

            Log.d(debugTag, "DELETE: Sucesso ao remover do Supabase.")

        } catch (e: Exception) {
            Log.e(deleteTag, "Sem internet: Remoção salva localmente.", e)
            Log.e(debugTag, "DELETE: Falha ao remover (Offline?): ${e.message}")
        }
    }

    suspend fun syncTaskToServer(userId: String, tarefa: Tarefa) {
        val syncTag = "SyncTask"
        try {
            val user = allUsers.find { it.id == userId } ?: return

            val existingTaskIndex = user.uTaskList.indexOfFirst { it.id == tarefa.id }
            if (existingTaskIndex != -1) {
                user.uTaskList[existingTaskIndex] = tarefa
                Log.d(syncTag, "Tarefa ${tarefa.id} atualizada no cache local durante sync.")
            } else {
                user.uTaskList.add(tarefa)
                Log.d(syncTag, "Tarefa ${tarefa.id} adicionada no cache local durante sync.")
            }
        } catch (e: Exception) {
            Log.e(syncTag, "Erro ao sincronizar tarefa ${tarefa.id} no cache local.", e)
            throw e
        }
    }

    fun findUserByEmail(email: String): User? {
        return allUsers.find { it.email.equals(email, ignoreCase = true) }
    }
    fun saveUsersToFile(file: File) {
        val jsonString = gson.toJson(allUsers)
        file.writeText(jsonString)
        Log.d("UserSaved", "Users saved sucessfully on: ${file.absolutePath} ")
    }

    fun loadUsersFromFile(file: File) {
        if (!file.exists()) {
            println("Arquivo não encontrado, nada para carregar.")
            return
        }
        val jsonString = file.readText()
        val type = object : TypeToken<MutableList<User>>() {}.type
        val loadedUsers: MutableList<User> = gson.fromJson(jsonString, type)
        allUsers.clear()
        allUsers.addAll(loadedUsers)
        println("${loadedUsers.size} usuários carregados com sucesso de: ${file.absolutePath}")
    }

    // Injeção de dados do disco para a memória (usado pelo MainActivity)
    fun loadFromCache(userId: String, cachedTasks: List<Tarefa>) {
        val user = User(
            id = userId,
            username = "Modo Offline",
            email = "",
            uTaskList = ArrayList(cachedTasks)
        )
        val existingUserIndex = allUsers.indexOfFirst { it.id == userId }
        if (existingUserIndex != -1) {
            allUsers[existingUserIndex] = user
        } else {
            allUsers.add(user)
        }
        Log.d("UserRepository", "Memória RAM restaurada via Armazenamento Local.")
    }

    // (Opcional) Mantive a reloadUserData antiga por compatibilidade,
    // mas o app agora usa syncUserData preferencialmente.
    suspend fun reloadUserData(userId: String): List<Tarefa>? {
        return syncUserData(userId)
    }
}