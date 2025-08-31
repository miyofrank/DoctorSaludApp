package com.miyo.doctorsaludapp.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.InputStream

class ReportaDiagnosticoPreoperatorioUseCase(private val context: Context) {
    private val interpreter: Interpreter

    init {
        val model = FileUtil.loadMappedFile(context, "ecg_model.tflite")
        interpreter = Interpreter(model)
    }

    fun processImage(uri: Uri, callback: (String) -> Unit) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        // Asegurarse de redimensionar la imagen a 224x224
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val result = classifyImage(resizedBitmap)
        callback(result)
    }

    private fun classifyImage(bitmap: Bitmap): String {
        // Convertir el Bitmap a TensorImage y luego a TensorBuffer
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

        // Obtener el ByteBuffer de TensorImage
        val byteBuffer = tensorImage.buffer
        val normalizedPixels = FloatArray(224 * 224 * 3)

        // Normalizar los valores de los píxeles
        byteBuffer.rewind()
        for (i in normalizedPixels.indices) {
            val pixelValue = byteBuffer.get().toInt() and 0xFF
            normalizedPixels[i] = pixelValue / 255.0f
        }

        inputBuffer.loadArray(normalizedPixels)

        // Crear el buffer de salida
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1), DataType.FLOAT32)
        interpreter.run(inputBuffer.buffer.rewind(), outputBuffer.buffer.rewind())

        // Obtener el valor de salida
        val outputValue = outputBuffer.floatArray[0]
        return if (outputValue > 0.5) "No apto para cirugía" else "Apto para cirugía"
    }
}


