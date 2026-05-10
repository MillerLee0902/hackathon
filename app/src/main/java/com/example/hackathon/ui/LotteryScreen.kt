package com.example.hackathon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathon.data.RetrofitClient
import com.example.hackathon.data.SessionManager
import com.example.hackathon.model.LotteryTicket
import kotlinx.coroutines.launch

@Composable
fun LotteryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var tickets by remember { mutableStateOf<List<LotteryTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }

    fun load() {
        isLoading = true
        errorMsg = ""
        scope.launch {
            try {
                val token = session.getToken() ?: return@launch
                val resp = RetrofitClient.api.getMyLotteryNumbers(token)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    tickets = resp.body()!!.tickets ?: emptyList()
                } else {
                    errorMsg = "無法載入抽獎號碼"
                }
            } catch (e: Exception) {
                errorMsg = "連線失敗：${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize()) {
        // ─── 標題列 ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text("我的抽獎號碼", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Text("每次歸還餐具可獲得一張抽獎券", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                }
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMsg.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { load() }) { Text("重試") }
                    }
                }
            }
            tickets.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                        Text("尚未持有抽獎號碼", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("歸還環保餐具即可獲得抽獎券！", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        // 統計卡
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("共持有 ${tickets.size} 張抽獎券", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("持有越多，中獎機率越高！", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("抽獎號碼列表", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    items(tickets) { ticket ->
                        LotteryTicketCard(ticket)
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { load() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重新整理")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LotteryTicketCard(ticket: LotteryTicket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 號碼區塊
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(width = 80.dp, height = 48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = ticket.ticketNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("No. ${ticket.ticketNumber}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("餐具：${ticket.utensilType}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                Text(
                    ticket.createdAt.take(10),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                )
            }
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(24.dp))
        }
    }
}
