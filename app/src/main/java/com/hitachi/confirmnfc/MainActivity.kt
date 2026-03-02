package com.hitachi.confirmnfc

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.hitachi.confirmnfc.databinding.ActivityMainBinding
import com.hitachi.confirmnfc.ui.viewmodel.LoginCommand
import com.hitachi.confirmnfc.ui.viewmodel.LoginViewModel
import com.hitachi.confirmnfc.ui.viewmodel.NfcConfirmViewModel

/**
 * 単一Activityでログイン〜NFC確認までを制御する画面。
 * Fragment遷移は使わず、ViewModelの状態に応じて表示を切り替える。
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val loginViewModel: LoginViewModel by viewModels()
    private val nfcConfirmViewModel: NfcConfirmViewModel by viewModels()
    private var notFoundDialog: AlertDialog? = null
    private var progressDialog: Dialog? = null
    private var isForegroundDispatchEnabled = false

    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }

    private val nfcPendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private val nfcIntentFilters by lazy {
        arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
    }

    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE
    )

    /**
     * 複数権限要求の結果を受け取り、ViewModelへ反映する。
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = permissions.all { result[it] == true }
        loginViewModel.applyPhonePermissionResult(granted, getPhoneNumber())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate called, intentAction=${intent?.action}")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupButtons()

        loginViewModel.onScreenStarted(hasPhonePermission(), getPhoneNumber())
    }

    override fun onResume() {
        super.onResume()
        syncForegroundDispatch(loginViewModel.isLoggedIn.value == true)
    }

    override fun onPause() {
        disableForegroundDispatch()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.i(TAG, "onNewIntent called, intentAction=${intent.action}")
        handleNfcIntent(intent)
    }

    /** ViewModel状態を監視してUIを同期する。 */
    private fun setupObservers() {
        binding.lifecycleOwner = this
        binding.loginViewModel = loginViewModel
        binding.nfcViewModel = nfcConfirmViewModel

        loginViewModel.isLoggedIn.observe(this) { loggedIn ->
            binding.loginContainer.root.isVisible = !loggedIn
            binding.nfcContainer.root.isVisible = loggedIn
            binding.topBarTitle.text = if (loggedIn) {
                getString(R.string.top_title_nfc_confirm)
            } else {
                getString(R.string.login_title)
            }
            binding.backButton.isVisible = loggedIn

            syncForegroundDispatch(loggedIn)
        }


        loginViewModel.progressMessage.observe(this) { message ->
            if (message.isNullOrBlank()) {
                progressDialog?.dismiss()
                progressDialog = null
            } else {
                if (progressDialog == null) {
                    progressDialog = Dialog(this).apply {
                        setContentView(R.layout.dialog_progress)
                        setCancelable(false)
                    }
                }
                progressDialog?.findViewById<android.widget.TextView>(R.id.progressMessage)?.text = message
                if (progressDialog?.isShowing != true) progressDialog?.show()
            }
        }

        loginViewModel.command.observe(this) { command ->
            when (command) {
                LoginCommand.RequestPhonePermission -> permissionLauncher.launch(permissions)
                LoginCommand.OpenPermissionSettings -> openAppPermissionSettings()
                null -> Unit
            }
            loginViewModel.consumeCommand()
        }

        nfcConfirmViewModel.notFoundDialogMessage.observe(this, Observer { message ->
            if (message.isNullOrBlank()) return@Observer
            notFoundDialog?.dismiss()
            notFoundDialog = AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    nfcConfirmViewModel.onNotFoundDialogShown()
                }
                .setOnDismissListener {
                    nfcConfirmViewModel.onNotFoundDialogShown()
                }
                .show()
        })
    }

    /** 画面内のクリックイベントを結線する。 */
    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            loginViewModel.clearSession()
            nfcConfirmViewModel.resetUi()
        }
    }

    /** NFC Intentを解析し、対象画面のときだけViewModelへ渡す。 */
    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null || loginViewModel.isLoggedIn.value != true) {
            return
        }

        val isNfcIntent =
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED
        if (!isNfcIntent) {
            return
        }

        val tag = getTagFromIntent(intent)
        nfcConfirmViewModel.onTagDetected(tag)
        consumeNfcIntent(intent)
    }

    private fun syncForegroundDispatch(isNfcScreen: Boolean) {
        if (isNfcScreen) {
            enableForegroundDispatch()
        } else {
            disableForegroundDispatch()
        }
    }

    private fun enableForegroundDispatch() {
        if (isForegroundDispatchEnabled) return
        val adapter = nfcAdapter ?: return

        runCatching {
            adapter.enableForegroundDispatch(
                this,
                nfcPendingIntent,
                nfcIntentFilters,
                null
            )
            isForegroundDispatchEnabled = true
            Log.i(TAG, "Foreground dispatch enabled")
        }.onFailure {
            Log.w(TAG, "Failed to enable foreground dispatch", it)
        }
    }

    private fun disableForegroundDispatch() {
        if (!isForegroundDispatchEnabled) return
        val adapter = nfcAdapter ?: return

        runCatching {
            adapter.disableForegroundDispatch(this)
            isForegroundDispatchEnabled = false
            Log.i(TAG, "Foreground dispatch disabled")
        }.onFailure {
            Log.w(TAG, "Failed to disable foreground dispatch", it)
        }
    }

    private fun consumeNfcIntent(sourceIntent: Intent) {
        sourceIntent.action = null
        sourceIntent.replaceExtras(Bundle())
        if (intent === sourceIntent) {
            setIntent(sourceIntent)
        }
    }

    private fun getTagFromIntent(intent: Intent): Tag? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
    }

    private fun hasPhonePermission(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** 端末から電話番号を取得する。取得不可時はnullを返す。 */
    private fun getPhoneNumber(): String? {
        if (!hasPhonePermission()) return null
        val telephonyManager = getSystemService(TelephonyManager::class.java)
        return runCatching { telephonyManager?.line1Number }.getOrNull()
    }

    /** 権限設定画面へ遷移する。 */
    private fun openAppPermissionSettings() {
        val permissionIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(permissionIntent)
    }


    override fun onDestroy() {
        notFoundDialog?.dismiss()
        progressDialog?.dismiss()
        notFoundDialog = null
        progressDialog = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
