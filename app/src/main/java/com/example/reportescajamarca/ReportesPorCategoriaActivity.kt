package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reportescajamarca.databinding.ActivityReportesPorCategoriaBinding
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
                // Callback para editar estado del reporte
                mostrarDialogoEditar(reporte)
            },
            onChatClick = { reporte ->
                // ⭐ NUEVO: Callback para abrir chat
                abrirChat(reporte)
            }
        )
        binding.recyclerViewReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReportes.adapter = adapter
    }

    // ⭐ NUEVA FUNCIÓN: Abrir chat
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
        // Resetear todos los botones
        binding.btnTodos.alpha = 0.5f
        binding.btnPendientes.alpha = 0.5f
        binding.btnEnProceso.alpha = 0.5f
        binding.btnResueltos.alpha = 0.5f

        // Resaltar el botón activo
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

        // Aplicar filtro de estado si no es "Todos"
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
                            fotoUrl = document.getString("fotoUrl") ?: ""
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
                android.widget.Toast.makeText(
                    this,
                    "Estado actualizado a: $nuevoEstado",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                cargarReportes() // Recargar la lista
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(
                    this,
                    "Error al actualizar: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}