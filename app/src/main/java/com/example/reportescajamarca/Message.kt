package com.example.reportescajamarca

data class Message(
    val id: String = "",
    val reporteId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderType: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String = ""
)