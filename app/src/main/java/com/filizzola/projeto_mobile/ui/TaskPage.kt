package com.filizzola.projeto_mobile.ui


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme

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

@OptIn(ExperimentalMaterial3Api::class)
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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    var presses by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "NoteSync",
                        textAlign = TextAlign.Center

                        )
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                ) {
                    Button(
                        onClick = {}
                    ) {
                        Icon(
                            Icons.Default.WorkHistory,
                            contentDescription = "To Do"
                        )
                    }
                    Button(
                        onClick = {
                            tarefas = tarefas + Tarefa("Nova tarefa", "A fazer")
                        },
                        modifier = Modifier
                            .fillMaxHeight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                    Button(
                        onClick = {}
                    ) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = "To Do"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column (
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)) // fundo escuro
                    .padding( innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding( 16.dp, 0.dp)
        ) {
            item {
                CategoriaSection("A fazer", Color.Red, tarefas.filter { it.status == "A fazer" })
            }
            item {
                CategoriaSection("Fazendo", Color.Blue, tarefas.filter { it.status == "Fazendo" })
            }
        }
    }
}}

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

@Preview
@Composable
fun previewTasks() {
    ProjetoMobileTheme {
        MinhasTarefasApp()
    }
}