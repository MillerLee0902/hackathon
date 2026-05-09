package com.example.hackathon.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathon.data.RetrofitClient
import com.example.hackathon.data.SessionManager
import com.example.hackathon.model.Transaction

@Composable
fun TransactionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val token = session.getToken() ?: run {
                errorMessage = "請先登入"
                isLoading = false
                return@LaunchedEffect
            }
            val response = RetrofitClient.api.getTransactions(token)
            if (response.isSuccessful) {
                transactions = response.body() ?: emptyList()
            } else {
                errorMessage = "無法載入交易記錄"
            }
        } catch (e: Exception) {
            errorMessage = "連線失敗"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text("交易記錄", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            errorMessage.isNotEmpty() -> Text(errorMessage, color = MaterialTheme.colorScheme.error)
            transactions.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("目前沒有交易記錄", color = MaterialTheme.colorScheme.secondary)
                    Text("借用或歸還環保餐具後會出現在這裡", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { tx -> TransactionItem(tx) }
            }
        }
    }
}

@Composable
private fun TransactionItem(tx: Transaction) {
    val isBorrow = tx.action == "borrow"
    val icon = if (isBorrow) Icons.Default.Add else Icons.Default.Refresh
    val containerColor = if (isBorrow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isBorrow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isBorrow) "借用 ${tx.utensilType ?: ""}" else "歸還 ${tx.utensilType ?: ""}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                if (!tx.createdAt.isNullOrBlank()) {
                    Text(tx.createdAt.take(16).replace("T", " "), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (tx.depositChange != 0.0) {
                    Text(
                        "${if (tx.depositChange > 0) "+" else ""}\$${String.format("%.0f", tx.depositChange)}",
                        fontWeight = FontWeight.Bold,
                        color = if (tx.depositChange > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                if (tx.pointsEarned > 0) {
                    Text("+${tx.pointsEarned} pt", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
