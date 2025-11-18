package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.reportescajamarca.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val newReportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "Volvió de NewReportActivity con result: ${result.resultCode}")
        cargarEstadisticas()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        configurarPerfil(currentUser.email ?: "Usuario")
        cargarEstadisticas()

        binding.btnNewReport.setOnClickListener {
            try {
                Log.d("MainActivity", "Intentando abrir NewReportActivity")
                val intent = Intent(this, NewReportActivity::class.java)
                newReportLauncher.launch(intent)
                Log.d("MainActivity", "Intent lanzado correctamente")
            } catch (e: Exception) {
                Log.e("MainActivity", "ERROR al abrir NewReportActivity: ${e.message}", e)
                Toast.makeText(this, "ERROR: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnMisReportes.setOnClickListener {
            try {
                Log.d("MainActivity", "Intentando abrir MisReportesActivity")
                val intent = Intent(this, MisReportesActivity::class.java)
                startActivity(intent)
                Log.d("MainActivity", "Intent MisReportes lanzado correctamente")
            } catch (e: Exception) {
                Log.e("MainActivity", "ERROR al abrir MisReportesActivity: ${e.message}", e)
                Toast.makeText(this, "ERROR: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarEstadisticas()
    }

    private fun configurarPerfil(email: String) {
        val nombre = email.substringBefore("@")
        binding.tvNombreUsuario.text = nombre.replaceFirstChar { it.uppercase() }
        binding.tvEmailUsuario.text = email
    }

    private fun cargarEstadisticas() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("reportes")
            .whereEqualTo("usuarioId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val total = documents.size()
                val pendientes = documents.count { it.getString("estado") == "Pendiente" }
                val enProceso = documents.count { it.getString("estado") == "En Proceso" }
                val resueltos = documents.count { it.getString("estado") == "Resuelto" }
                val rechazados = documents.count { it.getString("estado") == "Rechazado" }

                binding.tvTotalReportes.text = total.toString()
                binding.tvReportesPendientes.text = pendientes.toString()
                binding.tvReportesEnProceso.text = enProceso.toString()
                binding.tvReportesResueltos.text = resueltos.toString()
                binding.tvReportesRechazados.text = rechazados.toString()

                Log.d(
                    "MainActivity",
                    "Estadísticas: Total=$total, Pendientes=$pendientes, En Proceso=$enProceso, Resueltos=$resueltos, Rechazados=$rechazados"
                )
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al cargar estadísticas: ${e.message}")
                binding.tvTotalReportes.text = "0"
                binding.tvReportesPendientes.text = "0"
                binding.tvReportesEnProceso.text = "0"
                binding.tvReportesResueltos.text = "0"
                binding.tvReportesRechazados.text = "0"
            }
    }
}