package com.filizzola.projeto_mobile.ui


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MinhasTarefasApp() {
    var tarefas by remember {
        mutableStateOf(
            listOf(
                Tarefa("Estudar", "A fazer"),
                Tarefa("Ir à academia", "A fazer"),
                Tarefa("Almoçar", "Fazendo"),
                Tarefa("Tomar banho", "Fazendo"),
                Tarefa("Ler um livro", "Feito"),
                Tarefa("Pagar a conta de luz", "Feito")
            )
        )
    }

    var telaAtual by remember { mutableStateOf("A fazer") }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val alpha = 1f - scrollBehavior.state.collapsedFraction
            CenterAlignedTopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha),
                    titleContentColor = MaterialTheme.colorScheme.primary,

                    ),
                title = {
                    Text(
                        "NoteSync",
                        textAlign = TextAlign.Center,
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .navigationBarsPadding()
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { telaAtual = "A fazer" },
                            modifier = Modifier
                                .fillMaxHeight(0.65f)
                        ) {
                            Icon(
                                Icons.Default.WorkHistory,
                                contentDescription = "A fazer"
                            )
                        }

                        Button(
                            onClick = {
                                tarefas = tarefas + Tarefa("Nova tarefa", telaAtual)
                            },
                            modifier = Modifier
                                .fillMaxHeight(0.90f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }

                        Button(
                            onClick = { telaAtual = "Feito" },
                            modifier = Modifier
                                .fillMaxHeight(0.65f)

                        ) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = "Feito"
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = telaAtual,
                label = "animacao_tela",
                transitionSpec = {
                    if (targetState == "Feito" && initialState == "A fazer") {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                }
            ) { telaAlvo ->
                val tituloSecao = when (telaAlvo) {
                    "A fazer" -> "A fazer"
                    "Feito" -> "Feito"
                    else -> "Tarefas"
                }
                val tarefasFiltradas = tarefas.filter { it.status == telaAlvo }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    )
                ) {
                    item {
                        CategoriaSection(tituloSecao, Color.Red, tarefasFiltradas)
                    }
                }
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

@Preview
@Composable
fun previewTasks() {
    ProjetoMobileTheme {
        MinhasTarefasApp()
    }
}