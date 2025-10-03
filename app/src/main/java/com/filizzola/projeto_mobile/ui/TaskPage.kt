package com.filizzola.projeto_mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Tarefa(
    val titulo: String,
    val status: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinhasTarefasApp()
        }
    }
}

@Composable
fun MinhasTarefasApp() {
    var tarefas by remember {
        mutableStateOf(
            listOf(
                Tarefa("Estudar", "A fazer"),
                Tarefa("Ir à academia", "A fazer"),
                Tarefa("Almoçar", "Fazendo"),
                Tarefa("Tomar banho", "Fazendo")
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // fundo escuro
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Minhas Tarefas",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(8.dp)
        )

        Button(
            onClick = {
                tarefas = tarefas + Tarefa("Nova tarefa", "A fazer")
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("+ Adicionar")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                CategoriaSection("A fazer", Color.Red, tarefas.filter { it.status == "A fazer" })
            }
            item {
                CategoriaSection("Fazendo", Color.Blue, tarefas.filter { it.status == "Fazendo" })
            }
        }
    }
}

@Composable
fun CategoriaSection(titulo: String, cor: Color, tarefas: List<Tarefa>) {
    if (tarefas.isNotEmpty()) {
        Text(
            text = titulo,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        Column {
            tarefas.forEach { tarefa ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tarefa.titulo)

                        Row {
                            IconButton(onClick = { /* editar */ }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Editar",
                                    tint = Color.Blue
                                )
                            }
                            IconButton(onClick = { /* excluir */ }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Excluir",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}