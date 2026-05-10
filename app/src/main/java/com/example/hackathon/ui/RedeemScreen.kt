package com.example.hackathon.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathon.data.RetrofitClient
import com.example.hackathon.data.SessionManager
import com.example.hackathon.model.RedeemQrRequest
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumMap

@Composable
fun RedeemScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var pointsInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrData by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf("") }
    var generatedPoints by remember { mutableIntStateOf(0) }

    fun generateQr() {
        val pts = pointsInput.toIntOrNull()
        if (pts == null || pts <= 0) {
            errorMsg = "請輸入有效的兌換點數（正整數）"
            return
        }
        isLoading = true
        errorMsg = ""
        qrBitmap = null
        qrData = ""
        scope.launch {
            try {
                val token = session.getToken() ?: run {
                    errorMsg = "請重新登入"
                    isLoading = false
                    return@launch
                }
                val resp = RetrofitClient.api.createRedeemQr(token, RedeemQrRequest(pts))
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val body = resp.body()!!
                    qrData = body.qrData ?: ""
                    generatedPoints = body.points ?: pts
                    successMsg = body.message ?: "請出示給店員掃描"
                    qrBitmap = withContext(Dispatchers.Default) { generateRedeemQrBitmap(qrData) }
                } else {
                    errorMsg = resp.body()?.message ?: "產生 QR Code 失敗"
                }
            } catch (e: Exception) {
                errorMsg = "連線失敗：${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
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
                    Text("兌換點數", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Text("生成 QR Code 給店員掃描即可兌換", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // 說明卡
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("兌換流程", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("輸入點數 → 生成 QR → 出示給店員掃描 → 完成扣點", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        Text("QR Code 有效期限：10 分鐘", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // 輸入點數
            OutlinedTextField(
                value = pointsInput,
                onValueChange = {
                    pointsInput = it.filter { c -> c.isDigit() }
                    qrBitmap = null
                    qrData = ""
                    errorMsg = ""
                },
                label = { Text("欲兌換點數") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Button(
                onClick = { generateQr() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !isLoading && pointsInput.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成兌換 QR Code", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // QR Code 顯示區
            if (qrBitmap != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("兌換 QR Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "請出示給店員掃描（10 分鐘內有效）",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.size(240.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "兌換 QR Code",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "兌換 $generatedPoints 點",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(successMsg, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                OutlinedButton(
                    onClick = {
                        qrBitmap = null
                        qrData = ""
                        pointsInput = ""
                        errorMsg = ""
                        successMsg = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重新生成")
                }
            }
        }
    }
}

private fun generateRedeemQrBitmap(content: String, size: Int = 512): Bitmap {
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
        put(EncodeHintType.CHARACTER_SET, "UTF-8")
        put(EncodeHintType.MARGIN, 1)
    }
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
