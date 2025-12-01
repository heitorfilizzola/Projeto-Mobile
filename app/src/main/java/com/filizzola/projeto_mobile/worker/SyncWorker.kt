package com.filizzola.projeto_mobile.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.filizzola.projeto_mobile.data.repository.TaskRepository
import com.filizzola.projeto_mobile.utils.LoginManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository,
    private val loginManager: LoginManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!loginManager.hasSyncConsent()) {
            return Result.success()
        }

        return try {
            val userId = loginManager.getLoggedUser()
            if (userId != null) {
                repository.syncTasksRemote(userId)
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}