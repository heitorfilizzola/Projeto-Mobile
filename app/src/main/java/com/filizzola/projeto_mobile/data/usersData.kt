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
        supabase.auth.signOut()
        Log.d("LogoutProcess", "Usuário deslogado com sucesso.")
    }


    // Versão antiga do login sem Supabase (Provavelmente havera reutilizacao de parte para poder implementar login offline)

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

    suspend fun toggleTaskStatusForUser(userId: String, taskId: String) {
//        val userIndex = allUsers.indexOfFirst { it.id == userId }
//        if (userIndex != -1) {
//            val user = allUsers[userIndex]
//            val taskIndex = user.uTaskList.indexOfFirst { it.id == taskId }
//            if (taskIndex != -1) {
//                val task = user.uTaskList[taskIndex]
//                val updatedTask = task.copy(
//                    status = if (task.status == "A fazer") "Feito" else "A fazer"
//                )
//                val newTaskList = user.uTaskList.toMutableList()
//                newTaskList[taskIndex] = updatedTask
//                allUsers[userIndex] = user.copy(uTaskList = newTaskList as ArrayList<Tarefa>)
//            }
//        }
        val toggleTag = "TaskToggleStatus"
        val user = allUsers.find { it.id == userId }
        val task = user?.uTaskList?.find { it.id == taskId }

        if (task == null) {
            Log.e(toggleTag, "Tarefa $taskId não encontrada no cache local para o usuário $userId.")
            return
        }

        val newStatus = if (task.status == "A fazer") "Feito" else "A fazer"

        try {
            val statusUpdate = buildJsonObject {
                put("status", JsonPrimitive(newStatus))
            }

            supabase.from("Tasks").update(statusUpdate) {
                filter {
                    eq("id", taskId)
                    eq("userId", userId)
                }
            }
            Log.d(toggleTag, "Status da tarefa $taskId atualizado para '$newStatus' no Supabase.")

            val taskIndex = user.uTaskList.indexOfFirst { it.id == taskId }
            if (taskIndex != -1) {
                user.uTaskList[taskIndex] = user.uTaskList[taskIndex].copy(status = newStatus)
                Log.d(toggleTag, "Status da tarefa $taskId atualizado no cache local.")
            }

        } catch (e: Exception) {
            Log.e(toggleTag, "Erro ao atualizar status da tarefa $taskId no Supabase.", e)
            throw e
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

        val taskForSupabase = TarefaSupabase(
            id = task.id,
            titulo = task.titulo,
            status = task.status,
            userId = userId
        )

        try {
            supabase.from("Tasks").insert(taskForSupabase)
            val user = allUsers.find { it.id == userId }
            user?.uTaskList?.add(task)
            Log.d(addTaskTag, "Tarefa ${task.id} adicionada com sucesso ao usuário $userId no Supabase e localmente.")
        } catch (e: Exception) {
            Log.e(addTaskTag, "Erro ao salvar a tarefa ${task.id} no Supabase para o usuário $userId.", e)
            throw e
        }
    }

    suspend fun updateTaskForUser(userId: String, updatedTask: Tarefa) {
        val updateTag = "TaskUpdate"
        try {
            // 1. Prepara os dados para o Supabase. Não precisa mandar o ID.
            val updates = buildJsonObject {
                put("titulo", kotlinx.serialization.json.JsonPrimitive(updatedTask.titulo))
                put("status", kotlinx.serialization.json.JsonPrimitive(updatedTask.status))
                // Adicione outras colunas que possam ser editadas aqui
            }

            // 2. Atualiza a tarefa no banco de dados Supabase.
            supabase.from("Tasks").update(updates) {
                filter {
                    eq("id", updatedTask.id) // Encontra a tarefa pelo ID dela
                    eq("userId", userId)     // Garante que o usuário é o dono da tarefa
                }
            }
            Log.d(updateTag, "Tarefa ${updatedTask.id} atualizada no Supabase.")

            // 3. Atualiza a tarefa na lista local (cache) para refletir a mudança na UI imediatamente.
            val user = allUsers.find { it.id == userId }
            user?.let {
                val taskIndex = it.uTaskList.indexOfFirst { task -> task.id == updatedTask.id }
                if (taskIndex != -1) {
                    it.uTaskList[taskIndex] = updatedTask
                    Log.d(updateTag, "Tarefa ${updatedTask.id} atualizada no cache local para o usuário $userId")
                }
            }
        } catch (e: Exception) {
            Log.e(updateTag, "Erro ao atualizar a tarefa ${updatedTask.id} no Supabase.", e)
            throw e // Lança o erro para a UI poder reagir
        }
    }

    suspend fun deleteTaskForUser(userId: String, taskId: String) {
        val deleteTag = "TaskDelete"
        try {
            supabase.from("Tasks").delete {
                filter {
                    eq("id", taskId)
                    eq("userId", userId)
                }
            }
            Log.d(deleteTag, "Tarefa $taskId deletada do Supabase para o usuário $userId")
            // Remove a tarefa da lista local (cache)
            val user = allUsers.find { it.id == userId }
            user?.uTaskList?.removeAll { it.id == taskId }
            Log.d(deleteTag, "Tarefa $taskId removida do cache local.")
        } catch (e: Exception) {
            Log.e(deleteTag, "Erro ao deletar a tarefa $taskId no Supabase.", e)
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
}
