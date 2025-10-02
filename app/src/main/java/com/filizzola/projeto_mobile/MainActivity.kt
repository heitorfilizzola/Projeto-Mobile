package com.filizzola.projeto_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "login"
                        ) {
                            composable("login") {
                                GreetingLogin(
                                    modifier = Modifier.padding(innerPadding),
                                    onLoginClick = {
                                        navController.navigate("register")
                                    }
                                )
                            }

                            composable("register") {
                                RegisterScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    onRegisterClick = {
                                        navController.navigate("login")
                                    }
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}
