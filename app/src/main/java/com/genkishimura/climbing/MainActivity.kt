package com.genkishimura.climbing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.genkishimura.climbing.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val youtubeScope = Scope("https://www.googleapis.com/auth/youtube.upload")

    private val openFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                val flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flags)
                AppPrefs.setFolderUri(this, uri)
                renderStatus()
                toast("対象フォルダを保存しました")
            } catch (e: SecurityException) {
                toast("フォルダ権限の保存に失敗しました: ${e.message}")
            }
        }

    private val youtubeSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val accountName = account.account?.name ?: account.email
                if (accountName.isNullOrBlank()) {
                    toast("アカウント情報の取得に失敗しました")
                    return@registerForActivityResult
                }
                AppPrefs.setYouTubeAccountName(this, accountName)
                renderStatus()
                toast("YouTube接続が完了しました")
            } catch (e: ApiException) {
                toast("YouTube接続に失敗しました: ${e.statusCode}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        syncStoredAccountFromGoogleSignIn()
        setupActions()
        renderStatus()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(youtubeScope)
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupActions() {
        binding.selectFolderButton.setOnClickListener {
            openFolderLauncher.launch(null)
        }

        binding.connectYoutubeButton.setOnClickListener {
            val linkedAccount = AppPrefs.getYouTubeAccountName(this)
            if (linkedAccount.isNullOrBlank()) {
                youtubeSignInLauncher.launch(googleSignInClient.signInIntent)
            } else {
                googleSignInClient.signOut().addOnCompleteListener {
                    AppPrefs.clearYouTubeAccountName(this)
                    renderStatus()
                    toast("YouTube接続を解除しました")
                }
            }
        }

        binding.syncButton.setOnClickListener {
            enqueueSyncWork()
        }
    }

    private fun syncStoredAccountFromGoogleSignIn() {
        val lastSignedIn = GoogleSignIn.getLastSignedInAccount(this) ?: return
        if (!GoogleSignIn.hasPermissions(lastSignedIn, youtubeScope)) return
        if (!AppPrefs.getYouTubeAccountName(this).isNullOrBlank()) return
        val accountName = lastSignedIn.account?.name ?: lastSignedIn.email ?: return
        AppPrefs.setYouTubeAccountName(this, accountName)
    }

    private fun renderStatus() {
        val folderUri = AppPrefs.getFolderUri(this)
        val folderName = folderUri?.let { uri ->
            DocumentFile.fromTreeUri(this, uri)?.name ?: uri.toString()
        } ?: getString(R.string.folder_not_selected)
        binding.folderStatusText.text = folderName

        val accountName = AppPrefs.getYouTubeAccountName(this)
        if (accountName.isNullOrBlank()) {
            binding.youtubeStatusText.text = getString(R.string.youtube_not_connected)
            binding.connectYoutubeButton.text = getString(R.string.connect_youtube_button)
        } else {
            binding.youtubeStatusText.text = accountName
            binding.connectYoutubeButton.text = getString(R.string.disconnect_youtube_button)
        }
    }

    private fun enqueueSyncWork() {
        if (AppPrefs.getFolderUri(this) == null) {
            toast("先に対象フォルダを設定してください")
            return
        }
        if (AppPrefs.getYouTubeAccountName(this).isNullOrBlank()) {
            toast("先にYouTube接続を設定してください")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniqueWork("manual_sync", ExistingWorkPolicy.REPLACE, request)
        binding.syncStatusText.text = getString(R.string.sync_queued)

        val liveData = workManager.getWorkInfoByIdLiveData(request.id)
        val observer = object : Observer<WorkInfo> {
            override fun onChanged(info: WorkInfo) {
                val message = info.outputData.getString(UploadWorker.KEY_MESSAGE)
                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        binding.syncStatusText.text = message ?: getString(R.string.sync_succeeded)
                        liveData.removeObserver(this)
                    }

                    WorkInfo.State.FAILED -> {
                        binding.syncStatusText.text = message ?: getString(R.string.sync_failed)
                        liveData.removeObserver(this)
                    }

                    WorkInfo.State.CANCELLED -> {
                        binding.syncStatusText.text = getString(R.string.sync_cancelled)
                        liveData.removeObserver(this)
                    }

                    else -> {
                        binding.syncStatusText.text = getString(R.string.sync_running)
                    }
                }
            }
        }
        liveData.observe(this, observer)
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
