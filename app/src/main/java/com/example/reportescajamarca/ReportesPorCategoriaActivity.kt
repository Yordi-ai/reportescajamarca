package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reportescajamarca.databinding.ActivityReportesPorCategoriaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ReportesPorCategoriaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportesPorCategoriaBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ReportesTrabajadorAdapter
    private val reportesList = mutableListOf<Reporte>()
    private var categoria: String = ""
    private var filtroEstado: String = "Todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportesPorCategoriaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        categoria = intent.getStringExtra("CATEGORIA") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = categoria

        configurarRecyclerView()
        configurarFiltros()
        cargarReportes()
    }

    private fun configurarRecyclerView() {
        adapter = ReportesTrabajadorAdapter(
            reportesList,
            onEditarClick = { reporte ->
                mostrarDialogoEditar(reporte)
            },
            onChatClick = { reporte ->
                abrirChat(reporte)
            },
            onReporteVisto = { reporte ->
                // ⭐ NUEVO: Marcar reporte como visto
                marcarReporteComoVisto(reporte)
            }
        )
        binding.recyclerViewReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReportes.adapter = adapter
    }

    private fun abrirChat(reporte: Reporte) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("REPORTE_ID", reporte.id)
        intent.putExtra("REPORTE_TITULO", reporte.titulo)
        startActivity(intent)
    }

    private fun configurarFiltros() {
        binding.btnTodos.setOnClickListener {
            filtroEstado = "Todos"
            actualizarBotonesFiltro()
            cargarReportes()
        }

        binding.btnPendientes.setOnClickListener {
            filtroEstado = "Pendiente"
            actualizarBotonesFiltro()
            cargarReportes()
        }

        binding.btnEnProceso.setOnClickListener {
            filtroEstado = "En Proceso"
            actualizarBotonesFiltro()
            cargarReportes()
        }

        binding.btnResueltos.setOnClickListener {
            filtroEstado = "Resuelto"
            actualizarBotonesFiltro()
            cargarReportes()
        }
    }

    private fun actualizarBotonesFiltro() {
        binding.btnTodos.alpha = 0.5f
        binding.btnPendientes.alpha = 0.5f
        binding.btnEnProceso.alpha = 0.5f
        binding.btnResueltos.alpha = 0.5f

        when (filtroEstado) {
            "Todos" -> binding.btnTodos.alpha = 1.0f
            "Pendiente" -> binding.btnPendientes.alpha = 1.0f
            "En Proceso" -> binding.btnEnProceso.alpha = 1.0f
            "Resuelto" -> binding.btnResueltos.alpha = 1.0f
        }
    }

    private fun cargarReportes() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoReportes.visibility = View.GONE

        android.util.Log.d("ReportesCategoria", "=== INICIANDO BÚSQUEDA ===")
        android.util.Log.d("ReportesCategoria", "Categoría: $categoria")
        android.util.Log.d("ReportesCategoria", "Filtro Estado: $filtroEstado")

        var query: Query = db.collection("reportes")
            .whereEqualTo("tipoIncidente", categoria)
            .orderBy("fecha", Query.Direction.DESCENDING)

        if (filtroEstado != "Todos") {
            query = query.whereEqualTo("estado", filtroEstado)
        }

        query.get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                reportesList.clear()

                android.util.Log.d("ReportesCategoria", "Documentos encontrados: ${documents.size()}")

                if (documents.isEmpty) {
                    binding.tvNoReportes.visibility = View.VISIBLE
                    android.util.Log.d("ReportesCategoria", "No hay reportes en esta categoría")
                } else {
                    for (document in documents) {
                        android.util.Log.d("ReportesCategoria", "Reporte ID: ${document.id}")
                        android.util.Log.d("ReportesCategoria", "  - Título: ${document.getString("titulo")}")
                        android.util.Log.d("ReportesCategoria", "  - Tipo: ${document.getString("tipoIncidente")}")
                        android.util.Log.d("ReportesCategoria", "  - Estado: ${document.getString("estado")}")

                        // ⭐ NUEVO: Obtener lista de vistoPor
                        val vistoPorList = document.get("vistoPor") as? List<String> ?: emptyList()

                        val reporte = Reporte(
                            id = document.id,
                            tipoIncidente = document.getString("tipoIncidente") ?: "",
                            titulo = document.getString("titulo") ?: "",
                            descripcion = document.getString("descripcion") ?: "",
                            estado = document.getString("estado") ?: "Pendiente",
                            fecha = document.getLong("fecha") ?: 0L,
                            prioridad = document.getString("prioridad") ?: "Media",
                            direccion = document.getString("direccion") ?: "",
                            latitud = document.getDouble("latitud") ?: 0.0,
                            longitud = document.getDouble("longitud") ?: 0.0,
                            usuarioId = document.getString("usuarioId") ?: "",
                            numFotos = document.getLong("numFotos")?.toInt() ?: 0,
                            fotoUrl = document.getString("fotoUrl") ?: "",
                            vistoPor = vistoPorList  // ⭐ NUEVO
                        )
                        reportesList.add(reporte)
                    }
                    adapter.notifyDataSetChanged()
                    android.util.Log.d("ReportesCategoria", "Lista actualizada con ${reportesList.size} reportes")
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvNoReportes.visibility = View.VISIBLE
                binding.tvNoReportes.text = "Error al cargar reportes: ${e.message}"
                android.util.Log.e("ReportesCategoria", "ERROR: ${e.message}", e)
            }
    }

    private fun mostrarDialogoEditar(reporte: Reporte) {
        val estados = arrayOf("Pendiente", "En Proceso", "Resuelto", "Rechazado")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cambiar estado del reporte")
            .setItems(estados) { _, which ->
                val nuevoEstado = estados[which]
                actualizarEstadoReporte(reporte.id, nuevoEstado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarEstadoReporte(reporteId: String, nuevoEstado: String) {
        db.collection("reportes").document(reporteId)
            .update("estado", nuevoEstado)
            .addOnSuccessListener {
                Toast.makeText(this, "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
                enviarNotificacionCambioEstado(reporteId, nuevoEstado)
                cargarReportes()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun enviarNotificacionCambioEstado(reporteId: String, nuevoEstado: String) {
        db.collection("reportes").document(reporteId).get()
            .addOnSuccessListener { document ->
                val usuarioId = document.getString("usuarioId") ?: return@addOnSuccessListener
                val titulo = document.getString("titulo") ?: "Tu reporte"

                db.collection("usuarios").document(usuarioId).get()
                    .addOnSuccessListener { userDoc ->
                        val fcmToken = userDoc.getString("fcmToken") ?: return@addOnSuccessListener

                        val emoji = when (nuevoEstado) {
                            "En Proceso" -> "🔧"
                            "Resuelto" -> "✅"
                            "Rechazado" -> "❌"
                            else -> "📋"
                        }

                        val notificacion = hashMapOf(
                            "to" to fcmToken,
                            "notification" to hashMapOf(
                                "title" to "$emoji Estado actualizado",
                                "body" to "\"$titulo\" ahora está: $nuevoEstado"
                            ),
                            "data" to hashMapOf(
                                "tipo" to "cambio_estado",
                                "reporteId" to reporteId,
                                "estado" to nuevoEstado
                            )
                        )

                        db.collection("notificaciones_pendientes").add(notificacion)
                    }
            }
    }

    // ⭐ NUEVA FUNCIÓN: Marcar reporte como visto por el trabajador
    private fun marcarReporteComoVisto(reporte: Reporte) {
        val trabajadorId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (reporte.vistoPor.contains(trabajadorId)) {
            return
        }

        val vistoPorActualizado = reporte.vistoPor.toMutableList()
        vistoPorActualizado.add(trabajadorId)

        db.collection("reportes").document(reporte.id)
            .update("vistoPor", vistoPorActualizado)
            .addOnSuccessListener {
                android.util.Log.d("ReportesCategoria", "Reporte ${reporte.id} marcado como visto")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ReportesCategoria", "Error al marcar como visto: ${e.message}")
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}