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
import com.example.hackathon.model.ReturnRequest
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@Composable
fun StaffReturnScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    // 流程：Step 1 = 掃餐具，Step 2 = 掃用戶，Step 3 = 完成
    var step by remember { mutableIntStateOf(1) }
    var utensilQrCode by remember { mutableStateOf("") }
    var userQrCode by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var returnInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Step 1：掃餐具
    val utensilScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            utensilQrCode = result.contents
            step = 2
        }
    }

    // Step 2：掃用戶
    val userScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            userQrCode = result.contents
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
                Text("店員回收餐具", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("STAFF MODE", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 步驟指示器
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StaffStepIndicator(number = 1, label = "掃餐具", isActive = step >= 1, isDone = step > 1, modifier = Modifier.weight(1f))
            StaffStepIndicator(number = 2, label = "掃用戶", isActive = step >= 2, isDone = step > 2, modifier = Modifier.weight(1f))
            StaffStepIndicator(number = 3, label = "回收完成", isActive = step >= 3, isDone = false, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (step) {

            // ─── Step 1：掃餐具 QR ──────────────────────────────
            1 -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("先掃描要回收的餐具 QR Code", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("步驟 1：掃描餐具 QR Code", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("掃描餐具上的 QR Code（例如 UTENSIL-001、tool 1）", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("掃描餐具 QR Code")
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
            }

            // ─── Step 2：掃用戶 QR ──────────────────────────────
            2 -> {
                // 顯示已識別的餐具
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("已識別餐具", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(utensilQrCode, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("請用戶在 App 中開啟「我的 QR Code」頁面", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("步驟 2：掃描用戶 QR Code", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("掃描借用者手機上的個人 QR Code 以確認身分", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("掃描借用者的個人 QR Code（USER-xxx）")
                            setBeepEnabled(true)
                            setOrientationLocked(false)
                        }
                        userScanLauncher.launch(options)
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("掃描用戶 QR Code", fontSize = 16.sp)
                }

                if (userQrCode.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!userQrCode.startsWith("USER-")) {
                        Text(
                            "格式錯誤，請掃用戶個人 QR（需為 USER-xxx）：$userQrCode",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                        )
                    } else {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("用戶 QR Code", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Text(userQrCode, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val token = "Bearer ${session.getToken()}"
                                        val response = RetrofitClient.api.staffReturn(
                                            token, ReturnRequest(userQrCode, utensilQrCode)
                                        )
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            val body = response.body()!!
                                            resultMessage = body.message ?: "回收成功！"
                                            returnInfo = mapOf(
                                                "借用者" to (body.borrowerName ?: ""),
                                                "餐具種類" to (body.utensilType ?: ""),
                                                "退回押金" to "\$${body.depositReturned?.toInt() ?: 0}",
                                                "獲得點數" to "+${body.pointsEarned ?: 0} 點",
                                                "用戶目前點數" to "${body.newPoints ?: 0} 點",
                                                "用戶目前錢包" to "\$${body.newWalletBalance?.toInt() ?: 0}",
                                            )
                                            step = 3
                                        } else {
                                            resultMessage = response.body()?.message ?: "回收失敗"
                                        }
                                    } catch (e: Exception) {
                                        resultMessage = "連線失敗：${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Recycling, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("確認回收", fontSize = 16.sp)
                            }
                        }

                        if (resultMessage.isNotEmpty() && step != 3) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(resultMessage, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        }
                    }
                }
            }

            // ─── Step 3：回收完成 ────────────────────────────────
            3 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(80.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("回收成功！", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(resultMessage, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("回收明細", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            HorizontalDivider()
                            returnInfo.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(label, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                                    Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            step = 1
                            utensilQrCode = ""
                            userQrCode = ""
                            resultMessage = ""
                            returnInfo = emptyMap()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("繼續回收下一件")
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffStepIndicator(
    number: Int,
    label: String,
    isActive: Boolean,
    isDone: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isActive || isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(shape = RoundedCornerShape(50), color = color, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                if (isDone) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                else Text("$number", color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
    }
}
