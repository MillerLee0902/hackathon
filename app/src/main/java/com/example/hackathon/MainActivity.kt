package com.example.hackathon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hackathon.data.SessionManager
import com.example.hackathon.ui.*
import com.example.hackathon.ui.theme.HackathonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HackathonTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val session = remember { SessionManager(context) }

                    val startDestination = if (session.isLoggedIn()) "dashboard" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = { navController.navigate("register") },
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = { navController.popBackStack() },
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToBorrow = { navController.navigate("borrow") },
                                onNavigateToReturn = { navController.navigate("return") },
                                onNavigateToMyQrCode = { navController.navigate("myqrcode") },
                                onNavigateToTransactions = { navController.navigate("transactions") },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                            )
                        }

                        composable("borrow") {
                            BorrowScreen(onBack = { navController.popBackStack() })
                        }

                        composable("return") {
                            ReturnScreen(onBack = { navController.popBackStack() })
                        }

                        composable("myqrcode") {
                            MyQrCodeScreen(onBack = { navController.popBackStack() })
                        }

                        composable("transactions") {
                            TransactionScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
