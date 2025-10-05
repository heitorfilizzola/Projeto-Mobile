package com.filizzola.projeto_mobile.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.UserRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    userId: String
) {
    var telaAtual by remember { mutableStateOf("A fazer") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val user = UserRepository.allUsers.find { it.id == userId }
    val tarefas = user?.uTaskList ?: emptyList()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val alpha = 1f - scrollBehavior.state.collapsedFraction
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha),
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("NoteSync", textAlign = TextAlign.Center) },
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
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { telaAtual = "A fazer" }, modifier = Modifier.fillMaxHeight(0.65f)) {
                            Icon(Icons.Default.WorkHistory, "A fazer")
                        }

                        Button(
                            onClick = { navController.navigate("add_task/$userId") },
                            modifier = Modifier.fillMaxHeight(0.90f)
                        ) {
                            Icon(Icons.Default.Add, "Adicionar Tarefa")
                        }

                        Button(onClick = { telaAtual = "Feito" }, modifier = Modifier.fillMaxHeight(0.65f)) {
                            Icon(Icons.Default.Done, "Feito")
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
                val tarefasFiltradas = tarefas.filter { it.status == telaAlvo }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    )
                ) {
                    item {
                        CategoriaSection(telaAlvo, tarefasFiltradas)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onAddTask: (Tarefa) -> Unit,
    navController: NavController,
    userId: String
) {
    var titulo by remember { mutableStateOf("") }
    val status = "A fazer"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Nova Tarefa") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título da Tarefa") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (titulo.isNotBlank()) {
                        onAddTask(Tarefa(titulo = titulo, status = status, userId = userId))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = titulo.isNotBlank()
            ) {
                Text("Salvar Tarefa")
            }
        }
    }
}


@Composable
fun CategoriaSection(titulo: String, tarefas: List<Tarefa>) {
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tarefa.titulo)
                        Row {
                            IconButton(onClick = { /* editar */ }) {
                                Icon(Icons.Filled.Edit, "Editar", tint = Color.Blue)
                            }
                            IconButton(onClick = { /* excluir */ }) {
                                Icon(Icons.Filled.Delete, "Excluir", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}