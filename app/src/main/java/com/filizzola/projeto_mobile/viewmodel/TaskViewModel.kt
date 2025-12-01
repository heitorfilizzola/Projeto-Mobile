package com.filizzola.projeto_mobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.repository.TaskRepository
import com.filizzola.projeto_mobile.utils.LoginManager
import com.filizzola.projeto_mobile.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val loginManager: LoginManager
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Tarefa>>(emptyList())
    val tasks: StateFlow<List<Tarefa>> = _tasks.asStateFlow()

    private val _syncConsent = MutableStateFlow(false)
    val syncConsent: StateFlow<Boolean> = _syncConsent.asStateFlow()

    fun loadTasks(userId: String) {
        viewModelScope.launch {
            _syncConsent.value = loginManager.hasSyncConsent()
            taskRepository.getTasks(userId).collect {
                _tasks.value = it
            }
        }
    }

    fun updateSyncConsent(granted: Boolean) {
        viewModelScope.launch {
            loginManager.setSyncConsent(granted)
            _syncConsent.value = granted
        }
    }

    fun addTask(userId: String, task: Tarefa) {
        viewModelScope.launch {
            taskRepository.saveTask(task)
            NotificationHelper.scheduleNotification(context, task)
        }
    }

    fun updateTask(userId: String, task: Tarefa) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
            NotificationHelper.scheduleNotification(context, task)
        }
    }

    fun deleteTask(userId: String, taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            NotificationHelper.cancelNotification(context, taskId)
        }
    }

    fun changeTaskStatus(userId: String, task: Tarefa) {
        viewModelScope.launch {
            val newStatus = if (task.status == "A fazer") "Feito" else "A fazer"
            val updatedTask = task.copy(status = newStatus)

            taskRepository.updateTask(updatedTask)

            if (newStatus == "Feito") {
                NotificationHelper.cancelNotification(context, task.id)
            } else {
                NotificationHelper.scheduleNotification(context, updatedTask)
            }
        }
    }

    fun syncTasks(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!loginManager.hasSyncConsent()) {
                onResult(false)
                return@launch
            }
            taskRepository.syncTasksRemote(userId)
            onResult(true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            loginManager.clearLoggedUser()
        }
    }
}