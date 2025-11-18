package com.example.reportescajamarca


data class Reporte(
    var id: String = "",
    val tipoIncidente: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val estado: String = "Pendiente",
    val fecha: Long = 0L,
    val prioridad: String = "Media",
    val direccion: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val usuarioId: String = "",
    val numFotos: Int = 0,
    val fotoUrl: String = "",
    val vistoPor: List<String> = emptyList()  // ⭐ NUEVO: IDs de trabajadores que ya vieron
)