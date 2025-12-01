package com.filizzola.projeto_mobile.utils

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.worker.NotificationWorker
import java.util.concurrent.TimeUnit

import androidx.work.ExistingWorkPolicy

object NotificationHelper {

    fun scheduleNotification(context: Context, task: Tarefa) {
        val dueDate = task.dueDate ?: return
        val currentTime = System.currentTimeMillis()
        val delay = dueDate - currentTime

        if (delay > 0) {
            val data = Data.Builder()
                .putString("task_title", task.titulo)
                .putString("task_id", task.id)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(task.id) // Use task ID as tag to cancel if needed
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                task.id,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun cancelNotification(context: Context, taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(taskId)
    }
}
