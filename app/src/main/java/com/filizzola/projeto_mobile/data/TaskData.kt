package com.filizzola.projeto_mobile.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Tarefa(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val status: String,
    val userId: String
)

object TaskRepository {
    val allTasks: MutableList<Tarefa> = mutableListOf()
}