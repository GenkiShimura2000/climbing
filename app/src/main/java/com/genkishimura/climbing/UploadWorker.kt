package com.genkishimura.climbing

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

class UploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val folderUri = AppPrefs.getFolderUri(applicationContext)
            ?: return failure("対象フォルダが未設定です")
        val accountName = AppPrefs.getYouTubeAccountName(applicationContext)
            ?: return failure("YouTube連携が未設定です")

        val root = DocumentFile.fromTreeUri(applicationContext, folderUri)
            ?: return failure("対象フォルダにアクセスできません")

        val uploadedSet = AppPrefs.getUploadedUriSet(applicationContext)
        val videoFiles = root.listFiles()
            .filter { it.isFile && isVideoFile(it) && !uploadedSet.contains(it.uri.toString()) }

        if (videoFiles.isEmpty()) {
            return success("未アップロード動画はありません")
        }

        var uploadedCount = 0
        for (file in videoFiles) {
            val result = YouTubeUploader.uploadDocumentFile(
                context = applicationContext,
                file = file,
                accountName = accountName,
            )
            if (result.isSuccess) {
                uploadedCount += 1
            } else {
                val error = result.exceptionOrNull()?.message ?: "unknown error"
                return failure("アップロード失敗: ${file.name} ($error)")
            }
        }

        return success("アップロード完了: ${uploadedCount}件")
    }

    private fun isVideoFile(file: DocumentFile): Boolean {
        val type = file.type.orEmpty().lowercase()
        if (type.startsWith("video/")) return true
        val name = file.name.orEmpty().lowercase()
        return name.endsWith(".mp4") || name.endsWith(".mov") || name.endsWith(".m4v")
    }

    private fun success(message: String): Result {
        return Result.success(Data.Builder().putString(KEY_MESSAGE, message).build())
    }

    private fun failure(message: String): Result {
        return Result.failure(Data.Builder().putString(KEY_MESSAGE, message).build())
    }

    companion object {
        const val KEY_MESSAGE = "key_message"
    }
}

