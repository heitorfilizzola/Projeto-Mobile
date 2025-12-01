package com.filizzola.projeto_mobile.ui

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
import com.filizzola.projeto_mobile.data.Tarefa
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filizzola.projeto_mobile.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun TaskListScreen(
    navController: NavController,
    userId: String,
    onLogout: () -> Unit,
    taskViewModel: TaskViewModel = viewModel()
) {
    val tarefas by taskViewModel.tasks.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        taskViewModel.loadTasks(userId)
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    TaskListContent(
        userId = userId, // Pass userId
        tasks = tarefas,
        onLogoutClick = {
            scope.launch {
                taskViewModel.logout()
                onLogout()
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        },
        onAddTaskClick = { navController.navigate("add_task/$userId") },
        onEditTaskClick = { task -> navController.navigate("edit_task/${task.userId}/${task.id}") },
        onDeleteTask = { taskId -> taskViewModel.deleteTask(userId, taskId) },
        onStatusChange = { task -> taskViewModel.changeTaskStatus(userId, task) },
        onSyncClick = { id ->
            taskViewModel.syncTasks(userId = id, onResult = { success: Boolean ->
                val message = if (success) "Sincronizado com sucesso!" else "Falha na sincronização. Verifique sua conexão."
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            })
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun TaskListContent(
    userId: String, // Receive userId
    tasks: List<Tarefa>,
    onLogoutClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (Tarefa) -> Unit,
    onDeleteTask: (String) -> Unit,
    onStatusChange: (Tarefa) -> Unit,
    onSyncClick: (String) -> Unit // Receive sync function
) {
    var telaAtual by rememberSaveable { mutableStateOf("A fazer") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope() // Add coroutine scope here for toast

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
                    Text("NoteSync", textAlign = TextAlign.Center) // Center title
                },
                navigationIcon = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.Logout, contentDescription = "LogOut")
                    }
                },
                actions = { // Add actions block for buttons on the right
                    IconButton(onClick = {
                        scope.launch {
                            onSyncClick(userId)
                        }
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar")
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
                            onClick = onAddTaskClick,
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
                val tarefasFiltradas = tasks.filter { it.status == telaAlvo }

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
                            onEditClick = { onEditTaskClick(tarefa) },
                            onDeleteTask = onDeleteTask,
                            onStatusChange = onStatusChange
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
    onEditClick: () -> Unit, // Removido NavController, agora recebe uma função
    onDeleteTask: (taskId: String) -> Unit,
    onStatusChange: (task: Tarefa) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (isToDoList) {
                when (it) {
                    SwipeToDismissBoxValue.EndToStart -> { onDeleteTask(tarefa.id); true }
                    SwipeToDismissBoxValue.StartToEnd -> { onStatusChange(tarefa); true }
                    else -> false
                }
            } else {
                when (it) {
                    SwipeToDismissBoxValue.StartToEnd -> { onDeleteTask(tarefa.id); true }
                    SwipeToDismissBoxValue.EndToStart -> { onStatusChange(tarefa); true }
                    else -> false
                }
            }
        }
    )

    var expanded by remember { mutableStateOf(false) }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection ?: return@SwipeToDismissBox
            val color by animateColorAsState(
                if (isToDoList) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                } else {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) Color(0xFFB71C1C) else Color(0xFF1C6FB7)
                }, label = "color"
            )
            val icon = if(isToDoList) if(direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Done else Icons.Default.Delete else if(direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Delete else Icons.Default.Restore
            val alignment = if(direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

            Box(
                modifier = Modifier.fillMaxSize().background(color, MaterialTheme.shapes.large).padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let { Icon(it, null, tint = Color.White) }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = MaterialTheme.shapes.large,
            onClick = { expanded = !expanded }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = tarefa.titulo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, "Editar", tint = Color.Gray)
                    }
                }

                if (expanded) {
                    if (tarefa.desc.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        Text(
                            text = tarefa.desc,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Sem descrição",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun AddTaskScreen(
    navController: NavController,
    userId: String,
    taskViewModel: TaskViewModel = viewModel()
) {
    AddTaskContent(
        onBackClick = { navController.popBackStack() },
        onSaveTask = { titulo, desc, dueDate ->
            taskViewModel.addTask(
                userId,
                Tarefa(titulo = titulo, desc = desc, status = "A fazer", userId = userId, dueDate = dueDate)
            )
            navController.popBackStack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskContent(
    onBackClick: () -> Unit,
    onSaveTask: (String, String, Long?) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = java.util.Calendar.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Nova Tarefa") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Descrição (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Button(
                onClick = {
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            calendar.set(java.util.Calendar.YEAR, year)
                            calendar.set(java.util.Calendar.MONTH, month)
                            calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)

                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                    calendar.set(java.util.Calendar.MINUTE, minute)
                                    dueDate = calendar.timeInMillis
                                },
                                calendar.get(java.util.Calendar.HOUR_OF_DAY),
                                calendar.get(java.util.Calendar.MINUTE),
                                true
                            ).show()
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dueDate?.let {
                        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(it)
                    } ?: "Definir Lembrete"
                )
            }

            if (dueDate != null) {
                TextButton(onClick = { dueDate = null }) {
                    Text("Remover Lembrete", color = MaterialTheme.colorScheme.error)
                }
            }

            Button(
                onClick = {
                    if (titulo.isNotBlank() && !isSaving) {
                        isSaving = true
                        onSaveTask(titulo, desc, dueDate)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = titulo.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Salvar Tarefa")
                }
            }
        }
    }
}
@Composable
fun EditTaskScreen(
    navController: NavController,
    userId: String,
    taskId: String?,
    taskViewModel: TaskViewModel = viewModel()
) {
    LaunchedEffect(userId) {
        taskViewModel.loadTasks(userId)
    }

    val tasks by taskViewModel.tasks.collectAsState()
    val taskToEdit = tasks.find { it.id == taskId }

    EditTaskContent(
        taskToEdit = taskToEdit,
        onBackClick = { navController.popBackStack() },
        onSaveClick = { titulo, desc, dueDate ->
            if (taskToEdit != null) {
                val updatedTask = taskToEdit.copy(titulo = titulo, desc = desc, dueDate = dueDate)
                taskViewModel.updateTask(userId, updatedTask)
                navController.popBackStack()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskContent(
    taskToEdit: Tarefa?,
    onBackClick: () -> Unit,
    onSaveClick: (String, String, Long?) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = java.util.Calendar.getInstance()

    LaunchedEffect(taskToEdit) {
        if (taskToEdit != null) {
            titulo = taskToEdit.titulo
            desc = taskToEdit.desc
            dueDate = taskToEdit.dueDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Tarefa") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
            if (taskToEdit == null) {
                CircularProgressIndicator()
                Text("Carregando tarefa...", color = Color.Gray)
            } else {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título da Tarefa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Button(
                    onClick = {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(java.util.Calendar.YEAR, year)
                                calendar.set(java.util.Calendar.MONTH, month)
                                calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)

                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(java.util.Calendar.MINUTE, minute)
                                        dueDate = calendar.timeInMillis
                                    },
                                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                                    calendar.get(java.util.Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dueDate?.let {
                            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(it)
                        } ?: "Definir Lembrete"
                    )
                }

                if (dueDate != null) {
                    TextButton(onClick = { dueDate = null }) {
                        Text("Remover Lembrete", color = MaterialTheme.colorScheme.error)
                    }
                }

                Button(
                    onClick = {
                        if (titulo.isNotBlank() && !isSaving) {
                            isSaving = true
                            onSaveClick(titulo, desc, dueDate)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = titulo.isNotBlank() && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Salvar Alterações")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AddTaskPreview() {
    MaterialTheme {
        AddTaskContent(onBackClick = {}, onSaveTask = { _, _, _ -> })
    }
}