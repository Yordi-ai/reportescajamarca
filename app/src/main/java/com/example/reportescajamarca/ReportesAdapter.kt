package com.example.reportescajamarca

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ReportesAdapter(
    private val reportes: List<Reporte>,
    private val onReporteClick: (Reporte) -> Unit = {}
) : RecyclerView.Adapter<ReportesAdapter.ReporteViewHolder>() {

    class ReporteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardReporte)
        val tvTipoIncidente: TextView = view.findViewById(R.id.tvTipoIncidente)
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvPrioridad: TextView = view.findViewById(R.id.tvPrioridad)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
        val ivFotoReporte: ImageView = view.findViewById(R.id.ivFotoReporte)
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
        holder.tvEstado.text = reporte.estado
        holder.tvPrioridad.text = "Prioridad: ${reporte.prioridad}"
        holder.tvDireccion.text = "📍 ${reporte.direccion}"

        // Formatear fecha
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvFecha.text = sdf.format(Date(reporte.fecha))

        // Color según estado
        when (reporte.estado) {
            "Pendiente" -> holder.tvEstado.setTextColor(Color.parseColor("#FF9800"))
            "En Proceso" -> holder.tvEstado.setTextColor(Color.parseColor("#2196F3"))
            "Resuelto" -> holder.tvEstado.setTextColor(Color.parseColor("#4CAF50"))
            "Rechazado" -> holder.tvEstado.setTextColor(Color.parseColor("#F44336"))
            else -> holder.tvEstado.setTextColor(Color.parseColor("#666666"))
        }

        // NUEVO: Cargar imagen con Glide
        if (reporte.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(reporte.fotoUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(holder.ivFotoReporte)
        } else {
            holder.ivFotoReporte.setImageResource(R.drawable.ic_launcher_foreground)
            holder.ivFotoReporte.setBackgroundColor(Color.parseColor("#E0E0E0"))
        }

        // Click en la tarjeta para ver detalle
        holder.cardView.setOnClickListener {
            onReporteClick(reporte)
        }
    }

    override fun getItemCount() = reportes.size
}