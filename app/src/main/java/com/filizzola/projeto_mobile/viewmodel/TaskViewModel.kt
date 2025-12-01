package com.filizzola.projeto_mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.User
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.utils.LoginManager
import com.filizzola.projeto_mobile.utils.NotificationHelper
import com.filizzola.projeto_mobile.utils.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val _tasks = MutableStateFlow<List<Tarefa>>(emptyList())
    val tasks: StateFlow<List<Tarefa>> = _tasks.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // Estado do consentimento
    private val _syncConsent = MutableStateFlow(false)
    val syncConsent: StateFlow<Boolean> = _syncConsent.asStateFlow()

    private val loginManager = LoginManager(application)
    private val syncManager = SyncManager(application)

    fun loadTasks(userId: String) {
        viewModelScope.launch {
            val user = UserRepository.allUsers.find { it.id == userId }
            _user.value = user
            _tasks.value = user?.uTaskList?.filter { !it.isDeleted }?.toList() ?: emptyList()

            // Carrega também o status do consentimento
            loadSyncConsent()
        }
    }

    fun loadSyncConsent() {
        viewModelScope.launch {
            _syncConsent.value = loginManager.hasSyncConsent()
        }
    }

    fun updateSyncConsent(granted: Boolean) {
        viewModelScope.launch {
            loginManager.setSyncConsent(granted)
            _syncConsent.value = granted
        }
    }

    private suspend fun saveToDisk(userId: String) {
        val user = UserRepository.allUsers.find { it.id == userId }
        if (user != null) {
            syncManager.saveTaskLocally(userId, user.uTaskList)
        }
    }

    fun addTask(userId: String, task: Tarefa) {
        viewModelScope.launch {
            UserRepository.addTaskToUser(userId, task)
            NotificationHelper.scheduleNotification(getApplication(), task)
            saveToDisk(userId)
            loadTasks(userId)
        }
    }

    fun updateTask(userId: String, task: Tarefa) {
        viewModelScope.launch {
            UserRepository.updateTaskForUser(userId, task)
            NotificationHelper.scheduleNotification(getApplication(), task)
            saveToDisk(userId)
            loadTasks(userId)
        }
    }

    fun deleteTask(userId: String, taskId: String) {
        viewModelScope.launch {
            UserRepository.deleteTaskForUser(userId, taskId)
            NotificationHelper.cancelNotification(getApplication(), taskId)
            saveToDisk(userId)
            loadTasks(userId)
        }
    }

    fun changeTaskStatus(userId: String, task: Tarefa) {
        viewModelScope.launch {
            val newStatus = if (task.status == "A fazer") "Feito" else "A fazer"
            val updatedTask = task.copy(status = newStatus)
            UserRepository.updateTaskForUser(userId, updatedTask)

            if (newStatus == "Feito") {
                NotificationHelper.cancelNotification(getApplication(), task.id)
            } else {
                NotificationHelper.scheduleNotification(getApplication(), updatedTask)
            }

            saveToDisk(userId)
            loadTasks(userId)
        }
    }

    fun syncTasks(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Verifica o consentimento antes de tentar sincronizar via ViewModel também
            if (!loginManager.hasSyncConsent()) {
                onResult(false) // Indica falha (ou falta de permissão)
                return@launch
            }

            val result = UserRepository.syncUserData(userId)
            if (result != null) {
                saveToDisk(userId)
            }
            loadTasks(userId)
            onResult(result != null)
        }
    }

    fun logout() {
        viewModelScope.launch {
            UserRepository.logout()
            loginManager.clearLoggedUser()
        }
    }
}