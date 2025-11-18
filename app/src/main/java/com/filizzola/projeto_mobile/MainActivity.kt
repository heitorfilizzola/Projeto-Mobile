package com.filizzola.projeto_mobile

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.ui.AddTaskScreen
import com.filizzola.projeto_mobile.ui.EditTaskScreen
import com.filizzola.projeto_mobile.ui.GreetingLogin
import com.filizzola.projeto_mobile.ui.RegisterScreen
import com.filizzola.projeto_mobile.ui.TaskListScreen
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme
import com.filizzola.projeto_mobile.utils.LoginManager
import com.filizzola.projeto_mobile.utils.NetworkManager
import com.filizzola.projeto_mobile.utils.SyncManager
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SupabaseConfig {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = "https://hotdhewlluokhhxamydi.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhvdGRoZXdsbHVva2hoeGFteWRpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA3NTAxMDgsImV4cCI6MjA3NjMyNjEwOH0._dyd56suv0W-TK0AHHKDsQE82f3wyb9uQq4nZSlRtmc"
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}

val supabase get() = SupabaseConfig.client

class MainActivity : ComponentActivity() {

    private lateinit var networkManager: NetworkManager
    private lateinit var loginManager: LoginManager
    private lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        networkManager = NetworkManager(this)
        loginManager = LoginManager(this)
        syncManager = SyncManager(this)

        checkNetworkConnection()

        setContent {
            ProjetoMobileTheme {
                AppNavigation(loginManager, syncManager)
            }
        }
    }

    private fun checkNetworkConnection() {
        lifecycleScope.launch {
            networkManager.checkConnection { }
        }
    }
}

