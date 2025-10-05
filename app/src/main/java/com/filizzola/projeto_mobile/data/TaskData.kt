package com.filizzola.projeto_mobile.data

import java.util.UUID

data class Tarefa(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val status: String,
    val userId: String
)

object TaskRepository {
    val allTasks: MutableList<Tarefa> = mutableListOf()
}