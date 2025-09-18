package com.miyo.doctorsaludapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.data.repository.StatsRepository
import com.miyo.doctorsaludapp.domain.model.Patient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

data class HomeUiState(
    val loading: Boolean = true,
    val doctorName: String = "Doctor",
    val totalPacientes: Int = 0,
    val iaPrecisionPct: Double? = null,
    val iaAvgSeconds: Double? = null,
    val iaSavedMinutes: Double? = null,
    val riskBajo: Int = 0,
    val riskModerado: Int = 0,
    val riskAlto: Int = 0,
    val recientes: List<Patient> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val statsRepo: StatsRepository = StatsRepository(FirebaseFirestore.getInstance(), "pacientes"),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)

        viewModelScope.launch {
            try {
                val doctor = fetchDoctorName()

                val stats = statsRepo.fetchStats(lastMonths = 12)

                val pacSnaps = db.collection("pacientes")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                val recientes = pacSnaps.documents.mapNotNull { d ->
                    val p = Patient()
                    p.id = d.id
                    p.createdAt = (d.getTimestamp("createdAt")?.toDate() ?: d.getDate("createdAt") ?: Date())
                    p.updatedAt = (d.getTimestamp("updatedAt")?.toDate() ?: d.getDate("updatedAt"))
                    p.dni = d.getString("dni")
                    p.nombres = d.getString("nombres")
                    p.apellidos = d.getString("apellidos")
                    p.nombreCompleto = d.getString("nombreCompleto")
                    p.edad = (d.getLong("edad")?.toInt())
                    p.sexo = d.getString("sexo")
                    p.tipoCirugia = d.getString("tipoCirugia")
                    p
                }

                _state.value = HomeUiState(
                    loading = false,
                    doctorName = doctor,
                    totalPacientes = stats.totalPacientes,
                    iaPrecisionPct = stats.avgPrecisionGlobal,
                    iaAvgSeconds = stats.avgIaSeconds,
                    iaSavedMinutes = stats.savedMinutes,
                    riskBajo = stats.risk.bajo,
                    riskModerado = stats.risk.moderado,
                    riskAlto = stats.risk.alto,
                    recientes = recientes
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    /** Obtiene un nombre legible desde Auth y/o Firestore. Nunca devuelve null. */
    private suspend fun fetchDoctorName(): String {
        val user = auth.currentUser
        if (user == null) return "Doctor"

        // 1) Preferir displayName de Auth
        val authName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.providerData.firstOrNull { !it.displayName.isNullOrBlank() }?.displayName

        // 2) Intentar Firestore users/{uid}
        val fsName: String? = try {
            val snap = db.collection("usuarios").document(user.uid).get().await()
            if (snap.exists()) {
                val displayName = snap.getString("displayName")
                val name = snap.getString("name")
                val nombre = snap.getString("nombre")
                val nombres = snap.getString("nombres")
                val apellidos = snap.getString("apellidos")
                val first = snap.getString("firstName")
                val last  = snap.getString("lastName")

                when {
                    !displayName.isNullOrBlank() -> displayName
                    !name.isNullOrBlank()        -> name
                    !nombre.isNullOrBlank()      -> nombre
                    !nombres.isNullOrBlank() && !apellidos.isNullOrBlank() ->
                        "$nombres $apellidos"
                    !first.isNullOrBlank() && !last.isNullOrBlank() ->
                        "$first $last"
                    else -> null
                }
            } else null
        } catch (_: Exception) { null }

        // 3) Fallback a email local-part
        val emailName = user.email?.substringBefore('@')
            ?.replace('.', ' ')
            ?.replace('_', ' ')
            ?.replace('-', ' ')
            ?.trim()
            ?.split(' ')
            ?.joinToString(" ") { it.lowercase(Locale.getDefault()).replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }

        // Devolver el primero que esté disponible
        return authName ?: fsName ?: emailName ?: "Doctor"
    }
}

