package com.miyo.doctorsaludapp.presentation.view.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.domain.model.Patient
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Muestra el RIESGO tomado **exclusivamente** del ECG más reciente en la
 * subcolección `pacientes/{id}/ecgs`. NO usa el campo riesgo del documento paciente.
 *
 * - Consulta Firestore al bind de cada item y guarda resultado en un cache en memoria.
 * - Si hay "nivelRiesgo" a nivel raíz del doc ECG lo usa; si no, intenta en "analysis.nivelRiesgo".
 * - Lo mismo para "riesgoPct".
 */
class PacienteAdapter(
    private val onVerDetalles: (Patient) -> Unit,
    private val onVerEcg: (Patient) -> Unit
) : RecyclerView.Adapter<PacienteAdapter.VH>() {

    private val data = mutableListOf<Patient>()
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val pctDf = DecimalFormat("#.##")
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // Cache simple en memoria: patientId -> (riesgo, pct)
    private val riesgoCache = mutableMapOf<String, Pair<String?, Double?>>()

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
        val p = data[position]
        holder.bindStatic(p, df) // datos que no dependen de la consulta
        holder.bindEstado(p)     // pinta el estado

        // --- RIESGO: siempre desde ECG más reciente ---
        val pid = p.id
        if (pid.isNullOrBlank()) {
            holder.paintRiesgo("-", null)
            holder.setButtons(p, onVerDetalles, onVerEcg)
            return
        }

        // Para evitar resultados cruzados por reciclaje:
        holder.currentPatientId = pid
        holder.paintRiesgo("—", null) // placeholder mientras carga

        val cached = riesgoCache[pid]
        if (cached != null) {
            holder.paintRiesgo(cached.first, cached.second)
        } else {
            firestore.collection("pacientes")
                .document(pid)
                .collection("ecgs")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->
                    val doc = snap.documents.firstOrNull()
                    val riesgoTop = doc?.getString("nivelRiesgo")
                        ?: doc?.getString("riesgo")

                    val analysis = doc?.get("analysis") as? Map<*, *>
                    val riesgoFromAnalysis = (analysis?.get("nivelRiesgo") as? String)
                        ?: (analysis?.get("riesgo") as? String)

                    val riesgoFinal = riesgoTop ?: riesgoFromAnalysis
                    val pctTop = (doc?.get("riesgoPct") as? Number)?.toDouble()
                        ?: (analysis?.get("riesgoPct") as? Number)?.toDouble()

                    // Cachear
                    riesgoCache[pid] = Pair(riesgoFinal, pctTop)

                    // Solo pintar si el holder sigue mostrando este paciente
                    if (holder.currentPatientId == pid) {
                        holder.paintRiesgo(riesgoFinal, pctTop)
                    }
                }
                .addOnFailureListener {
                    // Si falla, muestra guión y no cachea
                    if (holder.currentPatientId == pid) {
                        holder.paintRiesgo("-", null)
                    }
                }
        }

        holder.setButtons(p, onVerDetalles, onVerEcg)
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

        // usado para evitar pintar respuestas fuera de tiempo por reciclaje
        var currentPatientId: String? = null

        fun bindStatic(p: Patient, df: SimpleDateFormat) {
            val nombre = when {
                !p.nombreCompleto.isNullOrBlank() -> p.nombreCompleto!!
                else -> listOfNotNull(p.nombres, p.apellidos).joinToString(" ").trim()
            }
            tvNombre.text = nombre

            tvDni.text = "DNI: ${p.dni.orEmpty()}"
            tvEdad.text = p.edad?.let { "Edad: $it años" } ?: "Edad: "
            tvSexo.text = "Sexo: ${p.sexo.orEmpty()}"
            tvCirugia.text = "Cirugía: ${p.tipoCirugia.orEmpty()}"
            tvFecha.text = "Fecha: ${p.fechaCirugia?.let { df.format(it) } ?: ""}"
        }

        fun bindEstado(p: Patient) {
            val ctx = itemView.context
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
        }

        fun paintRiesgo(riesgo: String?, pct: Double?) {
            val ctx = itemView.context
            val key = (riesgo ?: "").lowercase(Locale.getDefault())
            val pctLabel = pct?.let { " (${DecimalFormat("#.##").format(it)}%)" } ?: ""

            when {
                key.contains("alto") -> {
                    chipRiesgo.text = "Alto$pctLabel"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_red)
                    chipRiesgo.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
                key.contains("moder") -> {
                    chipRiesgo.text = "Moderado$pctLabel"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipRiesgo.setTextColor(ContextCompat.getColor(ctx, R.color.chip_orange_text))
                }
                key.contains("bajo") -> {
                    chipRiesgo.text = "Bajo$pctLabel"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_green)
                    chipRiesgo.setTextColor(ContextCompat.getColor(ctx, R.color.chip_green_text))
                }
                else -> {
                    chipRiesgo.text = if (!riesgo.isNullOrBlank()) riesgo + pctLabel else "-"
                    chipRiesgo.setBackgroundResource(R.drawable.bg_chip_orange)
                    chipRiesgo.setTextColor(ContextCompat.getColor(ctx, R.color.chip_orange_text))
                }
            }
        }

        fun setButtons(p: Patient, onVerDetalles: (Patient) -> Unit, onVerEcg: (Patient) -> Unit) {
            btnDetalles.setOnClickListener { onVerDetalles(p) }
            btnEcg.setOnClickListener { onVerEcg(p) }
        }
    }
}
