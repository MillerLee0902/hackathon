package com.example.hackathon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathon.data.RetrofitClient
import com.example.hackathon.data.SessionManager
import com.example.hackathon.model.StaffTransaction
import com.example.hackathon.model.StaffUtensil
import kotlinx.coroutines.launch

@Composable
fun StaffDashboardScreen(
    onNavigateToBorrow: () -> Unit,
    onNavigateToReturn: () -> Unit,
    onNavigateToRedeem: () -> Unit = {},
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0=首頁, 1=記錄, 2=餐具狀況
    var transactions by remember { mutableStateOf<List<StaffTransaction>>(emptyList()) }
    var utensils by remember { mutableStateOf<List<StaffUtensil>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // 切換到記錄/餐具 tab 時自動載入
    LaunchedEffect(selectedTab) {
        val token = session.getToken() ?: return@LaunchedEffect
        isLoading = true
        errorMsg = ""
        try {
            when (selectedTab) {
                1 -> {
                    val resp = RetrofitClient.api.staffGetTransactions(token)
                    if (resp.isSuccessful) transactions = resp.body()?.transactions ?: emptyList()
                    else errorMsg = "無法載入記錄"
                }
                2 -> {
                    val resp = RetrofitClient.api.staffGetUtensils(token)
                    if (resp.isSuccessful) utensils = resp.body()?.utensils ?: emptyList()
                    else errorMsg = "無法載入餐具狀況"
                }
            }
        } catch (e: Exception) {
            errorMsg = "連線失敗：${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("店員後台", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            Text("STAFF DASHBOARD", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = {
                            session.clearAll()
                            onLogout()
                        }) {
                            Icon(Icons.Default.Logout, contentDescription = "登出", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("首頁") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    label = { Text("借還記錄") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    label = { Text("餐具狀況") },
                )
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> StaffHomeTab(onNavigateToBorrow = onNavigateToBorrow, onNavigateToReturn = onNavigateToReturn, onNavigateToRedeem = onNavigateToRedeem)
                1 -> StaffTransactionsTab(transactions = transactions, isLoading = isLoading, errorMsg = errorMsg)
                2 -> StaffUtensilsTab(utensils = utensils, isLoading = isLoading, errorMsg = errorMsg)
            }
        }
    }
}

// ─── Tab 0：首頁 ────────────────────────────────────────────

@Composable
private fun StaffHomeTab(onNavigateToBorrow: () -> Unit, onNavigateToReturn: () -> Unit, onNavigateToRedeem: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("環保餐具借還系統", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("先掃餐具 QR → 再掃用戶 QR 完成借出／回收", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Text("快速操作", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)

        StaffActionButton(
            icon = Icons.Default.ArrowForward,
            title = "借出餐具",
            subtitle = "掃餐具 QR → 掃用戶 QR 完成借出",
            onClick = onNavigateToBorrow,
            color = Color(0xFF1565C0),
        )

        StaffActionButton(
            icon = Icons.Default.Recycling,
            title = "回收餐具",
            subtitle = "掃餐具 QR → 掃用戶 QR 完成回收",
            onClick = onNavigateToReturn,
            color = MaterialTheme.colorScheme.primary,
        )

        StaffActionButton(
            icon = Icons.Default.Redeem,
            title = "點數兌換",
            subtitle = "掃描用戶兌換 QR Code 完成扣點",
            onClick = onNavigateToRedeem,
            color = Color(0xFF6A1B9A),
        )

        Text("操作說明", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("借出流程", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1565C0))
                StaffGuideRow(step = "1", text = "點選「借出餐具」")
                StaffGuideRow(step = "2", text = "掃描餐具上的 QR Code")
                StaffGuideRow(step = "3", text = "請用戶開啟 App → 我的 QR Code")
                StaffGuideRow(step = "4", text = "掃描用戶手機上的個人 QR Code")
                StaffGuideRow(step = "5", text = "確認借出，系統自動扣押金 \$20")
                Spacer(modifier = Modifier.height(4.dp))
                Text("回收流程", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                StaffGuideRow(step = "1", text = "點選「回收餐具」")
                StaffGuideRow(step = "2", text = "掃描餐具上的 QR Code")
                StaffGuideRow(step = "3", text = "請用戶開啟 App → 我的 QR Code")
                StaffGuideRow(step = "4", text = "掃描用戶手機上的個人 QR Code")
                StaffGuideRow(step = "5", text = "確認回收，系統自動退押金 \$20 並加 1 點")
            }
        }
    }
}

@Composable
private fun StaffActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    color: Color,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimary)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun StaffGuideRow(step: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(step, color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 14.sp)
    }
}

// ─── Tab 1：借還記錄 ─────────────────────────────────────────

@Composable
private fun StaffTransactionsTab(
    transactions: List<StaffTransaction>,
    isLoading: Boolean,
    errorMsg: String,
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (errorMsg.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
        }
        return
    }
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("目前沒有任何記錄", color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("借還記錄（最新 200 筆）", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }
        items(transactions) { tx ->
            StaffTransactionCard(tx)
        }
    }
}

@Composable
private fun StaffTransactionCard(tx: StaffTransaction) {
    val isBorrow = tx.action == "borrow"
    val actionColor = if (isBorrow) Color(0xFFE65100) else Color(0xFF2E7D32)
    val actionLabel = if (isBorrow) "借出" else "歸還"
    val actionIcon = if (isBorrow) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = RoundedCornerShape(8.dp), color = actionColor.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(actionIcon, contentDescription = null, tint = actionColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(actionLabel, fontWeight = FontWeight.Bold, color = actionColor, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tx.utensilType, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Text("用戶：${tx.username}", fontSize = 13.sp)
                Text(tx.utensilQr, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                if (!tx.note.isNullOrEmpty()) {
                    Text(tx.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val depositText = if (tx.depositChange > 0) "+\$${tx.depositChange.toInt()}" else "\$${tx.depositChange.toInt()}"
                Text(depositText, fontWeight = FontWeight.SemiBold, color = if (tx.depositChange > 0) Color(0xFF2E7D32) else Color(0xFFE65100), fontSize = 14.sp)
                if (tx.pointsEarned > 0) Text("+${tx.pointsEarned}pt", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text(tx.createdAt.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// ─── Tab 2：餐具狀況 ─────────────────────────────────────────

@Composable
private fun StaffUtensilsTab(
    utensils: List<StaffUtensil>,
    isLoading: Boolean,
    errorMsg: String,
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (errorMsg.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val available = utensils.count { it.status == "available" }
    val borrowed = utensils.count { it.status == "borrowed" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("餐具現況", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(label = "可借出", value = "$available", color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                StatChip(label = "借出中", value = "$borrowed", color = Color(0xFFE65100), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(utensils) { utensil ->
            StaffUtensilCard(utensil)
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 13.sp, color = color)
        }
    }
}

@Composable
private fun StaffUtensilCard(utensil: StaffUtensil) {
    val isAvailable = utensil.status == "available"
    val statusColor = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFE65100)
    val statusLabel = if (isAvailable) "可借出" else "借出中"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(utensil.qrCode, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(utensil.type, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                if (!isAvailable && utensil.borrowerName != null) {
                    Text("借用者：${utensil.borrowerName}", fontSize = 12.sp, color = Color(0xFFE65100))
                }
                if (!isAvailable && utensil.borrowedAt != null) {
                    Text("借出時間：${utensil.borrowedAt.take(16).replace("T", " ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = statusColor.copy(alpha = 0.15f),
            ) {
                Text(
                    statusLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
