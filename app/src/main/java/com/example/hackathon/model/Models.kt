package com.example.hackathon.model

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(val username: String, val email: String, val password: String)

data class UserInfo(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int,
    val walletBalance: Double,
    val borrowedCount: Int = 0,
)

data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val user: UserInfo?,
    val message: String?,
)

data class BorrowRequest(val utensilQrCode: String)

data class ReturnRequest(val userQrCode: String, val utensilQrCode: String)

data class BorrowResponse(
    val success: Boolean,
    val message: String?,
    val utensilType: String?,
    val walletBalance: Double?,
)

data class ReturnResponse(
    val success: Boolean,
    val message: String?,
    val borrowerName: String?,
    val pointsEarned: Int?,
    val depositReturned: Double?,
    val newPoints: Int?,
    val newWalletBalance: Double?,
)

data class Transaction(
    val id: Int,
    val action: String,
    val utensilType: String?,
    val pointsEarned: Int,
    val depositChange: Double,
    val note: String?,
    val createdAt: String,
)

data class QrCodeResponse(val qrData: String)

data class ApiError(val success: Boolean, val message: String)
