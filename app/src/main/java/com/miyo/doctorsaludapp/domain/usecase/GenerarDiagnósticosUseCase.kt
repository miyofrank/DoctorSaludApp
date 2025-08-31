package com.miyo.doctorsaludapp.domain.usecase

import android.content.Context
import android.util.Log
import com.miyo.doctorsaludapp.domain.model.Paciente
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class GenerarDiagnosticosUseCase(private val context: Context) {

    fun ejecutar(paciente: Paciente, dolorPecho: Int, presionArterial: Float, colesterol: Float, azucarEnSangre: Int, restEcg: Int, frecuenciaCardiaca: Float, anginaEjercicio: Int, oldpeak: Float, pendiente: Int, vasosColoreados: Int, talasemia: Int): String {
        val model = loadModel()

        val inputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 13), DataType.FLOAT32)
        inputTensor.loadArray(preprocessInput(
            paciente.edad,
            paciente.genero,
            dolorPecho,
            presionArterial,
            colesterol,
            azucarEnSangre,
            restEcg,
            frecuenciaCardiaca,
            anginaEjercicio,
            oldpeak,
            pendiente,
            vasosColoreados,
            talasemia
        ))

        val outputTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1), DataType.FLOAT32)

        val tflite = Interpreter(model)
        tflite.run(inputTensor.buffer, outputTensor.buffer)

        val prediction = outputTensor.floatArray[0]
        Log.d("GenerarDiagnosticosUseCase", "Predicción del modelo: $prediction")
        return if (prediction < 0.5) "Apto, todo conforme" else "No Apto, necesita revisión médica"
    }

    private fun preprocessInput(edad: Int, sexo: String, dolorPecho: Int, presionArterial: Float, colesterol: Float, azucarEnSangre: Int, restEcg: Int, frecuenciaCardiaca: Float, anginaEjercicio: Int, oldpeak: Float, pendiente: Int, vasosColoreados: Int, talasemia: Int): FloatArray {
        return floatArrayOf(
            edad.toFloat(),
            if (sexo == "Male") 1.0f else 0.0f,
            dolorPecho.toFloat(),
            presionArterial,
            colesterol,
            azucarEnSangre.toFloat(),
            restEcg.toFloat(),
            frecuenciaCardiaca,
            anginaEjercicio.toFloat(),
            oldpeak,
            pendiente.toFloat(),
            vasosColoreados.toFloat(),
            talasemia.toFloat()
        )
    }

    private fun loadModel(): MappedByteBuffer {
        Log.d("GenerarDiagnosticosUseCase", "Cargando modelo tflite...")
        val assetFileDescriptor = context.assets.openFd("heart_disease_model.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}



