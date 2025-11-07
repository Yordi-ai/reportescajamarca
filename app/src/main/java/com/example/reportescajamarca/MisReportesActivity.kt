package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MisReportesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReporteAdapter
    private val reportes = mutableListOf<Reporte>()

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_reportes)

        supportActionBar?.title = "Mis Reportes"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initRecyclerView()
        cargarReportes()
    }

    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewReportes)

        adapter = ReporteAdapter(reportes) { reporte ->
            // Cuando se hace click en el botón de chat
            abrirChat(reporte)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun abrirChat(reporte: Reporte) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("REPORTE_ID", reporte.id)
        intent.putExtra("REPORTE_TITULO", reporte.titulo)
        startActivity(intent)
    }

    private fun cargarReportes() {
        val currentUser = auth.currentUser ?: return

        db.collection("reportes")
            .whereEqualTo("usuarioId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                reportes.clear()

                for (document in documents) {
                    val reporte = document.toObject(Reporte::class.java)
                    reporte.id = document.id
                    reportes.add(reporte)
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Manejar error
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}