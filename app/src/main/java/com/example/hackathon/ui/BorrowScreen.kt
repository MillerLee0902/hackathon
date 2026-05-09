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
import com.example.hackathon.model.BorrowRequest
import com.example.hackathon.model.Utensil
import kotlinx.coroutines.launch

@Composable
fun BorrowScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var utensils by remember { mutableStateOf<List<Utensil>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(true) }
    var selectedUtensil by remember { mutableStateOf<Utensil?>(null) }
    var resultMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var newWalletBalance by remember { mutableStateOf<Double?>(null) }

    // 載入可借餐具清單
    LaunchedEffect(Unit) {
        try {
            val token = session.getToken() ?: return@LaunchedEffect
            val response = RetrofitClient.api.getUtensils(token)
            if (response.isSuccessful) {
                utensils = response.body()?.filter { it.status == "available" } ?: emptyList()
            }
        } catch (_: Exception) {
        } finally {
            isLoadingList = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text("借用環保餐具", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 說明卡
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("選擇想借用的餐具，押金 \$20 將從錢包扣除", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 結果訊息
            if (resultMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(resultMessage, fontWeight = FontWeight.SemiBold)
                        }
                        if (isSuccess && newWalletBalance != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("目前錢包餘額：\$${String.format("%.0f", newWalletBalance)}", fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("可借用餐具", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingList) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (utensils.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text("目前沒有可借用的餐具，請稍後再試", modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(utensils) { utensil ->
                        UtensilCard(
                            utensil = utensil,
                            isSelected = selectedUtensil?.id == utensil.id,
                            onSelect = {
                                selectedUtensil = if (selectedUtensil?.id == utensil.id) null else utensil
                                resultMessage = ""
                            },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // 確認按鈕（浮動在底部）
        if (selectedUtensil != null) {
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            ) {
                Button(
                    onClick = {
                        val utensil = selectedUtensil ?: return@Button
                        isSubmitting = true
                        resultMessage = ""
                        scope.launch {
                            try {
                                val token = session.getToken() ?: run {
                                    resultMessage = "請先登入"
                                    isSubmitting = false
                                    return@launch
                                }
                                val response = RetrofitClient.api.borrowUtensil(token, BorrowRequest(utensil.qrCode))
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val body = response.body()!!
                                    resultMessage = body.message ?: "借用成功！"
                                    newWalletBalance = body.walletBalance
                                    isSuccess = true
                                    selectedUtensil = null
                                    // 重新整理清單
                                    val resp2 = RetrofitClient.api.getUtensils(token)
                                    if (resp2.isSuccessful) {
                                        utensils = resp2.body()?.filter { it.status == "available" } ?: emptyList()
                                    }
                                } else {
                                    resultMessage = response.body()?.message ?: "借用失敗"
                                    isSuccess = false
                                }
                            } catch (e: Exception) {
                                resultMessage = "連線失敗，請確認伺服器狀態"
                                isSuccess = false
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("確認借用「${selectedUtensil?.type}」", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UtensilCard(
    utensil: Utensil,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.SetMeal,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(utensil.type, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(utensil.qrCode, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                Text("押金 \$${String.format("%.0f", utensil.depositAmount)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}
