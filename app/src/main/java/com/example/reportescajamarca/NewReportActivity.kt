package com.example.reportescajamarca

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.reportescajamarca.databinding.ActivityNewReportBinding

class NewReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewReportBinding

    private val reportFormLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Si el reporte fue enviado exitosamente, cierra esta actividad también
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Selecciona el tipo de incidente"

        // TODOS los 8 cards configurados
        binding.cardBaches.setOnClickListener {
            abrirFormulario("Baches en Calle")
        }

        binding.cardAlumbrado.setOnClickListener {
            abrirFormulario("Alumbrado Público")
        }

        binding.cardSenalizacion.setOnClickListener {
            abrirFormulario("Señalización")
        }

        binding.cardBasura.setOnClickListener {
            abrirFormulario("Basura y Limpieza")
        }

        binding.cardMobiliario.setOnClickListener {
            abrirFormulario("Mobiliario Urbano")
        }

        binding.cardParques.setOnClickListener {
            abrirFormulario("Parques y Jardines")
        }

        binding.cardAgua.setOnClickListener {
            abrirFormulario("Agua y Drenaje")
        }

        binding.cardOtros.setOnClickListener {
            abrirFormulario("Otros Incidentes")
        }

        // Botón de emergencia
        binding.btnEmergencia.setOnClickListener {
            abrirFormulario("EMERGENCIA")
        }
    }

    private fun abrirFormulario(tipoIncidente: String) {
        val intent = Intent(this, ReportFormActivity::class.java)
        intent.putExtra("TIPO_INCIDENTE", tipoIncidente)
        reportFormLauncher.launch(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}