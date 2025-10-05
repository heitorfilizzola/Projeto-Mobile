package com.filizzola.projeto_mobile.data

data class Tarefa(
    val titulo: String,
    val status: String,
)

object TaskRepository {
    val allTasks: List<Tarefa> = listOf()
}
