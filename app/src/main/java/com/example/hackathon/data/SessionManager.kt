package com.example.hackathon.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("eco_session", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit().putString("token", token).apply()
    fun getToken(): String? = prefs.getString("token", null)
    fun clearToken() = prefs.edit().remove("token").apply()
    fun isLoggedIn() = getToken() != null

    fun saveRole(role: String) = prefs.edit().putString("role", role).apply()
    fun getRole(): String = prefs.getString("role", "user") ?: "user"
    fun isStaff(): Boolean = getRole() == "staff" || getRole() == "admin"

    fun saveUserId(id: Int) = prefs.edit().putInt("userId", id).apply()
    fun getUserId(): Int = prefs.getInt("userId", 0)

    fun clearAll() = prefs.edit().clear().apply()
}
