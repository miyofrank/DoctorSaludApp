package com.miyo.doctorsaludapp.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.miyo.doctorsaludapp.data.firestore.PatientRepository
import com.miyo.doctorsaludapp.domain.model.EcgAnalysis
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.usecase.AnalyzePatientEcgUseCase
import com.miyo.doctorsaludapp.domain.usecase.UploadEcgForPatientUseCase
import kotlinx.coroutines.launch

data class AnalysisUi(
    val patient: Patient? = null,
    val imageUri: Uri? = null,
    val imageMime: String? = null,
    val uploading: Boolean = false,
    val uploaded: Boolean = false,
    val analyzing: Boolean = false,
    val analysis: EcgAnalysis? = null,
    val error: String? = null
)

class AnalysisViewModel(
    app: Application,
    private val patients: PatientRepository = PatientRepository(),
    private val uploadEcg: UploadEcgForPatientUseCase = UploadEcgForPatientUseCase(),
    private val analyzeEcg: AnalyzePatientEcgUseCase = AnalyzePatientEcgUseCase()
) : AndroidViewModel(app) {

    private val _ui = MutableLiveData(AnalysisUi())
    val ui: LiveData<AnalysisUi> = _ui

    fun loadPatient(patientId: String) {
        viewModelScope.launch {
            val p = patients.get(patientId)
            _ui.postValue(_ui.value?.copy(patient = p))
        }
    }

    fun setLocalImage(uri: Uri, mime: String) {
        _ui.postValue(_ui.value?.copy(imageUri = uri, imageMime = mime, uploaded = false, analysis = null))
    }

    fun upload(patientId: String) {
        val uri = _ui.value?.imageUri
        val mime = _ui.value?.imageMime ?: "image/png"
        if (uri == null) {
            _ui.postValue(_ui.value?.copy(error = "Selecciona o toma una imagen primero."))
            return
        }
        _ui.postValue(_ui.value?.copy(uploading = true, error = null))
        viewModelScope.launch {
            try {
                uploadEcg(patientId, uri, mime)
                val p = patients.get(patientId)
                _ui.postValue(_ui.value?.copy(uploading = false, uploaded = true, patient = p))
            } catch (e: Exception) {
                _ui.postValue(_ui.value?.copy(uploading = false, error = e.message ?: "Error al subir"))
            }
        }
    }

    fun analyze(patientId: String) {
        _ui.postValue(_ui.value?.copy(analyzing = true, error = null))
        viewModelScope.launch {
            try {
                val a = analyzeEcg(getApplication(), patientId)
                val p = patients.get(patientId)
                _ui.postValue(_ui.value?.copy(analyzing = false, analysis = a, patient = p))
            } catch (e: Exception) {
                _ui.postValue(_ui.value?.copy(analyzing = false, error = e.message ?: "Error al analizar"))
            }
        }
    }
}