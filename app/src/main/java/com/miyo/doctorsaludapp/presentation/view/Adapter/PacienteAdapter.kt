package com.miyo.doctorsaludapp.presentation.view.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.databinding.ItemPacienteBinding
import com.miyo.doctorsaludapp.domain.model.Paciente
import com.miyo.doctorsaludapp.presentation.viewmodel.PacientesViewModel

class PacienteAdapter(private var pacientes: List<Paciente>, private val viewModel: PacientesViewModel) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    inner class PacienteViewHolder(private val binding: ItemPacienteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(paciente: Paciente) {
            binding.tvNombre.text = paciente.nombre
            binding.tvApellidos.text = paciente.apellidos
            binding.tvCorreo.text = paciente.correo
            binding.tvDireccion.text = paciente.direccion
            binding.tvEdad.text = paciente.edad.toString()
            binding.tvFecha.text = paciente.fecha
            binding.tvGenero.text = paciente.genero
            binding.tvTelefono.text = paciente.telefono.toString()
            Glide.with(binding.ivFoto.context)
                .load(paciente.fotoUrl)
                .apply(RequestOptions.placeholderOf(com.facebook.R.drawable.com_facebook_profile_picture_blank_square).error(
                    com.facebook.R.drawable.com_facebook_profile_picture_blank_square))
                .into(binding.ivFoto)

            binding.btnDelete.setOnClickListener {
                viewModel.eliminarPaciente(paciente.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val binding = ItemPacienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PacienteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        holder.bind(pacientes[position])
    }

    override fun getItemCount(): Int = pacientes.size

    fun updatePacientes(pacientes: List<Paciente>) {
        this.pacientes = pacientes
        notifyDataSetChanged()
    }
}
