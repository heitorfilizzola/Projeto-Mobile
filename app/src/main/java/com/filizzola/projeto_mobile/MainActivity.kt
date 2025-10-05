package com.filizzola.projeto_mobile

import android.os.Bundle
import android.util.Log
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
import com.filizzola.projeto_mobile.ui.GreetingLogin
import com.filizzola.projeto_mobile.ui.RegisterScreen
import com.filizzola.projeto_mobile.ui.theme.ProjetoMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjetoMobileTheme {
                val navController = rememberNavController()

                var currentUser by remember { mutableStateOf<User?>(null) }
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
                                        currentUser = user
                                        navController.navigate("tasks/${user.id}")
                                    },
                                    onClickTaski = {
                                        if (UserRepository.allUsers.isNotEmpty()) {
                                            val user = UserRepository.allUsers.first()
                                            currentUser = user
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
                                    TaskListScreen(
                                        navController = navController,
                                        userId = userId
                                    )
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
                                            Log.d("newTask", "Tarefa ${novaTarefa.id} adicionada ao usuario $userId")
                                            navController.popBackStack()
                                        },
                                        userId = userId
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