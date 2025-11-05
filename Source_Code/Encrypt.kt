package com.example.pro_impact_hub

import java.security.MessageDigest

object Encrypt {
    fun encryptPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashInBytes = md.digest(password.toByteArray(Charsets.UTF_8))
        val result = StringBuilder()
        for (byte in hashInBytes) {
            result.append(String.format("%02x", byte))
        }
        return result.toString()
    }
}
