package com.miyo.doctorsaludapp.presentation.view.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.miyo.doctorsaludapp.R
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
            // Nombre
            val nombre = when {
                !p.nombreCompleto.isNullOrBlank() -> p.nombreCompleto!!
                else -> listOfNotNull(p.nombres, p.apellidos).joinToString(" ").trim()
            }
            tvNombre.text = nombre

            // Otros campos
            tvDni.text = "DNI: ${p.dni.orEmpty()}"
            tvEdad.text = p.edad?.let { "Edad: $it años" } ?: "Edad: "
            tvSexo.text = "Sexo: ${p.sexo.orEmpty()}"
            tvCirugia.text = "Cirugía: ${p.tipoCirugia.orEmpty()}"
            tvFecha.text = "Fecha: ${p.fechaCirugia?.let { df.format(it) } ?: ""}"

            val ctx = itemView.context

            // Chip ESTADO (semáforo)
            val estadoKey = (p.estado ?: "").lowercase(Locale.getDefault())
            when {
                estadoKey.contains("apto") || estadoKey.contains("aproba") -> {
                    chipEstado.text = if (estadoKey.contains("aproba")) "Aprobado" else "Apto"
                    chipEstado.setBackgroundResource(R.drawable.bg_chip_green)
                    chipEstado.setTextColor(ContextCompat.getColor(ctx, R.color.chip_green_text))
                }
                estadoKey.contains("evalu") -> {
                    chipEstado.text = "En evaluación"
                    chipEstado.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipEstado.setTextColor(ContextCompat.getColor(ctx, R.color.chip_orange_text))
                }
                estadoKey.contains("rech") || estadoKey.contains("no apto") -> {
                    chipEstado.text = "No apto"
                    chipEstado.setBackgroundResource(R.drawable.bg_chip_red)
                    chipEstado.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
                else -> {
                    chipEstado.text = p.estado?.takeIf { it.isNotBlank() } ?: "-"
                    chipEstado.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipEstado.setTextColor(ContextCompat.getColor(ctx, R.color.chip_orange_text))
                }
            }

            // Chip RIESGO (semáforo)
            val riesgoKey = (p.riesgo ?: "").lowercase(Locale.getDefault())
            val pct = p.riesgoPct?.let { " ($it%)" } ?: ""
            when {
                riesgoKey.contains("alto") -> {
                    chipRiesgo.text = "Alto$pct"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_red)
                    chipRiesgo.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
                riesgoKey.contains("moder") -> {
                    chipRiesgo.text = "Moderado$pct"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipRiesgo.setTextColor(ContextCompat.getColor(ctx, R.color.chip_orange_text))
                }
                riesgoKey.contains("bajo") -> {
                    chipRiesgo.text = "Bajo$pct"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_green)
                    chipRiesgo.setTextColor(ContextCompat.getColor(ctx, R.color.chip_green_text))
                }
                else -> {
                    val label = p.riesgo?.takeIf { it.isNotBlank() }?.let { it + pct } ?: "-"
                    chipRiesgo.text = label
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipRiesgo.setTextColor(ContextCompat.getColor(ctx, R.color.chip_orange_text))
                }
            }

            btnDetalles.setOnClickListener { onVerDetalles(p) }
            btnEcg.setOnClickListener { onVerEcg(p) }
        }
    }
}
