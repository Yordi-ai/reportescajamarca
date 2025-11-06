package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.reportescajamarca.databinding.ActivityTrabajadorMainBinding

class TrabajadorMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrabajadorMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrabajadorMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.tvWelcome.text = "Bienvenido\n${currentUser.email}"

        // Botones de cada categoría
        binding.btnBaches.setOnClickListener {
            abrirCategoria("Baches en Calle")
        }

        binding.btnAlumbrado.setOnClickListener {
            abrirCategoria("Alumbrado Público")
        }

        binding.btnSenalizacion.setOnClickListener {
            abrirCategoria("Señalización")
        }

        binding.btnBasura.setOnClickListener {
            abrirCategoria("Basura y Limpieza")
        }

        binding.btnMobiliario.setOnClickListener {
            abrirCategoria("Mobiliario Urbano")
        }

        binding.btnParques.setOnClickListener {
            abrirCategoria("Parques y Jardines")
        }

        binding.btnAgua.setOnClickListener {
            abrirCategoria("Agua y Drenaje")
        }

        binding.btnOtros.setOnClickListener {
            abrirCategoria("Otros Incidentes")
        }

        binding.btnEmergencias.setOnClickListener {
            abrirCategoria("EMERGENCIA")
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun abrirCategoria(categoria: String) {
        val intent = Intent(this, ReportesPorCategoriaActivity::class.java)
        intent.putExtra("CATEGORIA", categoria)
        startActivity(intent)
    }
}