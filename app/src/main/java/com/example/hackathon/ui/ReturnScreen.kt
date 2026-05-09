package com.example.hackathon.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
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
import com.example.hackathon.model.ReturnRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@Composable
fun ReturnScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    // 從 session 自動帶入用戶身分，不需要掃 QR
    val userId = remember { session.getUserId() }
    val userQrCode = "USER-$userId"

    var utensilQrCode by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }
    var returnInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val utensilScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            utensilQrCode = result.contents
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
            Text("歸還環保餐具", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isDone) {
            // 用戶身分卡（自動帶入，不需掃描）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("已登入身分", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text(userQrCode, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("掃描餐具 QR Code", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("請掃描要歸還的環保餐具上的 QR Code", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val options = ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("請掃描餐具上的 QR Code")
                        setBeepEnabled(true)
                        setOrientationLocked(false)
                    }
                    utensilScanLauncher.launch(options)
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("掃描餐具 QR Code", fontSize = 16.sp)
            }

            if (utensilQrCode.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("掃描到的餐具", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(utensilQrCode, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (resultMessage.isNotEmpty() && !isSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(resultMessage, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        isLoading = true
                        resultMessage = ""
                        scope.launch {
                            try {
                                val token = session.getToken() ?: run {
                                    resultMessage = "請先登入"
                                    isLoading = false
                                    return@launch
                                }
                                val response = RetrofitClient.api.returnUtensil(
                                    token, ReturnRequest(userQrCode, utensilQrCode)
                                )
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val body = response.body()!!
                                    resultMessage = body.message ?: "歸還成功！"
                                    returnInfo = mapOf(
                                        "pointsEarned" to "${body.pointsEarned ?: 0} 點",
                                        "depositReturned" to "\$${body.depositReturned ?: 0}",
                                        "newPoints" to "${body.newPoints ?: 0} 點",
                                        "newWallet" to "\$${body.newWalletBalance ?: 0}",
                                    )
                                    isSuccess = true
                                    isDone = true
                                } else {
                                    resultMessage = response.body()?.message ?: "歸還失敗"
                                    isSuccess = false
                                }
                            } catch (e: Exception) {
                                resultMessage = "連線失敗，請確認伺服器狀態"
                                isSuccess = false
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onTertiary)
                    else Text("確認歸還", fontSize = 16.sp)
                }
            }
        } else {
            // 成功畫面
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("歸還成功！", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(resultMessage, fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary)

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("歸還明細", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        HorizontalDivider()
                        returnInfo.forEach { (key, value) ->
                            val label = when (key) {
                                "pointsEarned" -> "獲得點數"
                                "depositReturned" -> "退回押金"
                                "newPoints" -> "目前點數"
                                "newWallet" -> "目前錢包"
                                else -> key
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                                Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isDone = false
                        utensilQrCode = ""
                        resultMessage = ""
                        returnInfo = emptyMap()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("繼續歸還其他餐具")
                }
            }
        }
    }
}
