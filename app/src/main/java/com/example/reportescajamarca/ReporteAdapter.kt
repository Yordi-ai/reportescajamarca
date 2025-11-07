package com.example.reportescajamarca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ReporteAdapter(
    private val reportes: List<Reporte>,
    private val onChatClick: (Reporte) -> Unit
) : RecyclerView.Adapter<ReporteAdapter.ReporteViewHolder>() {

    inner class ReporteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTipoIncidente: TextView = itemView.findViewById(R.id.tvTipoIncidente)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        val tvPrioridad: TextView = itemView.findViewById(R.id.tvPrioridad)
        val tvDireccion: TextView = itemView.findViewById(R.id.tvDireccion)
        val ivFotoReporte: ImageView = itemView.findViewById(R.id.ivFotoReporte)
        val btnChat: Button = itemView.findViewById(R.id.btnChat)
        val btnVerDetalle: Button = itemView.findViewById(R.id.btnVerDetalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reporte, parent, false)
        return ReporteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        val reporte = reportes[position]

        holder.tvTipoIncidente.text = reporte.tipoIncidente
        holder.tvTitulo.text = reporte.titulo
        holder.tvFecha.text = formatearFecha(reporte.fecha)
        holder.tvEstado.text = reporte.estado
        holder.tvPrioridad.text = "Prioridad: ${reporte.prioridad}"
        holder.tvDireccion.text = "📍 ${reporte.direccion}"

        // Cargar imagen si existe
        if (reporte.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(reporte.fotoUrl)
                .into(holder.ivFotoReporte)
        }

        // Click en botón de chat
        holder.btnChat.setOnClickListener {
            onChatClick(reporte)
        }

        // Click en botón de detalles
        holder.btnVerDetalle.setOnClickListener {
            // Aquí puedes agregar lógica para ver detalles
        }
    }

    override fun getItemCount() = reportes.size

    private fun formatearFecha(timestamp: Long): String {
        if (timestamp == 0L) return "Fecha no disponible"

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}