package com.filizzola.projeto_mobile.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID
import com.filizzola.projeto_mobile.data.Tarefa
data class User(
    val id: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val uTaskList: List<Tarefa>
)

object UserRepository {
    val allUsers: MutableList<User> = mutableListOf()
    private val gson = Gson()

    fun findUserByEmal(email: String): User? {
        return allUsers.find { it.email.equals(email, ignoreCase = true) }
    }

    fun login(email: String, password: String): User? {
        val loginTag = "LoginProcess"
        val user = findUserByEmal(email)

        if (user == null) {
            Log.e(loginTag, "Login Falhou, Usuario com email $email nao encontrado")
            return null
        }

        if (password == user.passwordHash) {
            Log.d(loginTag, "Login bem sucedido para o usuario: ${user.username} (${user.id})")
            return user
        } else {
            Log.d(loginTag, "Erro no login para o usuario: ${user.username} (${user.id})")
            return null
        }
    }

    fun createUser(newUsername: String, newEmail: String, newPassword: String) {
        if (newUsername.isNotBlank() && newEmail.isNotBlank() && newPassword.isNotBlank()) {
            val createTag = "Criado"
            val newId = UUID.randomUUID().toString()

            val newUser = User(
                id = newId,
                username = newUsername,
                email = newEmail,
                passwordHash = newPassword,
                uTaskList = emptyList()
            )

            allUsers.add(newUser)
            Log.d(createTag, "User ${newUser.username} added with ${newUser.id} ID")
        } else {
            Log.e("CreationError", "Error: all inputs should be filled")
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