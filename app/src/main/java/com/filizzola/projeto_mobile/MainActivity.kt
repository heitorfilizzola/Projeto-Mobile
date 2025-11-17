package com.filizzola.projeto_mobile

import android.os.Bundle
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
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.launch

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
            val hasConnection = networkManager.checkConnection { errorMessage ->
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
            }

            if (hasConnection) {
                val userId = loginManager.getLoggedUser()
                if (userId != null) {
                    val synced = syncManager.syncWithServer(userId)
                    if (synced) {
                        Toast.makeText(
                            this@MainActivity,
                            "Dados sincronizados",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(loginManager: LoginManager, syncManager: SyncManager) {
    val navController = rememberNavController()
    var triggerRecomposition by remember { mutableStateOf(0) }
    var startDestination by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val savedUserId = loginManager.getLoggedUser()
        startDestination = if (savedUserId != null) {
            Routes.tasks(savedUserId)
        } else {
            Routes.LOGIN
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
                                loginManager.saveLoggedUser(user.id)
                                syncManager.syncWithServer(user.id)
                                navController.navigate(Routes.tasks(user.id)) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                        },
                        onClickTaski = {
                            UserRepository.allUsers.firstOrNull()?.let { user ->
                                navController.navigate(Routes.tasks(user.id))
                            }
                        }
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
                                        syncManager.deleteTaskLocally(taskIdToDelete)
                                        UserRepository.deleteTaskForUser(userId, taskIdToDelete)
                                        syncManager.syncWithServer(userId)
                                        triggerRecomposition++
                                    }
                                },
                                onToggleTaskStatus = { taskToToggle ->
                                    coroutineScope.launch {
                                        UserRepository.toggleTaskStatusForUser(userId, taskToToggle.id)
                                        syncManager.syncWithServer(userId)
                                        triggerRecomposition++
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
                                    UserRepository.addTaskToUser(userId, novaTarefa)
                                    syncManager.syncWithServer(userId)
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
                                    UserRepository.updateTaskForUser(userId, tarefaAtualizada)
                                    syncManager.syncWithServer(userId)
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