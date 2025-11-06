package com.example.reportescajamarca

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reportescajamarca.databinding.ActivityMapPickerBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import java.util.*

class MapPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapPickerBinding
    private lateinit var mapView: MapView
    private var selectedLocation: GeoPoint? = null
    private var selectedAddress: String = ""
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Seleccionar Ubicación"

        // Configurar OSMDroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        // Inicializar el mapa
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Centrar el mapa en Cajamarca, Perú
        val cajamarca = GeoPoint(-7.1607, -78.5136)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(cajamarca)

        // Configurar click en el mapa
        mapView.setOnClickListener {
            false
        }

        // Detectar toque en el mapa
        val mapEventsOverlay = org.osmdroid.views.overlay.MapEventsOverlay(object : org.osmdroid.events.MapEventsReceiver {
            override fun singleTapConfirmedHelper(geoPoint: GeoPoint): Boolean {
                // Eliminar marcador anterior
                marker?.let { mapView.overlays.remove(it) }

                // Crear nuevo marcador
                marker = Marker(mapView)
                marker?.position = geoPoint
                marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker?.title = "Ubicación seleccionada"
                mapView.overlays.add(marker)
                mapView.invalidate()

                selectedLocation = geoPoint

                // Obtener dirección
                obtenerDireccion(geoPoint.latitude, geoPoint.longitude)

                binding.tvUbicacionSeleccionada.text = "📍 Lat: ${String.format("%.6f", geoPoint.latitude)}, Lon: ${String.format("%.6f", geoPoint.longitude)}"

                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })

        mapView.overlays.add(0, mapEventsOverlay)

        binding.btnConfirmarUbicacion.setOnClickListener {
            if (selectedLocation != null) {
                val intent = Intent()
                intent.putExtra("LATITUD", selectedLocation!!.latitude)
                intent.putExtra("LONGITUD", selectedLocation!!.longitude)
                intent.putExtra("DIRECCION", selectedAddress)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerDireccion(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                selectedAddress = address.getAddressLine(0) ?: "Dirección no disponible"
                binding.tvDireccion.text = selectedAddress
            } else {
                selectedAddress = "Lat: $lat, Lon: $lon"
                binding.tvDireccion.text = "Coordenadas: ${String.format("%.6f", lat)}, ${String.format("%.6f", lon)}"
            }
        } catch (e: IOException) {
            selectedAddress = "Lat: $lat, Lon: $lon"
            binding.tvDireccion.text = "Coordenadas: ${String.format("%.6f", lat)}, ${String.format("%.6f", lon)}"
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}