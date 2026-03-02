package com.hitachi.confirmnfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.hitachi.confirmnfc.ui.viewmodel.NfcConfirmViewModel

/**
 * 画面遷移はNavigationに委譲し、ActivityはNFC Intent受け取りだけを担当する。
 */
class MainActivity : AppCompatActivity() {

    private val nfcConfirmViewModel: NfcConfirmViewModel by viewModels()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        syncForegroundDispatch(isNfcConfirmScreen())
    }

    override fun onPause() {
        disableForegroundDispatch()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null || !isNfcConfirmScreen()) {
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

    private fun isNfcConfirmScreen(): Boolean {
        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment)
            as? NavHostFragment ?: return false
        return navHost.navController.currentDestination?.id == R.id.nfcConfirmFragment
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

    companion object {
        private const val TAG = "MainActivity"
    }
}
