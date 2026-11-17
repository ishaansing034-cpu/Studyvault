package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.model.AttachmentType
import com.example.model.NoteAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FileAttachmentManager(private val context: Context) {

    private fun attachmentDir(userId: String): File {
        val dir = File(context.filesDir, "attachments/$userId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun savePickedFile(
        uri: Uri,
        userId: String,
        type: AttachmentType
    ): Result<NoteAttachment> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val displayName = resolveDisplayName(uri) ?: if (type == AttachmentType.PDF) {
                "Study_Document_${System.currentTimeMillis()}.pdf"
            } else {
                "Handwritten_Note_${System.currentTimeMillis()}.jpg"
            }

            val ext = when (type) {
                AttachmentType.PDF -> "pdf"
                AttachmentType.IMAGE -> displayName.substringAfterLast('.', "jpg")
                    .lowercase()
                    .takeIf { it in listOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
            }

            val id = UUID.randomUUID().toString().replace("-", "").take(16)
            val targetFile = File(attachmentDir(userId), "${id}.$ext")

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file."))

            Result.success(
                NoteAttachment(
                    id = id,
                    name = displayName.take(120),
                    type = type.name,
                    localPath = targetFile.absolutePath,
                    sizeBytes = targetFile.length()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLocalFile(localPath: String) = withContext(Dispatchers.IO) {
        try {
            if (localPath.isNotBlank()) {
                val file = File(localPath)
                if (file.exists()) file.delete()
            }
        } catch (_: Exception) {
            // Non-fatal cleanup error
        }
    }

    suspend fun decodeSampledBitmap(localPath: String, maxDim: Int = 1280): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(localPath)
                if (!file.exists()) return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeFile(file.absolutePath, opts)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun readBytesBounded(localPath: String, maxBytes: Int = 12 * 1024 * 1024): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(localPath)
                if (!file.exists() || file.length() > maxBytes) return@withContext null
                file.readBytes()
            } catch (_: Exception) {
                null
            }
        }

    suspend fun renderPdfPage(
        localPath: String,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Pair<Bitmap, Int>? = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            val file = File(localPath)
            if (!file.exists()) return@withContext null
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount == 0) return@withContext null
            val safeIndex = pageIndex.coerceIn(0, pageCount - 1)
            renderer.openPage(safeIndex).use { page ->
                val aspect = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
                val height = (targetWidth * aspect).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap to pageCount
            }
        } catch (_: Exception) {
            null
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }
}
