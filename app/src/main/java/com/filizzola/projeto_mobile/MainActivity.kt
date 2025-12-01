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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.filizzola.projeto_mobile.worker.SyncWorker


object SupabaseConfig {
    val client by lazy {
        createSupabaseClient(
//            supabaseUrl = "https://xyzcompany.supabase.co",
//            supabaseKey = "publishable-or-anon-key"
            supabaseUrl = "https://hotdhewlluokhhxamydi.supabase.co/",
            supabaseKey = "sb_publishable_Te2ter0ZFhL4kZKozwFgEA_aFhW7_lD"
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
        schedulePeriodicSync()

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
private fun AppNavigation(loginManager: LoginManager, syncManager: SyncManager) {
    val navController = rememberNavController()
    var triggerRecomposition by remember { mutableStateOf(0) }
    var startDestination by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val json = remember { Json { ignoreUnknownKeys = true } }

    LaunchedEffect(Unit) {
        val savedUserId = loginManager.getLoggedUser()
        val savedSessionJson = loginManager.getSession()

        if (savedUserId != null) {
            if (savedSessionJson != null) {
                try {
                    val session = json.decodeFromString<UserSession>(savedSessionJson)
                    supabase.auth.importSession(session)
                    Log.d("Auth", "Sessão restaurada com sucesso!")
                } catch (e: Exception) {
                    Log.e("Auth", "Erro ao restaurar sessão.", e)
                }
            }

            val localTasks = syncManager.loadTasksFromLocal(savedUserId)
            if (localTasks.isNotEmpty()) {
                UserRepository.loadFromCache(savedUserId, localTasks)
                startDestination = Routes.tasks(savedUserId)
            }

            launch {
                val syncedTasks = UserRepository.syncUserData(savedUserId)

                if (syncedTasks != null) {
                    syncManager.saveTaskLocally(savedUserId, syncedTasks)

                    val currentSession = supabase.auth.currentSessionOrNull()
                    if (currentSession != null) {
                        loginManager.saveSession(json.encodeToString(currentSession))
                    }

                    if (startDestination == null) {
                        startDestination = Routes.tasks(savedUserId)
                    }
                } else {
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
                                loginManager.saveLoggedUser(user.id)

                                val session = supabase.auth.currentSessionOrNull()
                                if (session != null) {
                                    val sessionString = json.encodeToString(session)
                                    loginManager.saveSession(sessionString)
                                    Log.d("Auth", "Sessão salva no disco.")
                                }

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
                        TaskListScreen(
                            navController = navController,
                            userId = userId,
                            onLogout = {
                                coroutineScope.launch {
                                    loginManager.clearLoggedUser()
                                }
                            }
                        )
                    }
                }

                composable(
                    route = Routes.ADD_TASK,
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    backStackEntry.arguments?.getString("userId")?.let { userId ->
                        AddTaskScreen(
                            navController = navController,
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