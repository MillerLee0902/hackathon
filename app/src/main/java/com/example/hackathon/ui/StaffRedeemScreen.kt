package com.example.hackathon.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathon.data.RetrofitClient
import com.example.hackathon.data.SessionManager
import com.example.hackathon.model.StaffRedeemRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun StaffRedeemScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var scannedQr by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var redeemInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isDone by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            scannedQr = result.contents
            if (!scannedQr.startsWith("REDEEM-")) {
                resultMessage = "此 QR Code 不是兌換碼，請掃描用戶的兌換 QR"
                isSuccess = false
            } else {
                // 自動呼叫兌換
                isLoading = true
                resultMessage = ""
                scope.launch {
                    try {
                        val token = session.getToken() ?: run {
                            resultMessage = "請重新登入"
                            isLoading = false
                            return@launch
                        }
                        val resp = RetrofitClient.api.staffRedeem(token, StaffRedeemRequest(scannedQr))
                        if (resp.isSuccessful && resp.body()?.success == true) {
                            val body = resp.body()!!
                            resultMessage = body.message ?: "兌換成功"
                            isSuccess = true
                            redeemInfo = mapOf(
                                "用戶名稱" to (body.username ?: ""),
                                "扣除點數" to "-${body.pointsDeducted ?: 0} 點",
                                "剩餘點數" to "${body.newPoints ?: 0} 點",
                            )
                            isDone = true
                        } else {
                            resultMessage = resp.body()?.message
                                ?: runCatching {
                                    JSONObject(resp.errorBody()?.string() ?: "").optString("message", "兌換失敗")
                                }.getOrDefault("兌換失敗")
                            isSuccess = false
                        }
                    } catch (e: Exception) {
                        resultMessage = "連線失敗：${e.message}"
                        isSuccess = false
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        // 標題列
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Column {
                Text("點數兌換", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("STAFF MODE", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isDone) {
            // ─── 掃描前 ───────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("操作說明", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("請用戶在 App 中開啟「兌換點數」頁面，輸入欲兌換點數後出示 QR Code", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scannedQr = ""
                    resultMessage = ""
                    isSuccess = false
                    val options = ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("掃描用戶的兌換 QR Code")
                        setBeepEnabled(true)
                        setOrientationLocked(false)
                    }
                    scanLauncher.launch(options)
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("處理中...", fontSize = 16.sp)
                } else {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("掃描兌換 QR Code", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // 錯誤訊息
            if (resultMessage.isNotEmpty() && !isSuccess) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(resultMessage, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }
                }
            }

            if (scannedQr.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("掃描內容：$scannedQr", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }

        } else {
            // ─── 兌換成功 ─────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(88.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("兌換成功！", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.height(6.dp))
                Text(resultMessage, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("兌換明細", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        HorizontalDivider()
                        redeemInfo.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                                Text(
                                    value,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (label == "扣除點數") Color(0xFFE65100) else MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isDone = false
                        scannedQr = ""
                        resultMessage = ""
                        redeemInfo = emptyMap()
                        isSuccess = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("繼續處理下一位")
                }
            }
        }
    }
}
