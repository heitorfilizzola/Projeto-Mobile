package com.filizzola.projeto_mobile.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.UserRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.taskDataStore by preferencesDataStore(name = "tasks_local_storage")

class SyncManager(private val context: Context) {

    private val networkManager = NetworkManager(context)
    private val loginManager = LoginManager(context) // Instância do LoginManager
    private val gson = Gson()

    suspend fun syncWithServer(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // VERIFICAÇÃO DE CONSENTIMENTO
            if (!loginManager.hasSyncConsent()) {
                Log.d("SyncManager", "Sincronização abortada: Usuário não forneceu consentimento.")
                return@withContext false
            }

            val hasConnection = networkManager.isConnected()

            if (!hasConnection) {
                return@withContext false
            }

            Log.d("SyncManager", "Dados sincronizados com sucesso")
            true
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed: ${e.message}")
            false
        }
    }

    suspend fun loadTasksFromLocal(userId: String): List<Tarefa> = withContext(Dispatchers.IO) {
        try {
            val preferences = context.taskDataStore.data.first()
            // Busca a string salva com a chave "tasks_ID_DO_USUARIO"
            val tasksJson = preferences[stringPreferencesKey("tasks_$userId")]

            if (tasksJson != null) {
                val type = object : TypeToken<List<Tarefa>>() {}.type
                gson.fromJson(tasksJson, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Erro ao ler armazenamento local: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveTaskLocally(userId: String, tasks: List<Tarefa>) = withContext(Dispatchers.IO) {
        try {
            val jsonString = gson.toJson(tasks)
            context.taskDataStore.edit { preferences ->
                preferences[stringPreferencesKey("tasks_$userId")] = jsonString
            }
            Log.d("SyncManager", "Dados salvos no ARMAZENAMENTO DO DISPOSITIVO para user $userId")
        } catch (e: Exception) {
            Log.e("SyncManager", "Erro ao gravar no disco: ${e.message}")
        }
    }

    suspend fun persistCurrentData(userId: String) {
        val currentTasks = UserRepository.allUsers.find { it.id == userId }?.uTaskList ?: arrayListOf()
        saveTaskLocally(userId, currentTasks)
    }

    suspend fun deleteTaskLocally(taskId: String) {
        Log.d("SyncManager", "Task $taskId marked for deletion")
    }

    suspend fun updateTaskLocally(task: Tarefa) {
        Log.d("SyncManager", "Task ${task.id} updated locally")
    }
}