package com.example.data.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File

interface VoiceStorageManager {
    suspend fun uploadVoiceNote(chatId: String, messageId: String, localFilePath: String): Result<String>
    fun isStorageAvailable(): Boolean
}

class FirebaseVoiceStorageManager(private val context: Context) : VoiceStorageManager {
    private val TAG = "VoiceStorageManager"

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
    ): Result<String> {
        val file = File(localFilePath)
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "Audio file is empty or not found: $localFilePath")
            return Result.success(localFilePath)
        }

        val storage = getStorage()
        if (storage == null) {
            Log.i(TAG, "Firebase Storage not configured, using local file URI: $localFilePath")
            return Result.success(localFilePath)
        }

        return try {
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
            val uploadTask = storageRef.putFile(fileUri, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.i(TAG, "Voice note uploaded to Firebase Storage: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to upload voice note to Firebase Storage: ${e.message}. Using local file fallback.")
            Result.success(localFilePath)
        }
    }
}
