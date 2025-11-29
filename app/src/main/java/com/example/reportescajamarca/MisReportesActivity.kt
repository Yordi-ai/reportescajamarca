package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MisReportesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReporteAdapter
    private lateinit var progressBar: ProgressBar
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

        initViews()
        cargarReportesConMensajes()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewReportes)
        progressBar = findViewById(R.id.progressBar)

        adapter = ReporteAdapter(reportes) { reporte ->
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

    private fun cargarReportesConMensajes() {
        val currentUser = auth.currentUser ?: return

        progressBar.visibility = View.VISIBLE
        recyclerView.alpha = 0f  // Invisible mientras carga

        db.collection("reportes")
            .whereEqualTo("usuarioId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                if (documents == null || documents.isEmpty) {
                    reportes.clear()
                    adapter.notifyDataSetChanged()
                    recyclerView.animate().alpha(1f).setDuration(300).start()
                    return@addOnSuccessListener
                }

                val nuevosReportes = mutableListOf<Reporte>()

                for (document in documents) {
                    val reporte = document.toObject(Reporte::class.java)
                    reporte.id = document.id
                    nuevosReportes.add(reporte)
                }

                reportes.clear()
                reportes.addAll(nuevosReportes)
                reportes.sortByDescending { it.fecha }
                adapter.notifyDataSetChanged()

                // Animación suave para aparecer
                recyclerView.animate().alpha(1f).setDuration(300).start()

                reportes.forEachIndexed { index, reporte ->
                    contarMensajesNoLeidos(reporte.id) { contador ->
                        reportes[index] = reporte.copy(mensajesNoLeidos = contador)
                        adapter.notifyItemChanged(index)
                    }
                }
            }
            .addOnFailureListener { error ->
                progressBar.visibility = View.GONE
                recyclerView.animate().alpha(1f).setDuration(300).start()
            }
    }

    private fun contarMensajesNoLeidos(reporteId: String, callback: (Int) -> Unit) {
        val currentUser = auth.currentUser ?: return callback(0)

        db.collection("reportes")
            .document(reporteId)
            .collection("mensajes")
            .whereEqualTo("leido", false)
            .whereNotEqualTo("usuarioId", currentUser.uid)
            .get()
            .addOnSuccessListener { mensajes ->
                callback(mensajes.size())
            }
            .addOnFailureListener {
                callback(0)
            }
    }

    override fun onResume() {
        super.onResume()
        // Solo actualizar badges cuando vuelves de otra pantalla
        if (reportes.isNotEmpty()) {
            reportes.forEachIndexed { index, reporte ->
                contarMensajesNoLeidos(reporte.id) { contador ->
                    if (reportes[index].mensajesNoLeidos != contador) {
                        reportes[index] = reporte.copy(mensajesNoLeidos = contador)
                        adapter.notifyItemChanged(index)
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}