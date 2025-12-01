package com.filizzola.projeto_mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0")
    fun getTasksFlow(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId")
    suspend fun getAllTasks(userId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, isSynced = 0 WHERE id = :taskId")
    suspend fun markAsDeleted(taskId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun hardDelete(taskId: String)
}