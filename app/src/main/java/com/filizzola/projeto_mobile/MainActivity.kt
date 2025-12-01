package com.filizzola.projeto_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.filizzola.projeto_mobile.ui.AddTaskScreen
import com.filizzola.projeto_mobile.ui.EditTaskScreen
import com.filizzola.projeto_mobile.ui.LoginScreen
import com.filizzola.projeto_mobile.ui.RegisterScreen
import com.filizzola.projeto_mobile.ui.TaskListScreen
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme
import com.filizzola.projeto_mobile.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint // <--- ISSO É OBRIGATÓRIO AGORA
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // O WorkManager agora usa o HiltWorkerFactory configurado no MyApplication
        schedulePeriodicSync()

        setContent {
            ProjetoMobileTheme {
                AppNavigation()
            }
        }
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NoteSyncPeriodicWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    // Nota: A lógica de sessão e redirecionamento idealmente ficaria em um MainViewModel
    // Para simplificar e fazer rodar, vamos iniciar no Login e deixar o LoginViewModel/TaskViewModel gerenciar o estado.
    val startDestination = Routes.LOGIN

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Routes.LOGIN) {
                    // O Hilt injeta o ViewModel automaticamente aqui dentro
                    LoginScreen(
                        onLoginSuccess = { userId ->
                            navController.navigate(Routes.tasks(userId)) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onRegisterClick = {
                            navController.navigate(Routes.REGISTER)
                        }
                    )
                }

                composable(
                    route = Routes.TASKS,
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""

                    // TaskListScreen deve ser atualizada para usar hiltViewModel() internamente ou receber via parâmetro
                    // Assumindo que sua TaskListScreen já usa viewModel() ou hiltViewModel():
                    TaskListScreen(
                        navController = navController,
                        userId = userId,
                        onLogout = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.TASKS) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = Routes.ADD_TASK,
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    AddTaskScreen(
                        navController = navController,
                        userId = userId
                    )
                }

                composable(
                    route = Routes.EDIT_TASK,
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType },
                        navArgument("taskId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    val taskId = backStackEntry.arguments?.getString("taskId")
                    EditTaskScreen(
                        navController = navController,
                        userId = userId,
                        taskId = taskId
                    )
                }

                composable(Routes.REGISTER) {
                    RegisterScreen(
                        onRegisterClick = { navController.popBackStack() },
                        onLoginBtnClick = { navController.popBackStack() }
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
    // fun addTask(userId: String) = "add_task/$userId" // Se precisar
    // fun editTask(userId: String, taskId: String) = "edit_task/$userId/$taskId"
}