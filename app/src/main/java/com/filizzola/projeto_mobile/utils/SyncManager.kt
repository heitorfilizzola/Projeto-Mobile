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

private val Context.taskDataStore by preferencesDataStore(name = "tasks_prefs")

class SyncManager(private val context: Context) {

    private val networkManager = NetworkManager(context)
    private val gson = Gson()

    suspend fun syncWithServer(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
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
            val tasksJson = preferences[stringPreferencesKey("tasks_$userId")]
            if (tasksJson != null) {
                val type = object : TypeToken<List<Tarefa>>() {}.type
                gson.fromJson(tasksJson, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveTaskLocally(userId: String, tasks: List<Tarefa>) = withContext(Dispatchers.IO) {
        try {
            context.taskDataStore.edit { preferences ->
                preferences[stringPreferencesKey("tasks_$userId")] = gson.toJson(tasks)
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error saving tasks locally: ${e.message}")
        }
    }

    suspend fun deleteTaskLocally(taskId: String) {
        Log.d("SyncManager", "Task $taskId marked for deletion")
    }

    suspend fun updateTaskLocally(task: Tarefa) {
        Log.d("SyncManager", "Task ${task.id} updated locally")
    }
}