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
        cargarReportesConMensajes()
    }

    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewReportes)

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

        db.collection("reportes")
            .whereEqualTo("usuarioId", currentUser.uid)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (documents == null || documents.isEmpty) {
                    reportes.clear()
                    adapter.notifyDataSetChanged()
                    return@addSnapshotListener
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

                reportes.forEachIndexed { index, reporte ->
                    contarMensajesNoLeidos(reporte.id) { contador ->
                        reportes[index] = reporte.copy(mensajesNoLeidos = contador)
                        adapter.notifyItemChanged(index)
                    }
                }
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        cargarReportesConMensajes()
    }
}