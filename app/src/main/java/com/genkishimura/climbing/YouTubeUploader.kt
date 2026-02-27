package com.genkishimura.climbing

import android.accounts.Account
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object YouTubeUploader {
    private const val YOUTUBE_UPLOAD_SCOPE = "oauth2:https://www.googleapis.com/auth/youtube.upload"
    private const val ENDPOINT =
        "https://www.googleapis.com/upload/youtube/v3/videos?part=snippet,status&uploadType=multipart"

    private val client = OkHttpClient()

    fun uploadDocumentFile(
        context: Context,
        file: DocumentFile,
        accountName: String,
    ): Result<String> {
        val sourceUri = file.uri
        val tempFile = copyToTempFile(context, file)
            ?: return Result.failure(IllegalStateException("動画ファイルを読み込めませんでした"))

        return try {
            val account = Account(accountName, "com.google")
            val token = GoogleAuthUtil.getToken(context, account, YOUTUBE_UPLOAD_SCOPE)
            val metadataJson = buildMetadata(file)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType()),
                )
                .addFormDataPart(
                    "media",
                    file.name ?: "climbing_video.mp4",
                    tempFile.asRequestBody((file.type ?: "video/mp4").toMediaTypeOrNull()),
                )
                .build()

            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(
                        IOException("YouTube upload failed: ${response.code} $bodyText"),
                    )
                }
                val videoId = JSONObject(bodyText).optString("id")
                if (videoId.isBlank()) {
                    return Result.failure(IllegalStateException("動画IDが取得できませんでした"))
                }
                AppPrefs.markUploaded(context, sourceUri.toString())
                Result.success(videoId)
            }
        } catch (e: GoogleAuthException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } finally {
            tempFile.delete()
        }
    }

    private fun copyToTempFile(context: Context, file: DocumentFile): File? {
        val temp = File.createTempFile("upload_", ".tmp", context.cacheDir)
        context.contentResolver.openInputStream(file.uri)?.use { input ->
            temp.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        return temp
    }

    private fun buildMetadata(file: DocumentFile): String {
        val titleSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val title = file.name ?: "climbing_$titleSuffix"
        val snippet = JSONObject()
            .put("title", title)
            .put("description", "Uploaded by Climbing app")
        val status = JSONObject().put("privacyStatus", "unlisted")
        return JSONObject()
            .put("snippet", snippet)
            .put("status", status)
            .toString()
    }
}
