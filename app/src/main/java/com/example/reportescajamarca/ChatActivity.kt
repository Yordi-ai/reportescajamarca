package com.example.reportescajamarca


import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: ImageButton
    private lateinit var btnAdjuntar: ImageButton

    private val mensajes = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var reporteId = ""
    private var reporteTitulo = ""

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        reporteId = intent.getStringExtra("REPORTE_ID") ?: ""
        reporteTitulo = intent.getStringExtra("REPORTE_TITULO") ?: ""

        supportActionBar?.title = "Chat - $reporteTitulo"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initFirebase()
        initViews()
        setupRecyclerView()
        loadMessages()
        setupListeners()
    }

    private fun initFirebase() {
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_chat)
        etMensaje = findViewById(R.id.et_mensaje)
        btnEnviar = findViewById(R.id.btn_enviar)
        btnAdjuntar = findViewById(R.id.btn_adjuntar)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(mensajes, auth.currentUser?.uid ?: "")
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        btnEnviar.setOnClickListener {
            enviarMensaje()
        }

        btnAdjuntar.setOnClickListener {
            seleccionarImagen()
        }
    }

    private fun loadMessages() {
        database.child("chats").child(reporteId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    mensajes.clear()

                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        message?.let { mensajes.add(it) }
                    }

                    mensajes.sortBy { it.timestamp }
                    adapter.notifyDataSetChanged()

                    if (mensajes.isNotEmpty()) {
                        recyclerView.smoothScrollToPosition(mensajes.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChatActivity,
                        "Error al cargar mensajes: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun enviarMensaje() {
        val texto = etMensaje.text.toString().trim()

        if (texto.isEmpty()) {
            Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val messageId = database.child("chats").child(reporteId).push().key ?: return

        // Detectar automáticamente si es trabajador o ciudadano
        val userType = if (currentUser.email?.contains("trabajador") == true ||
            currentUser.email?.contains("municipalidad") == true) {
            "municipalidad"
        } else {
            "ciudadano"
        }

        val message = Message(
            id = messageId,
            reporteId = reporteId,
            senderId = currentUser.uid,
            senderName = currentUser.email ?: "Usuario",
            senderType = userType,
            message = texto,
            timestamp = System.currentTimeMillis()
        )

        database.child("chats").child(reporteId).child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                etMensaje.text.clear()

                // ⭐ NUEVO: Enviar notificación al destinatario
                enviarNotificacionMensaje(reporteId, texto, userType)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error al enviar mensaje: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ⭐ NUEVA FUNCIÓN: Enviar notificación de nuevo mensaje
    private fun enviarNotificacionMensaje(reporteId: String, mensaje: String, tipoRemitente: String) {
        // Obtener el reporte para saber quién es el dueño
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("reportes").document(reporteId).get()
            .addOnSuccessListener { document ->
                val usuarioId = document.getString("usuarioId") ?: return@addOnSuccessListener
                val titulo = document.getString("titulo") ?: "Nuevo mensaje"

                // Si soy trabajador, notifico al ciudadano. Si soy ciudadano, notifico al trabajador
                val destinatarioId = if (tipoRemitente == "municipalidad") {
                    usuarioId  // Notificar al ciudadano dueño del reporte
                } else {
                    // Buscar trabajadores de esta categoría
                    obtenerTrabajadorParaNotificar(document.getString("tipoIncidente") ?: "")
                    return@addOnSuccessListener
                }

                // Obtener token FCM del destinatario
                db.collection("usuarios").document(destinatarioId).get()
                    .addOnSuccessListener { userDoc ->
                        val fcmToken = userDoc.getString("fcmToken") ?: return@addOnSuccessListener

                        // Crear notificación
                        val notificacion = hashMapOf(
                            "to" to fcmToken,
                            "notification" to hashMapOf(
                                "title" to "💬 $titulo",
                                "body" to mensaje.take(100)
                            ),
                            "data" to hashMapOf(
                                "tipo" to "nuevo_mensaje",
                                "reporteId" to reporteId,
                                "titulo" to titulo
                            )
                        )

                        // Guardar en Firestore para procesar
                        db.collection("notificaciones_pendientes").add(notificacion)
                    }
            }
    }

    private fun obtenerTrabajadorParaNotificar(categoria: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("usuarios")
            .whereEqualTo("tipoUsuario", "trabajador")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val trabajadorId = documents.documents[0].id
                    val fcmToken = documents.documents[0].getString("fcmToken") ?: return@addOnSuccessListener

                    // Crear notificación para trabajador
                    val notificacion = hashMapOf(
                        "to" to fcmToken,
                        "notification" to hashMapOf(
                            "title" to "💬 Nuevo mensaje en reporte",
                            "body" to "Un ciudadano te ha enviado un mensaje"
                        ),
                        "data" to hashMapOf(
                            "tipo" to "nuevo_mensaje",
                            "reporteId" to reporteId,
                            "titulo" to reporteTitulo
                        )
                    )

                    db.collection("notificaciones_pendientes").add(notificacion)
                }
            }
    }

    private fun seleccionarImagen() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri?.let {
                Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}