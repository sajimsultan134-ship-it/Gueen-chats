package com.example.data.sync

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.MessageType
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class MediaUploadResult(
    val downloadUrl: String,
    val localUri: String,
    val fileName: String,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val durationSec: Int = 0
)

interface MediaStorageManager : VoiceStorageManager {
    suspend fun uploadMedia(
        chatId: String,
        messageId: String,
        sourceUri: Uri,
        mediaType: MessageType,
        onProgress: (Float) -> Unit = {}
    ): Result<MediaUploadResult>
}

class FirebaseMediaStorageManager(private val context: Context) : MediaStorageManager {
    private val TAG = "MediaStorageManager"

    private fun getStorage(): FirebaseStorage? {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                FirebaseStorage.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseStorage initialization check: ${e.message}")
            null
        }
    }

    override fun isStorageAvailable(): Boolean {
        return getStorage() != null
    }

    override suspend fun uploadVoiceNote(
        chatId: String,
        messageId: String,
        localFilePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val file = File(localFilePath)
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "Audio file is empty or not found: $localFilePath")
            return@withContext Result.success(localFilePath)
        }

        val storage = getStorage()
        if (storage == null) {
            Log.i(TAG, "Firebase Storage not configured, using local file URI: $localFilePath")
            return@withContext Result.success(localFilePath)
        }

        try {
            val storageRef = storage.reference
                .child("voice_notes")
                .child(chatId)
                .child("$messageId.m4a")

            val metadata = StorageMetadata.Builder()
                .setContentType("audio/mp4")
                .setCustomMetadata("messageId", messageId)
                .setCustomMetadata("chatId", chatId)
                .build()

            val fileUri = Uri.fromFile(file)
            storageRef.putFile(fileUri, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.i(TAG, "Voice note uploaded to Firebase Storage: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload voice note to Firebase Storage: ${e.message}. Using local file fallback.")
            Result.success(localFilePath)
        }
    }

    override suspend fun uploadMedia(
        chatId: String,
        messageId: String,
        sourceUri: Uri,
        mediaType: MessageType,
        onProgress: (Float) -> Unit
    ): Result<MediaUploadResult> = withContext(Dispatchers.IO) {
        try {
            val (rawFileName, rawSize, detectedMime) = getFileInfo(sourceUri, mediaType)
            val durationSec = if (mediaType == MessageType.VIDEO || mediaType == MessageType.VOICE) {
                extractDuration(sourceUri)
            } else 0

            // Copy to persistent local cache so the media is immediately viewable offline
            val localCachedFile = cacheFileLocally(sourceUri, chatId, messageId, rawFileName)
            val localUri = Uri.fromFile(localCachedFile).toString()
            val formattedSize = formatFileSize(if (rawSize > 0) rawSize else localCachedFile.length())

            val storage = getStorage()
            if (storage == null) {
                Log.i(TAG, "Firebase Storage not connected. Stored media locally: $localUri")
                return@withContext Result.success(
                    MediaUploadResult(
                        downloadUrl = localUri,
                        localUri = localUri,
                        fileName = rawFileName,
                        fileSizeFormatted = formattedSize,
                        fileSizeBytes = localCachedFile.length(),
                        mimeType = detectedMime,
                        durationSec = durationSec
                    )
                )
            }

            // Determine Storage folder
            val folder = when (mediaType) {
                MessageType.IMAGE -> "images"
                MessageType.VIDEO -> "videos"
                MessageType.DOCUMENT -> "documents"
                MessageType.VOICE -> "voice_notes"
                else -> "attachments"
            }

            val sanitizedName = rawFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val storageRef = storage.reference
                .child(folder)
                .child(chatId)
                .child("${messageId}_$sanitizedName")

            val metadata = StorageMetadata.Builder()
                .setContentType(detectedMime)
                .setCustomMetadata("messageId", messageId)
                .setCustomMetadata("chatId", chatId)
                .setCustomMetadata("originalName", rawFileName)
                .setCustomMetadata("type", mediaType.name)
                .build()

            val uploadTask = storageRef.putFile(Uri.fromFile(localCachedFile), metadata)

            uploadTask.addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0) {
                    val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
                    onProgress(progress.coerceIn(0f, 1f))
                }
            }

            uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.i(TAG, "Media successfully uploaded to Firebase Storage: $downloadUrl")

            Result.success(
                MediaUploadResult(
                    downloadUrl = downloadUrl,
                    localUri = localUri,
                    fileName = rawFileName,
                    fileSizeFormatted = formattedSize,
                    fileSizeBytes = localCachedFile.length(),
                    mimeType = detectedMime,
                    durationSec = durationSec
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error in uploadMedia: ${e.message}. Falling back to sourceUri.")
            Result.success(
                MediaUploadResult(
                    downloadUrl = sourceUri.toString(),
                    localUri = sourceUri.toString(),
                    fileName = "attachment_${System.currentTimeMillis()}",
                    fileSizeFormatted = "Attachment",
                    fileSizeBytes = 0L,
                    mimeType = "application/octet-stream",
                    durationSec = 0
                )
            )
        }
    }

    private fun cacheFileLocally(
        sourceUri: Uri,
        chatId: String,
        messageId: String,
        fileName: String
    ): File {
        val mediaDir = File(context.filesDir, "media/$chatId").apply { mkdirs() }
        val targetFile = File(mediaDir, "${messageId}_$fileName")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }

    private fun getFileInfo(uri: Uri, mediaType: MessageType): Triple<String, Long, String> {
        var name = when (mediaType) {
            MessageType.IMAGE -> "IMG_${System.currentTimeMillis()}.jpg"
            MessageType.VIDEO -> "VID_${System.currentTimeMillis()}.mp4"
            MessageType.DOCUMENT -> "DOC_${System.currentTimeMillis()}.pdf"
            MessageType.VOICE -> "AUD_${System.currentTimeMillis()}.m4a"
            else -> "FILE_${System.currentTimeMillis()}"
        }
        var size = 0L
        val type = context.contentResolver.getType(uri) ?: when (mediaType) {
            MessageType.IMAGE -> "image/jpeg"
            MessageType.VIDEO -> "video/mp4"
            MessageType.DOCUMENT -> "application/pdf"
            MessageType.VOICE -> "audio/mp4"
            else -> "application/octet-stream"
        }

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)?.let { if (it.isNotBlank()) name = it }
                    }
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        return Triple(name, size, type)
    }

    private fun extractDuration(uri: Uri): Int {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            val durationMs = time?.toLongOrNull() ?: 0L
            (durationMs / 1000).toInt()
        } catch (_: Exception) {
            0
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown size"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
