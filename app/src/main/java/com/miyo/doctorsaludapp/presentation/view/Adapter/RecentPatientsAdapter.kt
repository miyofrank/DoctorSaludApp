package com.miyo.doctorsaludapp.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miyo.doctorsaludapp.databinding.ItemPatientRecentBinding
import com.miyo.doctorsaludapp.domain.model.Patient

class RecentPatientsAdapter(
    private val onClick: (Patient) -> Unit
) : RecyclerView.Adapter<RecentPatientsAdapter.VH>() {

    private val items = mutableListOf<Patient>()

    fun submit(list: List<Patient>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemPatientRecentBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemPatientRecentBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val nombre = when {
            !p.nombreCompleto.isNullOrBlank() -> p.nombreCompleto!!
            else -> listOfNotNull(p.nombres, p.apellidos).joinToString(" ").trim()
        }
        holder.b.tvNombre.text = nombre.ifBlank { "Paciente" }
        holder.b.tvLinea1.text = buildString {
            append(p.edad?.let { "$it años" } ?: "—")
            append(" • DNI: ")
            append(p.dni ?: "—")
        }
        holder.b.tvLinea2.text = p.tipoCirugia ?: "—"
        holder.b.chipEstado.text = "En evaluación"  // si tienes un campo estado, reemplázalo

        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount(): Int = items.size
}
