package com.filizzola.projeto_mobile.di

import android.content.Context
import androidx.room.Room
import com.filizzola.projeto_mobile.data.local.AppDatabase
import com.filizzola.projeto_mobile.data.local.TaskDao
import com.filizzola.projeto_mobile.utils.LoginManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "projeto_mobile_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideLoginManager(@ApplicationContext context: Context): LoginManager {
        return LoginManager(context)
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://hotdhewlluokhhxamydi.supabase.co/",
            supabaseKey = "sb_publishable_Te2ter0ZFhL4kZKozwFgEA_aFhW7_lD"
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
    }
}