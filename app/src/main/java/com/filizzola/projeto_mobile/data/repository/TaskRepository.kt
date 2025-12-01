package com.filizzola.projeto_mobile.data.repository

import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.local.TaskDao
import com.filizzola.projeto_mobile.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    // Retorna um Flow direto do Banco de Dados para a UI (Reativo)
    fun getTasks(userId: String): Flow<List<Tarefa>> {
        return taskDao.getTasksFlow(userId).map { entities ->
            entities.map { it.toTarefa() }
        }
    }

    suspend fun saveTask(task: Tarefa) {
        // Salva localmente marcando como não sincronizado (isSynced = false)
        // O Worker deve pegar isso depois e enviar pra nuvem
        taskDao.insertTask(task.toEntity(isSynced = false))
    }

    suspend fun updateTask(task: Tarefa) {
        taskDao.updateTask(task.toEntity(isSynced = false))
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.markAsDeleted(taskId)
    }

    // Método auxiliar para o Worker de Sincronização
    suspend fun saveSyncedTasks(tasks: List<Tarefa>) {
        tasks.forEach { task ->
            taskDao.insertTask(task.toEntity(isSynced = true))
        }
    }
}