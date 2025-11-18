package com.example.reportescajamarca


data class BadgeCounter(
    val reporteId: String = "",
    val mensajesNoLeidos: Int = 0,
    val ultimoMensajeTimestamp: Long = 0L
)

data class CategoriaBadge(
    val categoria: String = "",
    val reportesPendientes: Int = 0,
    val mensajesNoLeidos: Int = 0
)