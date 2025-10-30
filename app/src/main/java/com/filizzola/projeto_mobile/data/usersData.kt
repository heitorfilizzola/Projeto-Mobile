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
    val userId: String // Chave estrangeira para associar a tarefa ao usuário
)

object UserRepository {
    val allUsers: MutableList<User> = mutableListOf()
    private val gson = Gson()

    fun findUserByEmal(email: String): User? {
        return allUsers.find { it.email.equals(email, ignoreCase = true) }
    }

    private suspend fun fetchTasksForUser(userId: String): ArrayList<Tarefa> {
        val tasksTag = "FetchTasks"
        try {
            // Busca na tabela "tarefas" onde a coluna "user_id" corresponde ao ID do usuário
            // A função decodeList<Tarefa> mapeará a resposta para sua classe de dados local.
            val tasksFromSupabase = supabase.from("Tasks").select() {
                filter {
                    eq("userId", userId)
                }
            }.decodeList<Tarefa>()

            Log.d(tasksTag, "Tarefas buscadas com sucesso para o usuário $userId: ${tasksFromSupabase.size} tarefas encontradas.")
            return ArrayList(tasksFromSupabase)
        } catch (e: Exception) {
            Log.e(tasksTag, "Erro ao buscar tarefas para o usuário $userId no Supabase.", e)
            return arrayListOf() // Retorna uma lista vazia em caso de erro
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

            // 2. Obtém a sessão atual para pegar os dados do usuário autenticado
            val session = supabase.auth.currentSessionOrNull()
            if (session?.user != null) {
                val userId = session.user!!.id

                // 3. Puxa a lista de tasks do banco pelo userId
                val userTasks = fetchTasksForUser(userId)

                val authenticatedUser = User(
                    id = userId,
                    username = session.user!!.userMetadata?.get("username")?.toString()?.removeSurrounding("\"") ?: "Usuário",
                    email = email,
                    uTaskList = userTasks // Atribui a lista de tarefas buscada
                )

                // 4. Adiciona ou atualiza o usuário na lista local
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
        supabase.auth.signOut()
        Log.d("LogoutProcess", "Usuário deslogado com sucesso.")
    }


//    fun login(email: String, password: String): User? {
//        val loginTag = "LoginProcess"
//        val user = findUserByEmal(email)
//
//        if (user == null) {
//            Log.e(loginTag, "Login Falhou, Usuario com email $email nao encontrado")
//            return null
//        }
//
//        if (password == user.passwordHash) {
//            Log.d(loginTag, "Login bem sucedido para o usuario: ${user.username} (${user.id})")
//            return user
//        } else {
//            Log.d(loginTag, "Erro no login para o usuario: ${user.username} (${user.id})")
//            return null
//        }
//    }

    fun toggleTaskStatusForUser(userId: String, taskId: String) {
        val userIndex = allUsers.indexOfFirst { it.id == userId }
        if (userIndex != -1) {
            val user = allUsers[userIndex]
            val taskIndex = user.uTaskList.indexOfFirst { it.id == taskId }
            if (taskIndex != -1) {
                val task = user.uTaskList[taskIndex]
                val updatedTask = task.copy(
                    status = if (task.status == "A fazer") "Feito" else "A fazer"
                )
                val newTaskList = user.uTaskList.toMutableList()
                newTaskList[taskIndex] = updatedTask
                allUsers[userIndex] = user.copy(uTaskList = newTaskList as ArrayList<Tarefa>)
            }
        }
    }

    // O suspend serve para marcar uma funçao como assincrona, permitindo que ela seja pausada e retomada sem bloquear a thread em que foi chamada.
    suspend fun createUser(newUsername: String, newEmail: String, newPassword: String) {
        if (newUsername.isNotBlank() && newEmail.isNotBlank() && newPassword.isNotBlank()) {
            val createTag = "Criado"
            val newId = UUID.randomUUID().toString()

            val newUser = User(
                id = newId,
                username = newUsername,
                email = newEmail,
//                passwordHash = newPassword,
                uTaskList = arrayListOf()
            )

            try {
//                supabase.from("User").insert(newUser)

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

        // 1. Prepara a tarefa para ser inserida no Supabase, incluindo o user_id.
        // A sua classe Tarefa já tem o userId, então podemos usá-la diretamente.
        // No entanto, para garantir que o nome da coluna no banco ("user_id") está correto,
        // criamos um objeto intermediário TarefaSupabase.
        val taskForSupabase = TarefaSupabase(
            id = task.id,
            titulo = task.titulo,
            status = task.status,
            userId = userId // Garante que o ID do usuário logado seja usado
        )

        try {
            // 2. Insere a nova tarefa na tabela "tarefas" do Supabase
            supabase.from("Tasks").insert(taskForSupabase)

            // 3. Atualiza a lista de tarefas local do usuário
            val user = allUsers.find { it.id == userId }
            user?.uTaskList?.add(task)
            Log.d(addTaskTag, "Tarefa ${task.id} adicionada com sucesso ao usuário $userId no Supabase e localmente.")

        } catch (e: Exception) {
            Log.e(addTaskTag, "Erro ao salvar a tarefa ${task.id} no Supabase para o usuário $userId.", e)
            // Lançar a exceção pode ser útil para notificar a UI sobre a falha
            throw e
        }
    }

    fun updateTaskForUser(userId: String, updatedTask: Tarefa) {
        val user = allUsers.find { it.id == userId }
        user?.let {
            val taskIndex = it.uTaskList.indexOfFirst { task -> task.id == updatedTask.id }
            if (taskIndex != -1) {
                it.uTaskList[taskIndex] = updatedTask
                Log.d("TaskUpdate", "Tarefa ${updatedTask.id} atualizada para o usuário $userId")
            }
        }
    }

    suspend fun deleteTaskForUser(userId: String, taskId: String) {
        val deleteTag = "TaskDelete"
        try {
            // 1. Deleta a tarefa do banco de dados Supabase
            supabase.from("Tasks").delete {
                filter {
                    eq("id", taskId)      // Encontra a tarefa pelo seu ID
                    eq("userId", userId)  // Garante que só pode deletar a tarefa se for o dono
                }
            }
            Log.d(deleteTag, "Tarefa $taskId deletada do Supabase para o usuário $userId")

            // 2. Remove a tarefa da lista local (cache)
            val user = allUsers.find { it.id == userId }
            user?.uTaskList?.removeAll { it.id == taskId }
            Log.d(deleteTag, "Tarefa $taskId removida do cache local.")

        } catch (e: Exception) {
            Log.e(deleteTag, "Erro ao deletar a tarefa $taskId no Supabase.", e)
            // Lança a exceção para que a UI possa ser notificada do erro
            throw e
        }
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
}
