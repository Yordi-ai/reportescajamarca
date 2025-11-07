package com.example.reportescajamarca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val mensajes: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ENVIADO = 1
        private const val VIEW_TYPE_RECIBIDO = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (mensajes[position].senderId == currentUserId) {
            VIEW_TYPE_ENVIADO
        } else {
            VIEW_TYPE_RECIBIDO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ENVIADO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mensaje_enviado, parent, false)
            MensajeEnviadoViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mensaje_recibido, parent, false)
            MensajeRecibidoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = mensajes[position]

        if (holder is MensajeEnviadoViewHolder) {
            holder.bind(mensaje)
        } else if (holder is MensajeRecibidoViewHolder) {
            holder.bind(mensaje)
        }
    }

    override fun getItemCount() = mensajes.size

    inner class MensajeEnviadoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMensaje: TextView = itemView.findViewById(R.id.tv_mensaje)
        private val tvHora: TextView = itemView.findViewById(R.id.tv_hora)

        fun bind(mensaje: Message) {
            tvMensaje.text = mensaje.message
            tvHora.text = formatTime(mensaje.timestamp)
        }
    }

    inner class MensajeRecibidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMensaje: TextView = itemView.findViewById(R.id.tv_mensaje)
        private val tvHora: TextView = itemView.findViewById(R.id.tv_hora)
        private val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre)

        fun bind(mensaje: Message) {
            tvMensaje.text = mensaje.message
            tvHora.text = formatTime(mensaje.timestamp)

            if (mensaje.senderType == "municipalidad") {
                tvNombre.text = "Municipalidad de Cajamarca"
            } else {
                tvNombre.text = mensaje.senderName
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.timeInMillis = timestamp
        val messageDay = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = now
        val today = calendar.get(Calendar.DAY_OF_YEAR)

        return when {
            messageDay == today -> {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            messageDay == today - 1 -> {
                "Ayer ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
            }
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}