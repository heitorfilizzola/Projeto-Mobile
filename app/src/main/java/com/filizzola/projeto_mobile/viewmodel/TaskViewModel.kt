package com.filizzola.projeto_mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.User
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.utils.LoginManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val _tasks = MutableStateFlow<List<Tarefa>>(emptyList())
    val tasks: StateFlow<List<Tarefa>> = _tasks.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val loginManager = LoginManager(application)

    fun loadTasks(userId: String) {
        viewModelScope.launch {
            val user = UserRepository.allUsers.find { it.id == userId }
            _user.value = user
            _tasks.value = user?.uTaskList?.toList() ?: emptyList()
        }
    }

    fun addTask(userId: String, task: Tarefa) {
        viewModelScope.launch {
            UserRepository.addTaskToUser(userId, task)
            loadTasks(userId)
        }
    }

    fun updateTask(userId: String, task: Tarefa) {
        viewModelScope.launch {
            UserRepository.updateTaskForUser(userId, task)
            loadTasks(userId)
        }
    }

    fun deleteTask(userId: String, taskId: String) {
        viewModelScope.launch {
            UserRepository.deleteTaskForUser(userId, taskId)
            loadTasks(userId)
        }
    }

    fun changeTaskStatus(userId: String, task: Tarefa) {
        viewModelScope.launch {
            val newStatus = if (task.status == "A fazer") "Feito" else "A fazer"
            val updatedTask = task.copy(status = newStatus)
            UserRepository.updateTaskForUser(userId, updatedTask)
            loadTasks(userId)
        }
    }

    fun logout() {
        viewModelScope.launch {
            UserRepository.logout()
            loginManager.clearLoggedUser()
        }
    }
}
