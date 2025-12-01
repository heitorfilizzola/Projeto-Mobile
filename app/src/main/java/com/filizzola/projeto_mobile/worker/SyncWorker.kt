package com.filizzola.projeto_mobile.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.utils.SyncManager
import io.github.jan.supabase.auth.auth
import com.filizzola.projeto_mobile.supabase

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val syncManager = SyncManager(context)

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Iniciando sincronização em segundo plano...")
        return try {
            val session = supabase.auth.currentSessionOrNull()
            val userId = session?.user?.id

            if (userId != null) {
                val result = UserRepository.syncUserData(userId)
                if (result != null) {
                    // Atualiza o cache local no disco após o sync bem sucedido
                    syncManager.saveTaskLocally(userId, result)
                    Log.d("SyncWorker", "Sincronização em segundo plano concluída com sucesso.")
                    Result.success()
                } else {
                    Log.e("SyncWorker", "Falha na sincronização (Network error ou API).")
                    Result.retry()
                }
            } else {
                Log.d("SyncWorker", "Usuário não logado. Ignorando sync.")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Erro durante sincronização em segundo plano", e)
            Result.retry()
        }
    }
}
