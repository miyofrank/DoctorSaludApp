package com.miyo.doctorsaludapp.domain.usecase

class GenerarRecomendacionesUseCase {
    fun generarRecomendaciones(
        presionArterial: Float,
        colesterol: Float,
        azucarEnSangre: Int,
        frecuenciaCardiaca: Float,
        oldpeak: Float
    ): List<String> {
        val recomendaciones = mutableListOf<String>()

        if (presionArterial > 120) {
            recomendaciones.add("Reducir el consumo de sal y realizar ejercicios aeróbicos regularmente.")
        }
        if (colesterol > 200) {
            recomendaciones.add("Evitar alimentos ricos en grasas saturadas y trans. Incluir más frutas, verduras y granos enteros en la dieta.")
        }
        if (azucarEnSangre > 1) {
            recomendaciones.add("Controlar la ingesta de carbohidratos y realizar controles regulares de glucosa en sangre.")
        }
        if (frecuenciaCardiaca > 100) {
            recomendaciones.add("Practicar técnicas de relajación y evitar el estrés.")
        }
        if (oldpeak > 1.0) {
            recomendaciones.add("Consultar a un cardiólogo para una evaluación más detallada.")
        }
        // Si no hay recomendaciones, agregar un mensaje positivo
        if (recomendaciones.isEmpty()) {
            recomendaciones.add("¡Enhorabuena! No tienes problemas de salud detectados.")
        }
        return recomendaciones
    }
}


