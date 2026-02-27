package com.genkishimura.climbing

import android.content.Context
import android.net.Uri

object AppPrefs {
    private const val PREF_NAME = "climbing_prefs"
    private const val KEY_FOLDER_URI = "key_folder_uri"
    private const val KEY_YOUTUBE_ACCOUNT = "key_youtube_account"
    private const val KEY_UPLOADED_URIS = "key_uploaded_uris"

    fun getFolderUri(context: Context): Uri? {
        val uriText = prefs(context).getString(KEY_FOLDER_URI, null) ?: return null
        return Uri.parse(uriText)
    }

    fun setFolderUri(context: Context, uri: Uri) {
        prefs(context).edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
    }

    fun getYouTubeAccountName(context: Context): String? {
        return prefs(context).getString(KEY_YOUTUBE_ACCOUNT, null)
    }

    fun setYouTubeAccountName(context: Context, accountName: String) {
        prefs(context).edit().putString(KEY_YOUTUBE_ACCOUNT, accountName).apply()
    }

    fun clearYouTubeAccountName(context: Context) {
        prefs(context).edit().remove(KEY_YOUTUBE_ACCOUNT).apply()
    }

    fun getUploadedUriSet(context: Context): MutableSet<String> {
        return prefs(context).getStringSet(KEY_UPLOADED_URIS, emptySet())?.toMutableSet()
            ?: mutableSetOf()
    }

    fun markUploaded(context: Context, uriText: String) {
        val set = getUploadedUriSet(context)
        set.add(uriText)
        prefs(context).edit().putStringSet(KEY_UPLOADED_URIS, set).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}

