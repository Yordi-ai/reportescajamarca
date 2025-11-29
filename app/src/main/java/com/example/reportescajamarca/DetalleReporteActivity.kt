package com.example.reportescajamarca

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.reportescajamarca.Reporte
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DetalleReporteActivity : AppCompatActivity() {

    private lateinit var ivFotoDetalle: ImageView
    private lateinit var tvTipoIncidenteDetalle: TextView
    private lateinit var tvTituloDetalle: TextView
    private lateinit var tvEstadoDetalle: TextView
    private lateinit var tvFechaDetalle: TextView
    private lateinit var tvDireccionDetalle: TextView
    private lateinit var tvDescripcionDetalle: TextView
    private lateinit var tvPrioridadDetalle: TextView
    private lateinit var btnVolverDetalle: Button

    private lateinit var db: FirebaseFirestore
    private var reporteId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reporte)

        supportActionBar?.title = "Detalles del Reporte"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = FirebaseFirestore.getInstance()

        initViews()

        // Obtener ID del reporte
        reporteId = intent.getStringExtra("REPORTE_ID") ?: ""

        if (reporteId.isEmpty()) {
            Toast.makeText(this, "Error al cargar el reporte", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarDetalleReporte()
    }

    private fun initViews() {
        ivFotoDetalle = findViewById(R.id.ivFotoDetalle)
        tvTipoIncidenteDetalle = findViewById(R.id.tvTipoIncidenteDetalle)
        tvTituloDetalle = findViewById(R.id.tvTituloDetalle)
        tvEstadoDetalle = findViewById(R.id.tvEstadoDetalle)
        tvFechaDetalle = findViewById(R.id.tvFechaDetalle)
        tvDireccionDetalle = findViewById(R.id.tvDireccionDetalle)
        tvDescripcionDetalle = findViewById(R.id.tvDescripcionDetalle)
        tvPrioridadDetalle = findViewById(R.id.tvPrioridadDetalle)
        btnVolverDetalle = findViewById(R.id.btnVolverDetalle)

        btnVolverDetalle.setOnClickListener {
            finish()
        }
    }

    private fun cargarDetalleReporte() {
        db.collection("reportes")
            .document(reporteId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val reporte = document.toObject(Reporte::class.java)
                    if (reporte != null) {
                        mostrarDetalles(reporte)
                    }
                } else {
                    Toast.makeText(this, "Reporte no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun mostrarDetalles(reporte: Reporte) {
        // Cargar imagen
        if (reporte.fotoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(reporte.fotoUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(ivFotoDetalle)
        }

        // Información básica
        tvTipoIncidenteDetalle.text = reporte.tipoIncidente
        tvTituloDetalle.text = reporte.titulo
        tvDescripcionDetalle.text = reporte.descripcion
        tvDireccionDetalle.text = reporte.direccion
        tvPrioridadDetalle.text = "Prioridad: ${reporte.prioridad}"

        // Fecha formateada
        tvFechaDetalle.text = convertirFecha(reporte.fecha)

        // Estado con color
        tvEstadoDetalle.text = reporte.estado
        when (reporte.estado) {
            "Pendiente" -> tvEstadoDetalle.setTextColor(getColor(android.R.color.holo_orange_dark))
            "En Proceso" -> tvEstadoDetalle.setTextColor(getColor(android.R.color.holo_blue_dark))
            "Resuelto" -> tvEstadoDetalle.setTextColor(getColor(android.R.color.holo_green_dark))
            "Rechazado" -> tvEstadoDetalle.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun convertirFecha(timestamp: Long): String {
        if (timestamp == 0L) return "Sin fecha"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fecha = Date(timestamp)
        return sdf.format(fecha)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}