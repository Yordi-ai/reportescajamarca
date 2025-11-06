package com.example.reportescajamarca

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reportescajamarca.databinding.ActivityMisReportesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MisReportesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMisReportesBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ReportesAdapter
    private val reportesList = mutableListOf<Reporte>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisReportesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mis Reportes"

        configurarRecyclerView()
        cargarReportes()
    }

    private fun configurarRecyclerView() {
        adapter = ReportesAdapter(reportesList)
        binding.recyclerViewReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReportes.adapter = adapter
    }

    private fun cargarReportes() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoReportes.visibility = View.GONE

        db.collection("reportes")
            .whereEqualTo("usuarioId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                reportesList.clear()

                if (documents.isEmpty) {
                    binding.tvNoReportes.visibility = View.VISIBLE
                } else {
                    for (document in documents) {
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
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvNoReportes.visibility = View.VISIBLE
                binding.tvNoReportes.text = "Error al cargar reportes: ${e.message}"
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}