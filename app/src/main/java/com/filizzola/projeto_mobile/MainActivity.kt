package com.filizzola.projeto_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.filizzola.projeto_mobile.data.User
import com.filizzola.projeto_mobile.data.UserRepository
import com.filizzola.projeto_mobile.ui.*
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://xyzcompany.supabase.co",
    supabaseKey = "publishable-or-anon-key"
) {
    install(Auth)
    install(Postgrest)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjetoMobileTheme {
                val navController = rememberNavController()
                var triggerRecomposition by remember { mutableStateOf(0) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = "login"
                        ) {
                            composable("login") {
                                GreetingLogin(
                                    modifier = Modifier.padding(innerPadding),
                                    onNavigateToRegister = { navController.navigate("register") },
                                    onLoginSuccess = { user ->
                                        navController.navigate("tasks/${user.id}") {
                                            popUpTo("login") {
                                                inclusive =
                                                    true // 'true' significa que a própria tela de "login" também será removida.
                                            }
                                        }
                                    },
                                    onClickTaski = {
                                        if (UserRepository.allUsers.isNotEmpty()) {
                                            val user = UserRepository.allUsers.first()
                                            navController.navigate("tasks/${user.id}")
                                        }
                                    }
                                )
                            }

                            composable(
                                route = "tasks/{userId}",
                                arguments = listOf(navArgument("userId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId")
                                if (userId != null) {
                                    // MODIFICADO: A chave `key` garante que o Composable seja
                                    // recriado quando o `triggerRecomposition` mudar.
                                    key(triggerRecomposition) {
                                        TaskListScreen(
                                            navController = navController,
                                            userId = userId,
                                            onDeleteTask = { taskIdToDelete ->
                                                UserRepository.deleteTaskForUser(userId, taskIdToDelete)
                                                // Altera o estado para forçar a atualização
                                                triggerRecomposition++
                                            },
                                            // ADICIONADO: Passando a nova função de swipe
                                            onToggleTaskStatus = { taskToToggle ->
                                                UserRepository.toggleTaskStatusForUser(userId, taskToToggle.id)
                                                // Altera o estado para forçar a atualização
                                                triggerRecomposition++
                                            }
                                        )
                                    }
                                }
                            }

                            composable(
                                "add_task/{userId}",
                                arguments = listOf(navArgument("userId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val userId = backStackEntry.arguments?.getString("userId")
                                if (userId != null) {
                                    AddTaskScreen(
                                        navController = navController,
                                        onAddTask = { novaTarefa ->
                                            UserRepository.addTaskToUser(userId, novaTarefa)
                                            // Força a atualização da tela anterior ao voltar
                                            triggerRecomposition++
                                            navController.popBackStack()
                                        },
                                        userId = userId
                                    )
                                }
                            }

                            composable(
                                "edit_task/{userId}/{taskId}",
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
                                            UserRepository.updateTaskForUser(userId, tarefaAtualizada)
                                            // Força a atualização da tela anterior ao voltar
                                            triggerRecomposition++
                                            navController.popBackStack()
                                        },
                                        userId = userId,
                                        taskId = taskId
                                    )
                                }
                            }

                            composable("register") {
                                RegisterScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    onRegisterClick = { navController.navigate("login") },
                                    onLoginBtnClick = { navController.navigate("login") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}