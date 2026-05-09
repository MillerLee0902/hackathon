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

                    // 啟動時依已存的 role 決定起始頁
                    val startDestination = when {
                        !session.isLoggedIn() -> "login"
                        session.isStaff() -> "staffDashboard"
                        else -> "dashboard"
                    }

                    NavHost(navController = navController, startDestination = startDestination) {

                        // ─── 登入 ───────────────────────────────────────
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { role ->
                                    val dest = if (role == "staff" || role == "admin") "staffDashboard" else "dashboard"
                                    navController.navigate(dest) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = { navController.navigate("register") },
                            )
                        }

                        // ─── 註冊（一般用戶，不開放店員） ────────────────
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

                        // ─── 一般用戶頁面 ────────────────────────────────
                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToTransactions = { navController.navigate("transactions") },
                                onNavigateToStaffDashboard = {
                                    navController.navigate("staffDashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                                onLogout = {
                                    session.clearAll()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                },
                            )
                        }

                        composable("transactions") {
                            TransactionScreen(onBack = { navController.popBackStack() })
                        }

                        // ─── 店員頁面 ────────────────────────────────────
                        composable("staffDashboard") {
                            StaffDashboardScreen(
                                onNavigateToBorrow = { navController.navigate("staffBorrow") },
                                onNavigateToReturn = { navController.navigate("staffReturn") },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo("staffDashboard") { inclusive = true }
                                    }
                                },
                            )
                        }

                        composable("staffBorrow") {
                            StaffBorrowScreen(onBack = { navController.popBackStack() })
                        }

                        composable("staffReturn") {
                            StaffReturnScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
