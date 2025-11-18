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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    userId: String,
    onDeleteTask: (taskId: String) -> Unit,
    onToggleTaskStatus: (task: Tarefa) -> Unit,
    onLogout: () -> Unit
) {
    var telaAtual by rememberSaveable { mutableStateOf("A fazer") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // Observando a lista de forma mais segura para reagir a mudanças
    val user = UserRepository.allUsers.find { it.id == userId }
    val tarefas = user?.uTaskList ?: emptyList()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                                    try {
                                        UserRepository.logout()
                                    } catch (e: Exception) {
                                        Log.e("LogoutError", "Logout offline", e)
                                    }
                                    onLogout()
                                    Toast.makeText(context, "Saiu da conta", Toast.LENGTH_SHORT).show()
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = true
                                        }
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
                // Filtra a lista baseada na aba atual
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

                    // CORREÇÃO CRUCIAL: Usar 'items' com 'key'.
                    // Isso impede que o Compose confunda os itens ao deletar/mover.
                    items(
                        items = tarefasFiltradas,
                        key = { it.id }
                    ) { tarefa ->
                        TaskItem(
                            tarefa = tarefa,
                            isToDoList = (telaAlvo == "A fazer"),
                            navController = navController,
                            onDeleteTask = onDeleteTask,
                            onToggleTaskStatus = onToggleTaskStatus
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
    onToggleTaskStatus: (task: Tarefa) -> Unit
) {
    // Estado do swipe
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (isToDoList) {
                // --- LISTA "A FAZER" ---
                when (it) {
                    SwipeToDismissBoxValue.EndToStart -> { // Dir -> Esq (Vermelho)
                        onDeleteTask(tarefa.id)
                        true
                    }
                    SwipeToDismissBoxValue.StartToEnd -> { // Esq -> Dir (Verde)
                        onToggleTaskStatus(tarefa)
                        true
                    }
                    else -> false
                }
            } else {
                // --- LISTA "FEITO" ---
                when (it) {
                    SwipeToDismissBoxValue.StartToEnd -> { // Esq -> Dir (Azul/Voltar)
                        onToggleTaskStatus(tarefa) // Ação de voltar para "A Fazer"
                        true
                    }
                    SwipeToDismissBoxValue.EndToStart -> { // Dir -> Esq (Vermelho/Apagar)
                        onDeleteTask(tarefa.id)
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
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF1B5E20) // Verde (Concluir)
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFB71C1C) // Vermelho (Apagar)
                        else -> Color.Transparent
                    }
                } else {
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF1C6FB7) // Azul (Retornar)
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFB71C1C) // Vermelho (Apagar)
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
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Restore // Ícone de voltar
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
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
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    navController: NavController,
    onEditTask: (Tarefa) -> Unit,
    userId: String,
    taskId: String?
) {
    val user = UserRepository.allUsers.find { it.id == userId }
    val taskToEdit = user?.uTaskList?.find { it.id == taskId }

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
                        onEditTask(updatedTask)
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
