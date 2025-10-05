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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                                        navController.navigate("tasks")
                                    },
                                    onClickTaski = {
                                        if (UserRepository.allUsers.isNotEmpty()) {
                                            currentUser = UserRepository.allUsers.first()
                                            navController.navigate("tasks")
                                        }
                                    }
                                )
                            }

                            composable(route = "tasks") {
                                currentUser?.let { user ->
                                    TaskListScreen(
                                        navController = navController,
                                        tarefas = user.uTaskList
                                    )
                                }
                            }

                            composable(route = "add_task") {
                                AddTaskScreen(
                                    navController = navController,
                                    onAddTask = { novaTarefa ->
                                        currentUser?.let { user ->
                                            val updatedTasks = user.uTaskList + novaTarefa
                                            val updatedUser = user.copy(uTaskList = updatedTasks)

                                            currentUser = updatedUser

                                            val userIndex = UserRepository.allUsers.indexOfFirst { it.id == user.id }
                                            if (userIndex != -1) {
                                                UserRepository.allUsers[userIndex] = updatedUser
                                            }
                                        }
                                        navController.popBackStack()
                                    }
                                )
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