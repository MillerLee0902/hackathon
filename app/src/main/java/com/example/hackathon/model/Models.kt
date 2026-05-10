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
    val role: String = "user",   // "user" | "staff" | "admin"
)

data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val user: UserInfo?,
    val message: String?,
    val emailNotVerified: Boolean? = null,
)

data class ResendVerificationRequest(val email: String)

data class SimpleResponse(val success: Boolean, val message: String?)

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

// 一般用戶可見的餐具資訊（GET /api/utensils）
data class Utensil(
    val id: Int,
    val qrCode: String,
    val type: String,
    val status: String,         // "available" | "borrowed"
    val depositAmount: Double,
)

data class ApiError(val success: Boolean, val message: String)

// ── 店員專用 ──────────────────────────────────────────────

// POST /api/staff/borrow 的請求 & 回傳
data class StaffBorrowRequest(val utensilQrCode: String, val userQrCode: String)

data class StaffBorrowResponse(
    val success: Boolean,
    val message: String?,
    val borrowerName: String?,
    val utensilType: String?,
    val depositCharged: Double?,
    val newWalletBalance: Double?,
)

// POST /api/staff/return 的回傳
data class StaffReturnResponse(
    val success: Boolean,
    val message: String?,
    val borrowerName: String?,
    val utensilType: String?,
    val pointsEarned: Int?,
    val depositReturned: Double?,
    val newPoints: Int?,
    val newWalletBalance: Double?,
)

// GET /api/staff/transactions 的單筆記錄
data class StaffTransaction(
    val id: Int,
    val action: String,
    val pointsEarned: Int,
    val depositChange: Double,
    val note: String?,
    val createdAt: String,
    val username: String,
    val email: String,
    val utensilQr: String,
    val utensilType: String,
    val utensilStatus: String,
)

data class StaffTransactionsResponse(
    val success: Boolean,
    val transactions: List<StaffTransaction>?,
)

// GET /api/staff/utensils 的單筆餐具
data class StaffUtensil(
    val id: Int,
    val qrCode: String,
    val type: String,
    val status: String,
    val borrowedAt: String?,
    val borrowerName: String?,
    val borrowerEmail: String?,
)

data class StaffUtensilsResponse(
    val success: Boolean,
    val utensils: List<StaffUtensil>?,
)

// ── 抽獎號碼 ──────────────────────────────────────────────────

data class LotteryTicket(
    val id: Int,
    val ticketNumber: String,
    val utensilType: String,
    val utensilQr: String,
    val createdAt: String,
)

data class UserLotteryResponse(
    val success: Boolean,
    val total: Int,
    val tickets: List<LotteryTicket>?,
)

// ── 點數兌換 ──────────────────────────────────────────────────

data class RedeemQrRequest(val points: Int)

data class RedeemQrResponse(
    val success: Boolean,
    val qrData: String?,
    val points: Int?,
    val message: String?,
)

data class StaffRedeemRequest(val qrData: String)

data class StaffRedeemResponse(
    val success: Boolean,
    val message: String?,
    val username: String?,
    val pointsDeducted: Int?,
    val newPoints: Int?,
)
