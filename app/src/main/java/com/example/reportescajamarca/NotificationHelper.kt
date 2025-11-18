package com.example.reportescajamarca

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NotificationHelper {

    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    private const val SERVER_KEY = "TU_SERVER_KEY_AQUI" // Lo obtendremos en el siguiente paso

    fun enviarNotificacion(
        token: String,
        titulo: String,
        mensaje: String,
        data: Map<String, String> = emptyMap()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(FCM_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "key=$SERVER_KEY")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("to", token)
                    put("priority", "high")
                    put("notification", JSONObject().apply {
                        put("title", titulo)
                        put("body", mensaje)
                        put("sound", "default")
                    })
                    if (data.isNotEmpty()) {
                        put("data", JSONObject(data))
                    }
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(json.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                Log.d("NotificationHelper", "Response Code: $responseCode")

            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error enviando notificación: ${e.message}")
            }
        }
    }
}