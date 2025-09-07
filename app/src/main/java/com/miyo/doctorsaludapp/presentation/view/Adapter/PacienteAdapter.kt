package com.miyo.doctorsaludapp.presentation.view.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.miyo.doctorsaludapp.R
// ⬇️ USA TU MODELO REAL
import com.miyo.doctorsaludapp.domain.model.Patient
import java.text.SimpleDateFormat
import java.util.Locale

class PacienteAdapter(
    private val onVerDetalles: (Patient) -> Unit,
    private val onVerEcg: (Patient) -> Unit
) : RecyclerView.Adapter<PacienteAdapter.VH>() {

    private val data = mutableListOf<Patient>()
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun submit(list: List<Patient>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_paciente, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(data[position], df, onVerDetalles, onVerEcg)
    }

    override fun getItemCount(): Int = data.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvDni: TextView = itemView.findViewById(R.id.tvDni)
        private val tvEdad: TextView = itemView.findViewById(R.id.tvEdad)
        private val tvSexo: TextView = itemView.findViewById(R.id.tvSexo)
        private val tvCirugia: TextView = itemView.findViewById(R.id.tvCirugia)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val chipEstado: TextView = itemView.findViewById(R.id.chipEstado)
        private val chipRiesgo: TextView = itemView.findViewById(R.id.chipRiesgo)
        private val btnDetalles: MaterialButton = itemView.findViewById(R.id.btnVerDetalles)
        private val btnEcg: MaterialButton = itemView.findViewById(R.id.btnVerEcg)

        fun bind(
            p: Patient,
            df: SimpleDateFormat,
            onVerDetalles: (Patient) -> Unit,
            onVerEcg: (Patient) -> Unit
        ) {
            // Ajusta los nombres a tu modelo real si cambia alguno
            tvNombre.text = p.nombreCompleto ?: p.nombre ?: ""
            tvDni.text = "DNI: ${p.dni ?: ""}"
            tvEdad.text = p.edad?.let { "Edad: $it años" } ?: "Edad: "
            tvSexo.text = "Sexo: ${p.sexo ?: ""}"
            tvCirugia.text = "Cirugía: ${p.cirugia ?: ""}"
            tvFecha.text = "Fecha: ${p.fechaCirugia?.let { df.format(it) } ?: ""}"

            // Chip estado (semáforo)
            val ctx = itemView.context
            val estado = (p.estado ?: "").lowercase(Locale.getDefault())
            when {
                estado.contains("apto") || estado.contains("aproba") -> {
                    chipEstado.text = if (estado.contains("aproba")) "Aprobado" else "Apto"
                    chipEstado.setBackgroundResource(R.drawable.bg_chip_green)
                    chipEstado.setTextColor(ctx.getColor(R.color.chip_green_text))
                }
                estado.contains("evalu") -> {
                    chipEstado.text = "En evaluación"
                    chipEstado.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipEstado.setTextColor(ctx.getColor(R.color.chip_orange_text))
                }
                estado.contains("rech") || estado.contains("no apto") -> {
                    chipEstado.text = "No apto"
                    chipEstado.setBackgroundResource(R.drawable.bg_chip_red)
                    chipEstado.setTextColor(ctx.getColor(R.color.white))
                }
                else -> {
                    chipEstado.text = p.estado?.ifEmpty { "-" } ?: "-"
                    chipEstado.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipEstado.setTextColor(ctx.getColor(R.color.chip_orange_text))
                }
            }

            // Chip riesgo (semáforo)
            val riesgo = (p.riesgo ?: "").lowercase(Locale.getDefault())
            val pct = p.riesgoPct?.let { " ($it%)" } ?: ""
            when {
                riesgo.contains("alto") -> {
                    chipRiesgo.text = "Alto$pct"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_red)
                    chipRiesgo.setTextColor(ctx.getColor(R.color.white))
                }
                riesgo.contains("moder") -> {
                    chipRiesgo.text = "Moderado$pct"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipRiesgo.setTextColor(ctx.getColor(R.color.chip_orange_text))
                }
                riesgo.contains("bajo") -> {
                    chipRiesgo.text = "Bajo$pct"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_green)
                    chipRiesgo.setTextColor(ctx.getColor(R.color.chip_green_text))
                }
                else -> {
                    chipRiesgo.text = p.riesgo?.takeIf { it.isNotBlank() }?.plus(pct) ?: "-"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipRiesgo.setTextColor(ctx.getColor(R.color.chip_orange_text))
                }
            }

            btnDetalles.setOnClickListener { onVerDetalles(p) }
            btnEcg.setOnClickListener { onVerEcg(p) }
        }
    }
}
