package com.filizzola.projeto_mobile.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class NetworkManager(private val context: Context) {

    suspend fun checkConnection(onError: (String) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://hotdhewlluokhhxamydi.supabase.co")
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.connect()
                true
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Sem conexão com a internet")
                }
                false
            }
        }
    }

    suspend fun isConnected(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://hotdhewlluokhhxamydi.supabase.co")
                val connection = url.openConnection()
                connection.connectTimeout = 3000
                connection.connect()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}