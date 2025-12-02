package com.filizzola.projeto_mobile.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.filizzola.projeto_mobile.data.Tarefa
import com.filizzola.projeto_mobile.data.User
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
    val user by taskViewModel.user.collectAsState()
    val syncConsent by taskViewModel.syncConsent.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        taskViewModel.loadTasks(userId)
    }

    val context = LocalContext.current

    TaskListContent(
        userId = userId,
        user = user,
        tasks = tarefas,
        syncConsent = syncConsent,
        onConsentChange = { isGranted -> taskViewModel.updateSyncConsent(isGranted) },
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
                val message = if (success) "Sincronizado com sucesso!" else "Falha ou permissão negada."
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            })
        },
        onConfirmPasswordChange = { newPassword ->
            taskViewModel.changePassword(newPassword) { success ->
                val message = if (success) "Senha alterada com sucesso!" else "Erro ao alterar senha."
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListContent(
    userId: String,
    user: User?,
    tasks: List<Tarefa>,
    syncConsent: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (Tarefa) -> Unit,
    onDeleteTask: (String) -> Unit,
    onStatusChange: (Tarefa) -> Unit,
    onSyncClick: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Diálogo de Troca de Senha
    if (showChangePasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Trocar Senha") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nova Senha") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = passwordError
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = newPassword != it
                        },
                        label = { Text("Confirmar Nova Senha") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = passwordError
                    )
                    if (passwordError) {
                        Text("As senhas não coincidem.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword.isNotBlank() && newPassword == confirmPassword) {
                            onConfirmPasswordChange(newPassword)
                            showChangePasswordDialog = false
                        } else {
                            passwordError = true
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Configurações
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Configurações de Privacidade") },
            text = {
                Column {
                    Text("Gerencie como seus dados são tratados.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sincronizar dados na nuvem",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = syncConsent,
                            onCheckedChange = { isChecked ->
                                onConsentChange(isChecked)
                            }
                        )
                    }
                    if (!syncConsent) {
                        Text(
                            text = "A sincronização está desativada. Seus dados permanecerão apenas neste dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Text(
                            text = "Seus dados serão copiados para a nuvem para backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Diálogo de Perfil
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(28.dp))
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("Perfil do Usuário")
                 }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = user?.username ?: "Usuário",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user?.email ?: "Email não disponível",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    TextButton(
                        onClick = { 
                            showProfileDialog = false
                            showChangePasswordDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                         Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                             Spacer(modifier = Modifier.width(12.dp))
                             Text("Trocar Senha", color = MaterialTheme.colorScheme.onSurface)
                         }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            showProfileDialog = false
                            onLogoutClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                         Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                         Spacer(modifier = Modifier.width(8.dp))
                        Text("Sair da Conta")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

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
                    Text("NoteSync", textAlign = TextAlign.Center)
                },
                navigationIcon = {
                    // Botão de Perfil (antigo Logout)
                     IconButton(onClick = { showProfileDialog = true }) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.username?.firstOrNull()?.uppercase() ?: "U",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Botão de Sync
                    IconButton(onClick = {
                        scope.launch {
                            if (syncConsent) {
                                onSyncClick(userId)
                            } else {
                                android.widget.Toast.makeText(context, "Habilite a sincronização nas configurações.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sincronizar",
                            tint = if (syncConsent) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    // Botão de Configurações
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                    tonalElevation = 2.dp
                ) {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = {
                            Icon(
                                imageVector = if (pagerState.currentPage == 0) Icons.Filled.WorkHistory else Icons.Outlined.WorkHistory,
                                contentDescription = "A fazer"
                            )
                        },
                        label = { Text("A fazer", fontWeight = if(pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = onAddTaskClick,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Criar")
                        }
                    }

                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = {
                            Icon(
                                imageVector = if (pagerState.currentPage == 1) Icons.Filled.TaskAlt else Icons.Outlined.TaskAlt,
                                contentDescription = "Feito"
                            )
                        },
                        label = { Text("Feito", fontWeight = if(pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val telaAlvo = if (page == 0) "A fazer" else "Feito"
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
    onEditClick: () -> Unit,
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
    val context = LocalContext.current
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
    val context = LocalContext.current
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