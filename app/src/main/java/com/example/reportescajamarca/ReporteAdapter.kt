package com.example.reportescajamarca

import android.graphics.Color
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
    private var reportes: List<Reporte>,
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
        val btnVerDetalles: Button = itemView.findViewById(R.id.btnVerDetalles)
        val vIndicadorEstado: View = itemView.findViewById(R.id.vIndicadorEstado)
        val tvBadgeChat: TextView = itemView.findViewById(R.id.tvBadgeChat)
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
        holder.tvFecha.text = convertirFecha(reporte.fecha)
        holder.tvDireccion.text = reporte.direccion
        holder.tvPrioridad.text = reporte.prioridad

        // Configurar estado con punto de color
        configurarEstado(holder, reporte.estado)

        // Cargar imagen con Glide
        if (reporte.fotoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(reporte.fotoUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(holder.ivFotoReporte)
        } else {
            holder.ivFotoReporte.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Badge de mensajes no leídos
        val mensajesNoLeidos = reporte.mensajesNoLeidos
        if (mensajesNoLeidos > 0) {
            holder.tvBadgeChat.visibility = View.VISIBLE
            holder.tvBadgeChat.text = if (mensajesNoLeidos > 9) "9+" else mensajesNoLeidos.toString()
        } else {
            holder.tvBadgeChat.visibility = View.GONE
        }

        // Click en chat
        holder.btnChat.setOnClickListener {
            onChatClick(reporte)
        }
    }

    override fun getItemCount(): Int = reportes.size

    private fun convertirFecha(timestamp: Long): String {
        if (timestamp == 0L) return "Sin fecha"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fecha = Date(timestamp)
        return sdf.format(fecha)
    }

    private fun configurarEstado(holder: ReporteViewHolder, estado: String) {
        when (estado) {
            "Pendiente" -> {
                holder.tvEstado.text = "Pendiente"
                holder.tvEstado.setTextColor(Color.parseColor("#FF9800"))
                holder.vIndicadorEstado.background?.setTint(Color.parseColor("#FF9800"))
            }
            "En Proceso" -> {
                holder.tvEstado.text = "En Proceso"
                holder.tvEstado.setTextColor(Color.parseColor("#2196F3"))
                holder.vIndicadorEstado.background?.setTint(Color.parseColor("#2196F3"))
            }
            "Resuelto" -> {
                holder.tvEstado.text = "Resuelto"
                holder.tvEstado.setTextColor(Color.parseColor("#4CAF50"))
                holder.vIndicadorEstado.background?.setTint(Color.parseColor("#4CAF50"))
            }
            "Rechazado" -> {
                holder.tvEstado.text = "Rechazado"
                holder.tvEstado.setTextColor(Color.parseColor("#F44336"))
                holder.vIndicadorEstado.background?.setTint(Color.parseColor("#F44336"))
            }
            else -> {
                holder.tvEstado.text = estado
                holder.tvEstado.setTextColor(Color.parseColor("#999999"))
                holder.vIndicadorEstado.background?.setTint(Color.parseColor("#999999"))
            }
        }
    }

    fun actualizarReportes(nuevosReportes: List<Reporte>) {
        reportes = nuevosReportes
        notifyDataSetChanged()
    }
}