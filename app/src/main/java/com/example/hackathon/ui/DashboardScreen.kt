package com.example.hackathon.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathon.data.RetrofitClient
import com.example.hackathon.data.SessionManager
import com.example.hackathon.model.UserInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumMap

@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrData by remember { mutableStateOf("") }

    fun loadAll() {
        isLoading = true
        scope.launch {
            try {
                val token = session.getToken() ?: return@launch
                // 同時載入用戶資料與 QR Code
                val meResp = RetrofitClient.api.getMe(token)
                if (meResp.isSuccessful) userInfo = meResp.body()

                val qrResp = RetrofitClient.api.getMyQrCode(token)
                if (qrResp.isSuccessful && qrResp.body() != null) {
                    qrData = qrResp.body()!!.qrData
                    qrBitmap = withContext(Dispatchers.Default) { generateDashboardQrBitmap(qrData) }
                }

                if (!meResp.isSuccessful) errorMessage = "無法載入用戶資料"
            } catch (e: Exception) {
                errorMessage = "連線失敗"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        // ─── 標題列 ─────────────────────────────────────────────
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
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (userInfo != null) {
            val user = userInfo!!

            // ─── 用戶資訊卡片 ────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(user.username, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(user.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text("ID：${user.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(label = "點數", value = "${user.points} pt", icon = Icons.Default.Star)
                        StatItem(label = "錢包", value = "\$${String.format("%.0f", user.walletBalance)}", icon = Icons.Default.AccountBalanceWallet)
                        StatItem(label = "借用中", value = "${user.borrowedCount} 件", icon = Icons.Default.ShoppingBag)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── 個人 QR Code（給店員掃描）────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("我的 QR Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(
                        "借用／歸還餐具時出示給店員掃描",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )

                    if (qrBitmap != null) {
                        Card(
                            modifier = Modifier.size(220.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "個人 QR Code",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                qrData,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── 點數說明 ────────────────────────────────────────
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

            Spacer(modifier = Modifier.height(20.dp))

            Text("功能選單", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            // 交易記錄（全寬）
            MenuButton(
                modifier = Modifier.fillMaxWidth(),
                label = "交易記錄",
                icon = Icons.Default.List,
                onClick = onNavigateToTransactions,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { loadAll() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("重新整理")
            }

        } else {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { loadAll() }) { Text("重試") }
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
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun generateDashboardQrBitmap(content: String, size: Int = 512): Bitmap {
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
