package com.miyo.doctorsaludapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.data.repository.CollectionGroupIndexRequiredException
import com.miyo.doctorsaludapp.data.repository.StatsRepository
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.model.stats.defaultStatsFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

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
                // Nombre del doctor
                val uid = auth.currentUser?.uid
                val doctor = if (uid != null) {
                    val snap = db.collection("users").document(uid).get().await()
                    snap.getString("displayName")
                        ?: snap.getString("name")
                        ?: snap.getString("nombre")
                        ?: "Doctor"
                } else "Doctor"

                // Stats últimos 12 meses
                val filters = defaultStatsFilters(12)
                val stats = statsRepo.fetch(filters)

                // Recientes
                val pacSnaps = db.collection("pacientes")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()
                val recientes = pacSnaps.documents.map { d ->
                    Patient().apply {
                        id = d.id
                        createdAt = (d.getTimestamp("createdAt")?.toDate() ?: d.getDate("createdAt") ?: Date())
                        updatedAt = (d.getTimestamp("updatedAt")?.toDate() ?: d.getDate("updatedAt"))
                        dni = d.getString("dni")
                        nombres = d.getString("nombres")
                        apellidos = d.getString("apellidos")
                        nombreCompleto = d.getString("nombreCompleto")
                        edad = (d.getLong("edad")?.toInt())
                        sexo = d.getString("sexo")
                        tipoCirugia = d.getString("tipoCirugia")
                    }
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
                    recientes = recientes,
                    error = null
                )
            } catch (e: CollectionGroupIndexRequiredException) {
                _state.value = _state.value.copy(
                    loading = false,
                    totalPacientes = e.partial.totalPacientes,
                    iaPrecisionPct = null,
                    iaAvgSeconds = null,
                    iaSavedMinutes = null,
                    riskBajo = 0, riskModerado = 0, riskAlto = 0,
                    error = e.message
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }
}
