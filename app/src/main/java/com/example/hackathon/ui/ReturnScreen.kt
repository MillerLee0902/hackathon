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

    var step by remember { mutableIntStateOf(1) } // 1=掃用戶, 2=掃餐具, 3=完成
    var userQrCode by remember { mutableStateOf("") }
    var utensilQrCode by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var returnInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val userScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            userQrCode = result.contents
            if (userQrCode.startsWith("USER-")) step = 2
        }
    }

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

        Spacer(modifier = Modifier.height(16.dp))

        // 步驟指示器
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepIndicator(number = 1, label = "掃描用戶", isActive = step >= 1, isDone = step > 1, modifier = Modifier.weight(1f))
            StepIndicator(number = 2, label = "掃描餐具", isActive = step >= 2, isDone = step > 2, modifier = Modifier.weight(1f))
            StepIndicator(number = 3, label = "完成歸還", isActive = step >= 3, isDone = false, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (step) {
            1 -> {
                Text("步驟 1：掃描用戶 QR Code", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("請掃描借用者手機上顯示的個人 QR Code", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("掃描借用者的個人 QR Code")
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

                if (userQrCode.isNotEmpty() && !userQrCode.startsWith("USER-")) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("QR Code 格式錯誤：$userQrCode", color = MaterialTheme.colorScheme.error)
                }
            }

            2 -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("已識別用戶", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text(userQrCode, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("步驟 2：掃描餐具 QR Code", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("請掃描要歸還的環保餐具上的 QR Code", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val options = ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("掃描餐具上的 QR Code")
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
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("餐具 QR Code", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(utensilQrCode, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isLoading = true
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
                                            "borrowerName" to (body.borrowerName ?: ""),
                                            "pointsEarned" to "${body.pointsEarned ?: 0} 點",
                                            "depositReturned" to "\$${body.depositReturned ?: 0}",
                                            "newPoints" to "${body.newPoints ?: 0} 點",
                                            "newWallet" to "\$${body.newWalletBalance ?: 0}",
                                        )
                                        isSuccess = true
                                        step = 3
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
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onTertiary)
                        else Text("確認歸還", fontSize = 16.sp)
                    }
                }
            }

            3 -> {
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
                                    "borrowerName" -> "借用者"
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
                            step = 1
                            userQrCode = ""
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
}

@Composable
private fun StepIndicator(number: Int, label: String, isActive: Boolean, isDone: Boolean, modifier: Modifier = Modifier) {
    val color = when {
        isDone -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(50),
            color = color,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDone) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                else Text("$number", color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
    }
}
