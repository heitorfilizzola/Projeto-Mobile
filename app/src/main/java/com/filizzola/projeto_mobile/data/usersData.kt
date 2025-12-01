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
    val userId: String, // Chave estrangeira para associar a tarefa ao usuário
    val dueDate: String? = null
)

object UserRepository {
    val allUsers: MutableList<User> = mutableListOf()
    private val gson = Gson()

    private fun Long.toIsoString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(this))
    }

    private fun String.toMillis(): Long {
        // Tenta formatos comuns do Postgres/ISO 8601
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        
        for (pattern in patterns) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return sdf.parse(this)?.time ?: continue
            } catch (e: Exception) {
                continue
            }
        }
        return 0L
    }

    private suspend fun fetchTasksForUser(userId: String): ArrayList<Tarefa> {
        val tasksTag = "FetchTasks"
        try {
            // Decodifica para TarefaSupabase primeiro para lidar com a String de data
            val tasksFromSupabase = supabase.from("Tasks").select() {
                filter {
                    eq("userId", userId)
                }
            }.decodeList<TarefaSupabase>()

            // Converte para o modelo Tarefa do app
            val tasks = tasksFromSupabase.map { taskSupabase ->
                Tarefa(
                    id = taskSupabase.id,
                    titulo = taskSupabase.titulo,
                    status = taskSupabase.status,
                    desc = taskSupabase.desc ?: "",
                    userId = taskSupabase.userId,
                    dueDate = taskSupabase.dueDate?.toMillis()
                )
            }

            Log.d(tasksTag, "Tarefas buscadas com sucesso para o usuário $userId: ${tasks.size} tarefas encontradas.")
            return ArrayList(tasks)
        } catch (e: Exception) {
            Log.e(tasksTag, "Erro ao buscar tarefas para o usuário $userId no Supabase.", e)
            return arrayListOf()
        }
    }

    // --- NOVA FUNÇÃO DE SYNC (INCREMENTAL) ---
    suspend fun syncUserData(userId: String): List<Tarefa>? {
        val syncTag = "SyncProcess"
        val debugTag = "SupabaseDebug"
        try {
            val user = allUsers.find { it.id == userId }
            val localTasks = user?.uTaskList ?: arrayListOf()

            // 1. PUSH: Envia mudanças locais (apenas as sujas)
            val dirtyTasks = localTasks.filter { !it.isSynced }.toList() // Cópia para evitar ConcurrentModification
            Log.d(syncTag, "SYNC: Encontradas ${dirtyTasks.size} mudanças locais para enviar.")

            dirtyTasks.forEach { task ->
                try {
                    if (task.isDeleted) {
                        Log.d(debugTag, "SYNC: Deletando tarefa ${task.id} do servidor...")
                        supabase.from("Tasks").delete {
                            filter { eq("id", task.id); eq("userId", userId) }
                        }
                        // Sucesso: Remove definitivamente da RAM
                        val index = localTasks.indexOfFirst { it.id == task.id }
                        if (index != -1) {
                            localTasks.removeAt(index)
                        }
                    } else {
                        val taskForSupabase = TarefaSupabase(
                            id = task.id,
                            titulo = task.titulo,
                            desc = task.desc,
                            status = task.status,
                            userId = userId,
                            dueDate = task.dueDate?.toIsoString()
                        )
                        Log.d(debugTag, "SYNC: Enviando tarefa ${task.id} para servidor...")
                        supabase.from("Tasks").upsert(taskForSupabase) { onConflict = "id" }
                        
                        // Sucesso: Marca como sincronizado
                        val index = localTasks.indexOfFirst { it.id == task.id }
                        if (index != -1) {
                            localTasks[index] = localTasks[index].copy(isSynced = true)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(syncTag, "Falha ao enviar tarefa ${task.id}: ${e.message}")
                    // Continua para a próxima, mantendo esta como dirty
                }
            }

            // 2. PULL: Baixa dados remotos (Snapshot completo do servidor)
            Log.d(debugTag, "SYNC: Baixando dados remotos (Pull)...")
            val remoteTasks = fetchTasksForUser(userId)

            // 3. MERGE: Integra Remoto com Local
            val remoteIds = remoteTasks.map { it.id }.toSet()

            // 3a. Handle Remote Deletes: Remove locais (limpos) que não estão no remoto
            // Se estiver dirty, mantemos (re-criará no servidor ou conflito)
            val tasksToRemove = localTasks.filter { it.isSynced && !remoteIds.contains(it.id) }
            localTasks.removeAll(tasksToRemove)
            if(tasksToRemove.isNotEmpty()) Log.d(syncTag, "SYNC: ${tasksToRemove.size} tarefas removidas remotamente foram apagadas localmente.")

            // 3b. Update/Insert Remote Tasks
            remoteTasks.forEach { remote ->
                val localIndex = localTasks.indexOfFirst { it.id == remote.id }
                if (localIndex != -1) {
                    val local = localTasks[localIndex]
                    if (local.isSynced) {
                        // Local está limpo, sobrescreve com o remoto (Server Wins)
                        // Preserva o synced=true
                         localTasks[localIndex] = remote.copy(isSynced = true, lastModified = System.currentTimeMillis())
                    } else {
                        // Local está sujo (Conflito). Mantém o local (Client Wins temporariamente)
                        Log.d(syncTag, "Conflito: Tarefa ${remote.id} alterada localmente. Mantendo versão local.")
                    }
                } else {
                    // Novo do remoto
                    localTasks.add(remote.copy(isSynced = true, lastModified = System.currentTimeMillis()))
                }
            }

            Log.d(syncTag, "Sincronização completa. Lista atualizada.")
            return localTasks

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

        // 1. Prepare task for local storage (Default: Not Synced)
        val taskToAdd = task.copy(
            isSynced = false,
            lastModified = System.currentTimeMillis()
        )

        // 2. Atualiza RAM primeiro (Instantâneo)
        val user = allUsers.find { it.id == userId }
        user?.uTaskList?.add(taskToAdd)
        Log.d(addTaskTag, "Tarefa adicionada na memória (Pending Sync).")

        // 3. Tenta Supabase (Online)
        try {
            val taskForSupabase = TarefaSupabase(
                id = taskToAdd.id,
                titulo = taskToAdd.titulo,
                desc = taskToAdd.desc,
                status = taskToAdd.status,
                userId = userId,
                dueDate = taskToAdd.dueDate?.toIsoString()
            )

            Log.d(debugTag, "ADD: Enviando tarefa para Supabase... $taskForSupabase")
            supabase.from("Tasks").insert(taskForSupabase)
            
            // Sucesso: Marca como sincronizado na RAM
            val index = user?.uTaskList?.indexOfFirst { it.id == taskToAdd.id }
            if (index != null && index != -1) {
                user?.uTaskList?.set(index, taskToAdd.copy(isSynced = true))
            }
            
            Log.d(debugTag, "ADD: Tarefa inserida com SUCESSO no Supabase.")

        } catch (e: Exception) {
            // Se falhar (Offline), apenas loga. O dado está na RAM com isSynced=false
            Log.e(addTaskTag, "Sem internet: Tarefa mantida localmente para sync futuro.", e)
        }
    }

    suspend fun updateTaskForUser(userId: String, updatedTask: Tarefa) {
        val updateTag = "TaskUpdate"
        val debugTag = "SupabaseDebug"

        val taskToUpdate = updatedTask.copy(
            isSynced = false,
            lastModified = System.currentTimeMillis()
        )

        // 1. Atualiza RAM primeiro
        val user = allUsers.find { it.id == userId }
        val taskIndex = user?.uTaskList?.indexOfFirst { it.id == taskToUpdate.id } ?: -1
        if (taskIndex != -1) {
            user?.uTaskList?.set(taskIndex, taskToUpdate)
        }

        // 2. Tenta Supabase
        try {
            val updates = buildJsonObject {
                put("titulo", kotlinx.serialization.json.JsonPrimitive(taskToUpdate.titulo))
                put("desc", kotlinx.serialization.json.JsonPrimitive(taskToUpdate.desc))
                put("status", kotlinx.serialization.json.JsonPrimitive(taskToUpdate.status))
                if (taskToUpdate.dueDate != null) {
                    put("dueDate", kotlinx.serialization.json.JsonPrimitive(taskToUpdate.dueDate.toIsoString()))
                }
            }

            // Logica de dueDate nulo omitida para brevidade, mantendo comportamento anterior

            Log.d(debugTag, "UPDATE: Atualizando tarefa ${taskToUpdate.id} no Supabase: $updates")

            supabase.from("Tasks").update(updates) {
                filter { eq("id", taskToUpdate.id); eq("userId", userId) }
            }

            // Sucesso: Marca como sincronizado
            if (taskIndex != -1) {
                user?.uTaskList?.set(taskIndex, taskToUpdate.copy(isSynced = true))
            }

            Log.d(debugTag, "UPDATE: Sucesso ao atualizar no Supabase.")

        } catch (e: Exception) {
            Log.e(updateTag, "Sem internet: Atualização mantida localmente para sync futuro.", e)
        }
    }

    suspend fun deleteTaskForUser(userId: String, taskId: String) {
        val deleteTag = "TaskDelete"
        val debugTag = "SupabaseDebug"

        // 1. Soft Delete na RAM primeiro (para esconder da UI imediatamente se estiver offline)
        val user = allUsers.find { it.id == userId }
        val taskIndex = user?.uTaskList?.indexOfFirst { it.id == taskId } ?: -1
        
        if (taskIndex != -1) {
            val task = user?.uTaskList?.get(taskIndex)
            if (task != null) {
                val deletedTask = task.copy(
                    isDeleted = true,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )
                user.uTaskList[taskIndex] = deletedTask
            }
        }

        // 2. Tenta Supabase
        try {
            Log.d(debugTag, "DELETE: Removendo tarefa $taskId do Supabase...")

            supabase.from("Tasks").delete {
                filter { eq("id", taskId); eq("userId", userId) }
            }

            // Sucesso: Remove definitivamente da RAM
            user?.uTaskList?.removeAll { it.id == taskId }

            Log.d(debugTag, "DELETE: Sucesso ao remover do Supabase.")

        } catch (e: Exception) {
            Log.e(deleteTag, "Sem internet: Marcação de deletado salva localmente para sync futuro.", e)
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