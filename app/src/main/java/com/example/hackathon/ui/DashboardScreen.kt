package com.example.hackathon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathon.data.RetrofitClient
import com.example.hackathon.data.SessionManager
import com.example.hackathon.model.UserInfo
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToBorrow: () -> Unit,
    onNavigateToReturn: () -> Unit,
    onNavigateToMyQrCode: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    fun loadUserInfo() {
        isLoading = true
        scope.launch {
            try {
                val token = session.getToken() ?: return@launch
                val response = RetrofitClient.api.getMe(token)
                if (response.isSuccessful) {
                    userInfo = response.body()
                } else {
                    errorMessage = "無法載入用戶資料"
                }
            } catch (e: Exception) {
                errorMessage = "連線失敗"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadUserInfo() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("環保餐具借還", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = {
                session.clearToken()
                onLogout()
            }) {
                Icon(Icons.Default.ExitToApp, contentDescription = "登出", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (userInfo != null) {
            val user = userInfo!!

            // 用戶資訊卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("您好，${user.username}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(label = "點數", value = "${user.points} pt", icon = Icons.Default.Star)
                        StatItem(label = "錢包", value = "\$${String.format("%.0f", user.walletBalance)}", icon = Icons.Default.AccountBalanceWallet)
                        StatItem(label = "借用中", value = "${user.borrowedCount} 件", icon = Icons.Default.ShoppingBag)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 點數說明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Text(
                    "歸還環保餐具可獲得 1 點，點數未來可折抵消費或兌換優惠",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("功能選單", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            // 功能按鈕
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuButton(
                    modifier = Modifier.weight(1f),
                    label = "借用餐具",
                    icon = Icons.Default.Add,
                    onClick = onNavigateToBorrow,
                    containerColor = MaterialTheme.colorScheme.primary,
                )
                MenuButton(
                    modifier = Modifier.weight(1f),
                    label = "歸還餐具",
                    icon = Icons.Default.Refresh,
                    onClick = onNavigateToReturn,
                    containerColor = MaterialTheme.colorScheme.secondary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuButton(
                    modifier = Modifier.weight(1f),
                    label = "我的 QR Code",
                    icon = Icons.Default.QrCode,
                    onClick = onNavigateToMyQrCode,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                )
                MenuButton(
                    modifier = Modifier.weight(1f),
                    label = "交易記錄",
                    icon = Icons.Default.List,
                    onClick = onNavigateToTransactions,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { loadUserInfo() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重新整理")
            }

        } else {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { loadUserInfo() }) { Text("重試") }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun MenuButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 13.sp)
        }
    }
}
