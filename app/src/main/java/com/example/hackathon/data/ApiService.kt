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

    @GET("api/utensils")
    suspend fun getUtensils(@Header("Authorization") token: String): Response<List<Utensil>>

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

    // ── 店員專用 API ──────────────────────────────────────

    @POST("api/staff/borrow")
    suspend fun staffBorrowUtensil(
        @Header("Authorization") token: String,
        @Body request: StaffBorrowRequest,
    ): Response<StaffBorrowResponse>

    @POST("api/staff/return")
    suspend fun staffReturn(
        @Header("Authorization") token: String,
        @Body request: ReturnRequest,
    ): Response<StaffReturnResponse>

    @GET("api/staff/transactions")
    suspend fun staffGetTransactions(
        @Header("Authorization") token: String,
    ): Response<StaffTransactionsResponse>

    @GET("api/staff/utensils")
    suspend fun staffGetUtensils(
        @Header("Authorization") token: String,
    ): Response<StaffUtensilsResponse>

    // ── 抽獎號碼 ──────────────────────────────────────────────
    @GET("api/users/lottery-numbers")
    suspend fun getMyLotteryNumbers(
        @Header("Authorization") token: String,
    ): Response<UserLotteryResponse>

    // ── 點數兌換 ──────────────────────────────────────────────
    @POST("api/users/redeem-qr")
    suspend fun createRedeemQr(
        @Header("Authorization") token: String,
        @Body request: RedeemQrRequest,
    ): Response<RedeemQrResponse>

    @POST("api/staff/redeem")
    suspend fun staffRedeem(
        @Header("Authorization") token: String,
        @Body request: StaffRedeemRequest,
    ): Response<StaffRedeemResponse>
}
