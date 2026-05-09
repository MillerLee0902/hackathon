package com.example.hackathon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hackathon.data.RetrofitClient
import com.example.hackathon.data.SessionManager
import com.example.hackathon.model.LoginRequest
import com.example.hackathon.model.ResendVerificationRequest
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LoginScreen(onLoginSuccess: (role: String) -> Unit, onNavigateToRegister: () -> Unit) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResendButton by remember { mutableStateOf(false) }
    var resendMessage by remember { mutableStateOf("") }
    var isResending by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "環保餐具借還",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("登入帳號", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                showResendButton = false
                resendMessage = ""
            },
            label = { Text("電子郵件") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密碼") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        // 若未驗證 Email，顯示「重新寄送驗證信」按鈕
        if (showResendButton) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    isResending = true
                    resendMessage = ""
                    scope.launch {
                        try {
                            val resp = RetrofitClient.api.resendVerification(
                                ResendVerificationRequest(email.trim())
                            )
                            resendMessage = resp.body()?.message ?: "驗證信已重新寄出"
                        } catch (e: Exception) {
                            resendMessage = "寄送失敗，請稍後再試"
                        } finally {
                            isResending = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isResending,
            ) {
                if (isResending) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("重新寄送驗證信", fontSize = 14.sp)
                }
            }
        }

        if (resendMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                resendMessage,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "請填寫帳號和密碼"
                    return@Button
                }
                isLoading = true
                errorMessage = ""
                showResendButton = false
                resendMessage = ""
                scope.launch {
                    try {
                        val response = RetrofitClient.api.login(LoginRequest(email.trim(), password))
                        if (response.isSuccessful && response.body()?.success == true) {
                            val body = response.body()!!
                            session.saveToken("Bearer ${body.token!!}")
                            val role = body.user?.role ?: "user"
                            session.saveRole(role)
                            onLoginSuccess(role)
                        } else {
                            // 解析 errorBody（4xx 時 body() 為 null）
                            val msg = response.body()?.message
                                ?: runCatching {
                                    val errJson = response.errorBody()?.string() ?: ""
                                    JSONObject(errJson).optString("message", "登入失敗，請重試")
                                }.getOrDefault("登入失敗，請重試")

                            errorMessage = msg

                            // 403 = 未驗證 Email
                            if (response.code() == 403) {
                                showResendButton = true
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = "無法連線到伺服器，請確認後端是否啟動"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("登入", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("還沒有帳號？點此註冊")
        }
    }
}