@Composable
private fun AppNavigation(loginManager: LoginManager, syncManager: SyncManager) {
    val navController = rememberNavController()
    var triggerRecomposition by remember { mutableStateOf(0) }
    var startDestination by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Configuração do JSON para ser compatível com o Supabase
    val json = remember { Json { ignoreUnknownKeys = true } }

    // --- LÓGICA DE INICIALIZAÇÃO ---
    LaunchedEffect(Unit) {
        val savedUserId = loginManager.getLoggedUser()
        val savedSessionJson = loginManager.getSession()

        if (savedUserId != null) {
            // 1. RESTAURAR SESSÃO (Isso resolve o erro 42501)
            if (savedSessionJson != null) {
                try {
                    val session = json.decodeFromString<UserSession>(savedSessionJson)
                    supabase.auth.importSession(session)
                    Log.d("Auth", "Sessão restaurada com sucesso!")
                } catch (e: Exception) {
                    Log.e("Auth", "Erro ao restaurar sessão. O usuário precisará logar novamente.", e)
                    // Se a sessão estiver corrompida, forçamos logout
                    loginManager.clearLoggedUser()
                    startDestination = Routes.LOGIN
                    return@LaunchedEffect
                }
            } else {
                // Se tem ID mas não tem Sessão (Login antigo), força logout para corrigir
                Log.w("Auth", "Login antigo detectado (sem token). Forçando logout.")
                loginManager.clearLoggedUser()
                startDestination = Routes.LOGIN
                return@LaunchedEffect
            }

            // 2. CACHE FIRST (OFFLINE)
            val localTasks = syncManager.loadTasksFromLocal(savedUserId)
            if (localTasks.isNotEmpty()) {
                UserRepository.loadFromCache(savedUserId, localTasks)
                startDestination = Routes.tasks(savedUserId)
            }

            // 3. BACKGROUND SYNC (ONLINE)
            launch {
                // Agora que importSession rodou, o syncUserData vai usar o token correto
                val syncedTasks = UserRepository.syncUserData(savedUserId)

                if (syncedTasks != null) {
                    syncManager.saveTaskLocally(savedUserId, syncedTasks)

                    // Atualiza o token salvo caso ele tenha mudado (refresh token)
                    val currentSession = supabase.auth.currentSessionOrNull()
                    if (currentSession != null) {
                        loginManager.saveSession(json.encodeToString(currentSession))
                    }

                    if (startDestination == null) {
                        startDestination = Routes.tasks(savedUserId)
                    }
                } else {
                    // Se falhou a rede e não tem cache, vai pro login
                    if (localTasks.isEmpty()) {
                        loginManager.clearLoggedUser()
                        startDestination = Routes.LOGIN
                    }
                }
            }
        } else {
            startDestination = Routes.LOGIN
        }
    }

    if (startDestination == null) {
        return
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination!!
            ) {
                composable(Routes.LOGIN) {
                    GreetingLogin(
                        modifier = Modifier.padding(innerPadding),
                        onNavigateToRegister = {
                            navController.navigate(Routes.REGISTER)
                        },
                        onLoginSuccess = { user ->
                            coroutineScope.launch {
                                // 1. Salva ID
                                loginManager.saveLoggedUser(user.id)

                                // 2. SALVA SESSÃO (Com Kotlinx Serialization)
                                val session = supabase.auth.currentSessionOrNull()
                                if (session != null) {
                                    val sessionString = json.encodeToString(session)
                                    loginManager.saveSession(sessionString)
                                    Log.d("Auth", "Sessão salva no disco.")
                                }

                                // 3. Sync e Navegação
                                syncManager.saveTaskLocally(user.id, user.uTaskList)
                                navController.navigate(Routes.tasks(user.id)) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                        },
                        onClickTaski = { }
                    )
                }

                composable(
                    route = Routes.TASKS,
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    backStackEntry.arguments?.getString("userId")?.let { userId ->
                        androidx.compose.runtime.key(triggerRecomposition) {
                            TaskListScreen(
                                navController = navController,
                                userId = userId,
                                onDeleteTask = { taskIdToDelete ->
                                    coroutineScope.launch {
                                        try { UserRepository.deleteTaskForUser(userId, taskIdToDelete) } catch (e: Exception) {}
                                        syncManager.persistCurrentData(userId)
                                        triggerRecomposition++
                                    }
                                },
                                onToggleTaskStatus = { taskToToggle ->
                                    coroutineScope.launch {
                                        try { UserRepository.toggleTaskStatusForUser(userId, taskToToggle.id) } catch (e: Exception) {}
                                        syncManager.persistCurrentData(userId)
                                        triggerRecomposition++
                                    }
                                },
                                onLogout = {
                                    coroutineScope.launch {
                                        loginManager.clearLoggedUser()
                                    }
                                }
                            )
                        }
                    }
                }

                composable(
                    route = Routes.ADD_TASK,
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    backStackEntry.arguments?.getString("userId")?.let { userId ->
                        AddTaskScreen(
                            navController = navController,
                            onAddTask = { novaTarefa ->
                                coroutineScope.launch {
                                    try { UserRepository.addTaskToUser(userId, novaTarefa) } catch (e: Exception) {}
                                    syncManager.persistCurrentData(userId)
                                    triggerRecomposition++
                                    navController.popBackStack()
                                }
                            },
                            userId = userId
                        )
                    }
                }

                composable(
                    route = Routes.EDIT_TASK,
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType },
                        navArgument("taskId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")
                    val taskId = backStackEntry.arguments?.getString("taskId")
                    if (userId != null) {
                        EditTaskScreen(
                            navController = navController,
                            onEditTask = { tarefaAtualizada ->
                                coroutineScope.launch {
                                    try { UserRepository.updateTaskForUser(userId, tarefaAtualizada) } catch (e: Exception) {}
                                    syncManager.persistCurrentData(userId)
                                    triggerRecomposition++
                                    navController.popBackStack()
                                }
                            },
                            userId = userId,
                            taskId = taskId
                        )
                    }
                }

                composable(Routes.REGISTER) {
                    RegisterScreen(
                        modifier = Modifier.padding(innerPadding),
                        onRegisterClick = { navController.navigate(Routes.LOGIN) },
                        onLoginBtnClick = { navController.navigate(Routes.LOGIN) }
                    )
                }
            }
        }
    }
}

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val TASKS = "tasks/{userId}"
    const val ADD_TASK = "add_task/{userId}"
    const val EDIT_TASK = "edit_task/{userId}/{taskId}"

    fun tasks(userId: String) = "tasks/$userId"
    fun addTask(userId: String) = "add_task/$userId"
    fun editTask(userId: String, taskId: String) = "edit_task/$userId/$taskId"
}