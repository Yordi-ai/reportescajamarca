package com.example.reportescajamarca

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.reportescajamarca.databinding.ActivityReportFormBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ReportFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportFormBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var tipoIncidente: String = ""
    private val fotosUris = mutableListOf<Uri>()
    private var fotoUri: Uri? = null
    private var latitud: Double? = null
    private var longitud: Double? = null
    private var direccion: String = ""

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            fotoUri?.let { uri ->
                fotosUris.add(uri)
                actualizarContadorFotos()
                Toast.makeText(this, "Foto agregada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            fotosUris.add(it)
            actualizarContadorFotos()
            Toast.makeText(this, "Foto agregada", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tomarFoto()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                obtenerUbicacion()
            }
            else -> {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            latitud = data?.getDoubleExtra("LATITUD", 0.0)
            longitud = data?.getDoubleExtra("LONGITUD", 0.0)
            direccion = data?.getStringExtra("DIRECCION") ?: ""

            binding.tvUbicacion.text = "📍 Lat: ${String.format("%.6f", latitud)}, Lon: ${String.format("%.6f", longitud)}"
            binding.tvDireccion.text = "📍 $direccion"
            binding.tvDireccion.visibility = android.view.View.VISIBLE
            Toast.makeText(this, "Ubicación seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tipoIncidente = intent.getStringExtra("TIPO_INCIDENTE") ?: "Sin especificar"

        configurarUI()
        configurarListeners()
    }

    private fun configurarUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Nuevo Reporte"

        binding.tvTipoIncidente.text = "Reportar: $tipoIncidente"

        val prioridades = arrayOf("Baja", "Media", "Alta", "Urgente")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, prioridades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPrioridad.adapter = adapter
    }

    private fun configurarListeners() {
        binding.btnTomarFoto.setOnClickListener {
            mostrarOpcionesFoto()
        }

        binding.btnUsarUbicacion.setOnClickListener {
            verificarPermisosUbicacion()
        }

        binding.btnSeleccionarMapa.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        binding.btnEnviarReporte.setOnClickListener {
            enviarReporte()
        }

        binding.btnGuardarBorrador.setOnClickListener {
            guardarBorrador()
        }
    }

    private fun verificarPermisosUbicacion() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                obtenerUbicacion()
            }
            else -> {
                requestLocationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun obtenerUbicacion() {
        try {
            binding.btnUsarUbicacion.isEnabled = false
            binding.btnUsarUbicacion.text = "Obteniendo ubicación..."

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val cancellationTokenSource = CancellationTokenSource()

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        latitud = location.latitude
                        longitud = location.longitude

                        obtenerDireccion(location.latitude, location.longitude)

                        binding.tvUbicacion.text = "📍 Lat: ${String.format("%.6f", latitud)}, Lon: ${String.format("%.6f", longitud)}"
                        binding.tvDireccion.visibility = android.view.View.VISIBLE
                        Toast.makeText(this, "Ubicación obtenida correctamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                    }
                    binding.btnUsarUbicacion.isEnabled = true
                    binding.btnUsarUbicacion.text = "Usar mi ubicación"
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnUsarUbicacion.isEnabled = true
                    binding.btnUsarUbicacion.text = "Usar mi ubicación"
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.btnUsarUbicacion.isEnabled = true
            binding.btnUsarUbicacion.text = "Usar mi ubicación"
        }
    }

    private fun obtenerDireccion(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                direccion = address.getAddressLine(0) ?: "Dirección no disponible"
                binding.tvDireccion.text = "📍 $direccion"
            }
        } catch (e: IOException) {
            direccion = "Lat: $lat, Lon: $lon"
            binding.tvDireccion.text = "📍 Coordenadas: ${String.format("%.6f", lat)}, ${String.format("%.6f", lon)}"
        }
    }

    private fun mostrarOpcionesFoto() {
        val opciones = arrayOf("Tomar foto", "Seleccionar de galería")
        AlertDialog.Builder(this)
            .setTitle("Agregar fotografía")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> verificarPermisosCamara()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun verificarPermisosCamara() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                tomarFoto()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun tomarFoto() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        val photoFile = File.createTempFile("FOTO_${timeStamp}_", ".jpg", storageDir)

        fotoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile
        )

        fotoUri?.let { uri ->
            takePictureLauncher.launch(uri)
        }
    }

    private fun actualizarContadorFotos() {
        binding.tvContadorFotos.text = "${fotosUris.size} foto(s) agregada(s)"
    }

    private fun enviarReporte() {
        val titulo = binding.etTitulo.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()
        val prioridad = binding.spinnerPrioridad.selectedItem.toString()

        if (titulo.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (descripcion.isEmpty()) {
            Toast.makeText(this, "La descripción es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        if (latitud == null || longitud == null) {
            Toast.makeText(this, "La ubicación es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar envío")
            .setMessage("¿Estás seguro de enviar este reporte?\n\nTipo: $tipoIncidente\nTítulo: $titulo")
            .setPositiveButton("Sí, enviar") { _, _ ->
                procesarEnvioReporte(titulo, descripcion, prioridad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procesarEnvioReporte(titulo: String, descripcion: String, prioridad: String) {
        val userId = auth.currentUser?.uid ?: return

        // Bloquear TODO
        binding.btnEnviarReporte.isEnabled = false
        binding.btnGuardarBorrador.isEnabled = false
        binding.etTitulo.isEnabled = false
        binding.etDescripcion.isEnabled = false
        binding.btnTomarFoto.isEnabled = false
        binding.btnUsarUbicacion.isEnabled = false
        binding.btnSeleccionarMapa.isEnabled = false
        binding.spinnerPrioridad.isEnabled = false
        binding.btnEnviarReporte.text = "Enviando..."

        // Si hay fotos, subirlas primero
        if (fotosUris.isNotEmpty()) {
            subirFotosYCrearReporte(userId, titulo, descripcion, prioridad)
        } else {
            // Si no hay fotos, crear reporte directamente
            crearReporteEnFirestore(userId, titulo, descripcion, prioridad, "")
        }
    }

    private fun subirFotosYCrearReporte(userId: String, titulo: String, descripcion: String, prioridad: String) {
        val primeraFoto = fotosUris[0]

        try {
            // Convertir imagen a Base64
            val inputStream = contentResolver.openInputStream(primeraFoto)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

            // Redimensionar para que no sea tan pesada
            val maxWidth = 800
            val maxHeight = 800
            val scale = Math.min(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
            )
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)

            // Guardar como data URL
            val fotoUrl = "data:image/jpeg;base64,$base64"
            crearReporteEnFirestore(userId, titulo, descripcion, prioridad, fotoUrl)

        } catch (e: Exception) {
            Log.e("Foto", "Error al procesar imagen: ${e.message}")
            Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
            crearReporteEnFirestore(userId, titulo, descripcion, prioridad, "")
        }
    }

    private fun crearReporteEnFirestore(userId: String, titulo: String, descripcion: String, prioridad: String, fotoUrl: String) {
        val reporte = hashMapOf(
            "tipoIncidente" to tipoIncidente,
            "titulo" to titulo,
            "descripcion" to descripcion,
            "prioridad" to prioridad,
            "usuarioId" to userId,
            "fecha" to System.currentTimeMillis(),
            "estado" to "Pendiente",
            "latitud" to latitud,
            "longitud" to longitud,
            "direccion" to direccion,
            "numFotos" to fotosUris.size,
            "fotoUrl" to fotoUrl
        )

        db.collection("reportes").add(reporte)

        Toast.makeText(this, "✅ Reporte enviado exitosamente", Toast.LENGTH_SHORT).show()

        binding.root.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }, 500)
    }

    private fun guardarBorrador() {
        Toast.makeText(this, "Reporte guardado como borrador", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}