package com.filizzola.projeto_mobile.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.User
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filizzola.projeto_mobile.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    userId: String,
    onLogout: () -> Unit,
    taskViewModel: TaskViewModel = viewModel()
) {
    var telaAtual by rememberSaveable { mutableStateOf("A fazer") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(userId) {
        taskViewModel.loadTasks(userId)
    }

    val tarefas by taskViewModel.tasks.collectAsState()

    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val alpha = 1f - scrollBehavior.state.collapsedFraction
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha),
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        IconButton(
                            onClick = {
                                scope.launch {
                                    taskViewModel.logout()
                                    onLogout()
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "LogOut")
                        }
                        Text("NoteSync", textAlign = TextAlign.Center)
                    }
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
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { telaAtual = "A fazer" }, modifier = Modifier.fillMaxHeight(0.7f)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.WorkHistory, "A fazer")
                                Text("A fazer", fontSize = 8.sp)
                            }
                        }

                        Button(
                            onClick = { navController.navigate("add_task/$userId") },
                            modifier = Modifier.fillMaxHeight(0.90f)
                        ){
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, "Criar")
                                Text("Criar", fontSize = 11.sp)
                            }
                        }

                        Button(onClick = { telaAtual = "Feito" }, modifier = Modifier.fillMaxHeight(0.7f)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Done, "Feito")
                                Text("Feito", fontSize = 8.sp)
                            }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = telaAlvo,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(
                        items = tarefasFiltradas,
                        key = { it.id }
                    ) { tarefa ->
                        TaskItem(
                            tarefa = tarefa,
                            isToDoList = (telaAlvo == "A fazer"),
                            navController = navController,
                            onDeleteTask = { taskViewModel.deleteTask(userId, it) },
                            onStatusChange = { taskViewModel.changeTaskStatus(userId, it) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    tarefa: Tarefa,
    isToDoList: Boolean,
    navController: NavController,
    onDeleteTask: (taskId: String) -> Unit,
    onStatusChange: (task: Tarefa) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (isToDoList) {
                when (it) {
                    SwipeToDismissBoxValue.EndToStart -> { // Apagar
                        onDeleteTask(tarefa.id)
                        true
                    }
                    SwipeToDismissBoxValue.StartToEnd -> { // Concluir
                        onStatusChange(tarefa)
                        true
                    }
                    else -> false
                }
            } else {
                when (it) {
                    SwipeToDismissBoxValue.StartToEnd -> { // ESQ -> DIR (Apagar)
                        onDeleteTask(tarefa.id)
                        true
                    }
                    SwipeToDismissBoxValue.EndToStart -> { // DIR -> ESQ (Retornar)
                        onStatusChange(tarefa)
                        true
                    }
                    else -> false
                }
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismissBox

            val color by animateColorAsState(
                if (isToDoList) {
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF1B5E20) // Verde
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFB71C1C) // Vermelho
                        else -> Color.Transparent
                    }
                } else {
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFFB71C1C) // Vermelho (Apagar)
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFF1C6FB7) // Azul (Retornar)
                        else -> Color.Transparent
                    }
                },
                label = "color_animation"
            )

            val icon = if (isToDoList) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Done
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    else -> null
                }
            } else {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Restore // Ícone de voltar
                    else -> null
                }
            }

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                label = "scale_animation"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = MaterialTheme.shapes.large)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = Color.White, modifier = Modifier.scale(scale))
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(tarefa.titulo)
                IconButton(onClick = {
                    navController.navigate("edit_task/${tarefa.userId}/${tarefa.id}")
                }) {
                    Icon(Icons.Filled.Edit, "Editar", tint = Color.Gray)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    navController: NavController,
    userId: String,
    taskViewModel: TaskViewModel = viewModel()
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
                        taskViewModel.addTask(userId, Tarefa(titulo = titulo, status = status, userId = userId))
                        navController.popBackStack()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    navController: NavController,
    userId: String,
    taskId: String?,
    taskViewModel: TaskViewModel = viewModel()
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val taskToEdit = tasks.find { it.id == taskId }

    var titulo by remember { mutableStateOf(taskToEdit?.titulo ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Tarefa") },
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
                    if (titulo.isNotBlank() && taskToEdit != null) {
                        val updatedTask = taskToEdit.copy(titulo = titulo)
                        taskViewModel.updateTask(userId, updatedTask)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = titulo.isNotBlank()
            ) {
                Text("Salvar Alterações")
            }
        }
    }
}
