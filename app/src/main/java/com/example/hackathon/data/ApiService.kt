package com.example.hackathon.data

import com.example.hackathon.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendVerificationRequest): Response<SimpleResponse>

    @GET("api/users/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserInfo>

    @GET("api/users/qrcode")
    suspend fun getMyQrCode(@Header("Authorization") token: String): Response<QrCodeResponse>

    @GET("api/users/transactions")
    suspend fun getTransactions(@Header("Authorization") token: String): Response<List<Transaction>>

    @POST("api/borrow/borrow")
    suspend fun borrowUtensil(
        @Header("Authorization") token: String,
        @Body request: BorrowRequest,
    ): Response<BorrowResponse>

    @POST("api/borrow/return")
    suspend fun returnUtensil(
        @Header("Authorization") token: String,
        @Body request: ReturnRequest,
    ): Response<ReturnResponse>
}
