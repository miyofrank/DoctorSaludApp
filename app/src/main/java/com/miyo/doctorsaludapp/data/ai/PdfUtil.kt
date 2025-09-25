package com.miyo.doctorsaludapp.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfUtil {

    suspend fun renderFirstPage(context: Context, bytes: ByteArray): Bitmap? =
        withContext(Dispatchers.IO) {
            // Guardar en cache y renderizar
            val tmp = File.createTempFile("ecg_", ".pdf", context.cacheDir)
            tmp.outputStream().use { it.write(bytes) }

            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                pfd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                if (renderer.pageCount <= 0) return@withContext null
                renderer.openPage(0).use { page ->
                    val w = (page.width * 1.5f).toInt()
                    val h = (page.height * 1.5f).toInt()
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return@withContext bmp
                }
            } finally {
                renderer?.close()
                pfd?.close()
                tmp.delete()
            }
        }
}
