package com.filizzola.projeto_mobile.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.supabase
import com.filizzola.projeto_mobile.utils.LoginManager
import com.filizzola.projeto_mobile.utils.SyncManager
import io.github.jan.supabase.auth.auth

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val syncManager = SyncManager(context)
    private val loginManager = LoginManager(context)

    override suspend fun doWork(): Result {
        // VERIFICAÇÃO DE CONSENTIMENTO
        if (!loginManager.hasSyncConsent()) {
            Log.d("SyncWorker", "Worker cancelado: Sem consentimento de sincronização.")
            // Retorna success para não ficar tentando reexecutar desnecessariamente
            return Result.success()
        }

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