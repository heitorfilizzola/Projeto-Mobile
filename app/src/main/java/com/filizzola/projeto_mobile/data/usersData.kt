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
    val passwordHash: String,
    val uTaskList: ArrayList<Tarefa>
)

object UserRepository {
    val allUsers: MutableList<User> = mutableListOf()
    private val gson = Gson()

    fun findUserByEmal(email: String): User? {
        return allUsers.find { it.email.equals(email, ignoreCase = true) }
    }

    suspend fun login(email: String, password: String): User? {
        val loginTag = "LoginProcess"
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = findUserByEmal(email)

            if (user != null) {
                Log.d(loginTag, "Login bem sucedido para o usuario: ${user.username} (${user.id})")
                return user
            } else {
                // Cenário de fallback: se o usuário existe no Supabase mas não localmente.
                // Pode acontecer se o app foi reinstalado.
                // Aqui você poderia, opcionalmente, recriar o usuário na lista local a partir dos dados do Supabase.
                Log.w(loginTag, "Usuário autenticado via Supabase, mas não encontrado na lista local 'allUsers'.")

                // Tentando recuperar o ID e o nome do usuário da sessão atual do Supabase
                val session = supabase.auth.currentSessionOrNull()
                if(session != null) {
                    val authenticatedUser = User(
                        id = session.user!!.id, // ID do Supabase
                        username = session.user!!.userMetadata?.get("username")?.toString()?.removeSurrounding("\"") ?: "Usuário",
                        email = email,
                        passwordHash = "", // Não armazene a senha em texto plano!
                        uTaskList = arrayListOf()
                    )
                    // Adiciona o usuário recuperado à lista local para consistência
                    allUsers.add(authenticatedUser)
                    return authenticatedUser
                }

                return null
            }

        } catch (e: Exception) {
            // 3. Se o Supabase retornar um erro (ex: senha incorreta, usuário não existe),
            //    a exceção será capturada aqui.
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
                passwordHash = newPassword,
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

    fun addTaskToUser(userId: String, task: Tarefa) {
        val user = allUsers.find { it.id == userId }
        user?.uTaskList?.add(task)
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

    fun deleteTaskForUser(userId: String, taskId: String) {
        val user = allUsers.find { it.id == userId }
        user?.uTaskList?.removeAll { it.id == taskId }
        Log.d("TaskDelete", "Tarefa $taskId deletada para o usuário $userId")
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
