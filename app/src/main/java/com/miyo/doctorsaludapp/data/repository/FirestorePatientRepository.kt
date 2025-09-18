package com.miyo.doctorsaludapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.miyo.doctorsaludapp.domain.model.Patient
import com.miyo.doctorsaludapp.domain.repository.PatientRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestorePatientRepository(
    private val db: FirebaseFirestore,
    private val collection: String = "pacientes"
) : PatientRepository {

    override fun getPatientsStream(): Flow<List<Patient>> = callbackFlow {
        val reg = db.collection(collection)
            .orderBy("nombreCompleto", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { d -> d.data?.toPatient(d.id) }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun getPatientById(id: String): Patient? {
        val doc = db.collection(collection).document(id).get().await()
        return doc.data?.toPatient(doc.id)
    }

    override suspend fun addPatient(patient: Patient): String {
        val now = Date()
        val withMeta = patient.copy(
            createdAt = patient.createdAt ?: now,
            updatedAt = now,
            nombreCompleto = patient.nombreCompleto
                ?: listOfNotNull(patient.nombres, patient.apellidos).joinToString(" ").trim()
        ).toMap()
        val ref = db.collection(collection).add(withMeta).await()
        return ref.id
    }

    override suspend fun setPatient(id: String, patient: Patient) {
        val withMeta = patient.copy(
            updatedAt = Date(),
            nombreCompleto = patient.nombreCompleto
                ?: listOfNotNull(patient.nombres, patient.apellidos).joinToString(" ").trim()
        ).toMap()
        db.collection(collection).document(id).set(withMeta).await()
    }

    override suspend fun updatePatient(patient: Patient) {
        require(!patient.id.isNullOrEmpty()) { "patient.id requerido" }
        db.collection(collection).document(patient.id!!).update(patient.copy(updatedAt = Date()).toMap()).await()
    }

    override suspend fun deletePatient(id: String) {
        db.collection(collection).document(id).delete().await()
    }

    // ---- helpers ----
    private fun Map<String, Any?>.toPatient(id: String): Patient? = try {
        fun ts(key: String) = (this[key] as? Timestamp)?.toDate()
        fun intOrNull(key: String): Int? = when (val v = this[key]) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
        fun strList(key: String): List<String>? = when (val v = this[key]) {
            is List<*> -> v.filterIsInstance<String>()
            is String -> v.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> null
        }

        Patient(
            id = id,
            createdAt = ts("createdAt"),
            updatedAt = ts("updatedAt"),
            dni = this["dni"] as? String,
            nombres = this["nombres"] as? String,
            apellidos = this["apellidos"] as? String,
            nombreCompleto = (this["nombreCompleto"] ?: this["nombre"]) as? String,
            edad = intOrNull("edad"),
            sexo = this["sexo"] as? String,
            grupoSanguineo = this["grupoSanguineo"] as? String,
            alturaCm = intOrNull("alturaCm"),
            pesoKg = intOrNull("pesoKg"),
            alergias = strList("alergias"),
            medicamentosActuales = strList("medicamentosActuales"),
            enfermedadesCronicas = strList("enfermedadesCronicas"),
            cirugiasPrevias = strList("cirugiasPrevias"),
            antecedentesFamiliares = strList("antecedentesFamiliares"),
            tipoCirugia = this["tipoCirugia"] as? String,
            fechaCirugia = ts("fechaCirugia"),
            duracionEstimadaMin = intOrNull("duracionEstimadaMin"),
            tipoAnestesia = this["tipoAnestesia"] as? String,
            urgencia = this["urgencia"] as? String,
            cirujano = this["cirujano"] as? String,
            ecgUrl = this["ecgUrl"] as? String,
            ecgId = this["ecgId"] as? String,
            ecgMime = this["ecgMime"] as? String,
            examenesTexto = this["examenesTexto"] as? String,
            examenesArchivos = strList("examenesArchivos"),
            notas = this["notas"] as? String,
            estado = this["estado"] as? String,
            riesgo = this["riesgo"] as? String,
            riesgoPct = intOrNull("riesgoPct")
        )
    } catch (_: Throwable) { null }

    private fun Patient.toMap(): Map<String, Any?> = hashMapOf(
        "createdAt" to (this.createdAt?.let { Timestamp(it) }),
        "updatedAt" to (this.updatedAt?.let { Timestamp(it) }),

        "dni" to this.dni,
        "nombres" to this.nombres,
        "apellidos" to this.apellidos,
        "nombreCompleto" to (this.nombreCompleto ?: listOfNotNull(nombres, apellidos).joinToString(" ").trim()),
        "edad" to this.edad,
        "sexo" to this.sexo,
        "grupoSanguineo" to this.grupoSanguineo,
        "alturaCm" to this.alturaCm,
        "pesoKg" to this.pesoKg,

        "alergias" to this.alergias,
        "medicamentosActuales" to this.medicamentosActuales,
        "enfermedadesCronicas" to this.enfermedadesCronicas,
        "cirugiasPrevias" to this.cirugiasPrevias,
        "antecedentesFamiliares" to this.antecedentesFamiliares,

        "tipoCirugia" to this.tipoCirugia,
        "fechaCirugia" to (this.fechaCirugia?.let { Timestamp(it) }),
        "duracionEstimadaMin" to this.duracionEstimadaMin,
        "tipoAnestesia" to this.tipoAnestesia,
        "urgencia" to this.urgencia,
        "cirujano" to this.cirujano,

        "ecgUrl" to this.ecgUrl,
        "ecgId" to this.ecgId,
        "ecgMime" to this.ecgMime,

        "examenesTexto" to this.examenesTexto,
        "examenesArchivos" to this.examenesArchivos,

        "notas" to this.notas,
        "estado" to this.estado,
        "riesgo" to this.riesgo,
        "riesgoPct" to this.riesgoPct
    )
}
