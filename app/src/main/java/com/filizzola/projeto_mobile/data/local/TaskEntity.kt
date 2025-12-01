package com.filizzola.projeto_mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.filizzola.projeto_mobile.data.Tarefa

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val desc: String,
    val status: String,
    val userId: String,
    val dueDate: Long?,
    val isDeleted: Boolean = false,
    val isSynced: Boolean = true // Flag para controle de sync
) {
    // Mapper para converter Entity -> Modelo de Domínio (Tarefa) usado na UI
    fun toTarefa(): Tarefa {
        return Tarefa(
            id = id,
            titulo = titulo,
            desc = desc,
            status = status,
            userId = userId,
            dueDate = dueDate,
            isDeleted = isDeleted
        )
    }
}

// Mapper reverso: Tarefa -> Entity
fun Tarefa.toEntity(isSynced: Boolean = false): TaskEntity {
    return TaskEntity(
        id = id,
        titulo = titulo,
        desc = desc,
        status = status,
        userId = userId,
        dueDate = dueDate,
        isDeleted = isDeleted,
        isSynced = isSynced
    )
}