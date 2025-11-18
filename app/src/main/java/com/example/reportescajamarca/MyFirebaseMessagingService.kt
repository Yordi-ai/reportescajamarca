package com.example.reportescajamarca

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Si hay datos en la notificación
        remoteMessage.data.isNotEmpty().let {
            val tipo = remoteMessage.data["tipo"]
            val reporteId = remoteMessage.data["reporteId"]
            val titulo = remoteMessage.data["titulo"]
            val mensaje = remoteMessage.data["mensaje"]

            when (tipo) {
                "nuevo_reporte" -> mostrarNotificacionNuevoReporte(titulo, mensaje, reporteId)
                "nuevo_mensaje" -> mostrarNotificacionNuevoMensaje(titulo, mensaje, reporteId)
                "cambio_estado" -> mostrarNotificacionCambioEstado(titulo, mensaje, reporteId)
            }
        }

        // Si hay notificación visible
        remoteMessage.notification?.let {
            mostrarNotificacionSimple(it.title ?: "", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardar el token en Firestore para enviar notificaciones
        guardarTokenEnFirestore(token)
    }

    private fun guardarTokenEnFirestore(token: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(userId)
            .update("fcmToken", token)
    }

    private fun mostrarNotificacionNuevoReporte(titulo: String?, mensaje: String?, reporteId: String?) {
        val intent = Intent(this, TrabajadorMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        mostrarNotificacion(
            "Nuevo Reporte",
            mensaje ?: "Hay un nuevo reporte en tu categoría",
            intent,
            CHANNEL_REPORTES
        )
    }

    private fun mostrarNotificacionNuevoMensaje(titulo: String?, mensaje: String?, reporteId: String?) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("REPORTE_ID", reporteId)
            putExtra("REPORTE_TITULO", titulo)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        mostrarNotificacion(
            "Nuevo Mensaje",
            mensaje ?: "Tienes un nuevo mensaje",
            intent,
            CHANNEL_MENSAJES
        )
    }

    private fun mostrarNotificacionCambioEstado(titulo: String?, mensaje: String?, reporteId: String?) {
        val intent = Intent(this, MisReportesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        mostrarNotificacion(
            "Estado actualizado",
            mensaje ?: "El estado de tu reporte ha cambiado",
            intent,
            CHANNEL_ESTADOS
        )
    }

    private fun mostrarNotificacionSimple(titulo: String, mensaje: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        mostrarNotificacion(titulo, mensaje, intent, CHANNEL_GENERAL)
    }

    private fun mostrarNotificacion(titulo: String, mensaje: String, intent: Intent, channelId: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getChannelName(channelId),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getChannelName(channelId: String): String {
        return when (channelId) {
            CHANNEL_REPORTES -> "Nuevos Reportes"
            CHANNEL_MENSAJES -> "Mensajes de Chat"
            CHANNEL_ESTADOS -> "Cambios de Estado"
            else -> "General"
        }
    }

    companion object {
        const val CHANNEL_REPORTES = "reportes_channel"
        const val CHANNEL_MENSAJES = "mensajes_channel"
        const val CHANNEL_ESTADOS = "estados_channel"
        const val CHANNEL_GENERAL = "general_channel"
    }
}