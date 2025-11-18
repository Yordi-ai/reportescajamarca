package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.reportescajamarca.databinding.ActivityTrabajadorMainBinding

class TrabajadorMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrabajadorMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrabajadorMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.tvWelcome.text = "Bienvenido\n${currentUser.email}"

        // ⭐ Cargar badges al inicio
        cargarBadgesCategorias()

        // Botones de cada categoría
        binding.btnBaches.setOnClickListener { abrirCategoria("Baches en Calle") }
        binding.btnAlumbrado.setOnClickListener { abrirCategoria("Alumbrado Público") }
        binding.btnSenalizacion.setOnClickListener { abrirCategoria("Señalización") }
        binding.btnBasura.setOnClickListener { abrirCategoria("Basura y Limpieza") }
        binding.btnMobiliario.setOnClickListener { abrirCategoria("Mobiliario Urbano") }
        binding.btnParques.setOnClickListener { abrirCategoria("Parques y Jardines") }
        binding.btnAgua.setOnClickListener { abrirCategoria("Agua y Drenaje") }
        binding.btnOtros.setOnClickListener { abrirCategoria("Otros Incidentes") }
        binding.btnEmergencias.setOnClickListener { abrirCategoria("EMERGENCIA") }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarBadgesCategorias()
    }

    private fun cargarBadgesCategorias() {
        val categorias = mapOf(
            "Baches en Calle" to binding.badgeBaches,
            "Alumbrado Público" to binding.badgeAlumbrado,
            "Señalización" to binding.badgeSenalizacion,
            "Basura y Limpieza" to binding.badgeBasura,
            "Mobiliario Urbano" to binding.badgeMobiliario,
            "Parques y Jardines" to binding.badgeParques,
            "Agua y Drenaje" to binding.badgeAgua,
            "Otros Incidentes" to binding.badgeOtros,
            "EMERGENCIA" to binding.badgeEmergencias
        )

        categorias.forEach { (categoria, badgeView) ->
            db.collection("reportes")
                .whereEqualTo("tipoIncidente", categoria)
                .whereEqualTo("estado", "Pendiente")
                .get()
                .addOnSuccessListener { documents ->
                    val cantidad = documents.size()
                    actualizarBadge(badgeView, cantidad)
                }
        }
    }

    private fun actualizarBadge(badgeView: TextView, cantidad: Int) {
        if (cantidad > 0) {
            badgeView.text = if (cantidad > 99) "99+" else cantidad.toString()
            badgeView.visibility = View.VISIBLE
        } else {
            badgeView.visibility = View.GONE
        }
    }

    private fun abrirCategoria(categoria: String) {
        val intent = Intent(this, ReportesPorCategoriaActivity::class.java)
        intent.putExtra("CATEGORIA", categoria)
        startActivity(intent)
    }
}