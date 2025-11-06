package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.reportescajamarca.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    // Launcher para NewReportActivity
    private val newReportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Cuando vuelve de NewReportActivity, ya está listo para enviar otro reporte
        Log.d("MainActivity", "Volvió de NewReportActivity con result: ${result.resultCode}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.tvWelcome.text = "Bienvenido\n${currentUser.email}"

        // Botón Crear Nuevo Reporte
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

        // NUEVO: Botón Mis Reportes
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

        // Botón Cerrar Sesión
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}