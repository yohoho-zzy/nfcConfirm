package com.hitachi.confirmnfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import com.hitachi.confirmnfc.databinding.ActivityMainBinding
import com.hitachi.confirmnfc.ui.viewmodel.LoginViewModel
import com.hitachi.confirmnfc.ui.viewmodel.LoginSessionStore
import com.hitachi.confirmnfc.ui.viewmodel.NfcConfirmViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val loginViewModel: LoginViewModel by viewModels()
    private val nfcConfirmViewModel: NfcConfirmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate called, intentAction=${intent?.action}")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTopBar()
        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.i(TAG, "onNewIntent called, intentAction=${intent.action}")
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) {
            Log.w(TAG, "handleNfcIntent skipped because intent is null")
            return
        }

        val isNfcIntent =
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED
        if (!isNfcIntent) {
            Log.i(TAG, "handleNfcIntent ignored because action is not NFC: ${intent.action}")
            return
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost) as? NavHostFragment
        val navController = navHostFragment?.navController
        val currentDestinationId = navController?.currentDestination?.id

        Log.i(
            TAG,
            "handleNfcIntent action=${intent.action}, currentDestinationId=$currentDestinationId, sessionRecordCount=${LoginSessionStore.csvRecords.size}"
        )

        if (currentDestinationId == null) {
            Log.w(TAG, "NavController destination is null. Retry NFC handling on next loop.")
            binding.root.post { handleNfcIntent(intent) }
            return
        }

        if (currentDestinationId == R.id.loginFragment) {
            if (LoginSessionStore.csvRecords.isEmpty()) {
                Log.i(TAG, "Ignore NFC intent on login page because session is empty")
                return
            }

            Log.i(TAG, "NFC intent received on login page with active session. Navigate to confirm page first.")
            navController.navigate(R.id.action_loginFragment_to_nfcConfirmFragment)
            binding.root.post {
                val tag = getTagFromIntent(intent)
                nfcConfirmViewModel.onTagDetected(tag)
            }
            return
        }

        val tag = getTagFromIntent(intent)
        nfcConfirmViewModel.onTagDetected(tag)
    }

    private fun getTagFromIntent(intent: Intent): Tag? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
    }

    private fun setupTopBar() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    binding.topBarTitle.text = getString(R.string.login_title)
                    binding.backButton.isVisible = false
                }

                R.id.nfcConfirmFragment -> {
                    binding.topBarTitle.text = getString(R.string.top_title_nfc_confirm)
                    binding.backButton.isVisible = true
                }

                else -> {
                    binding.topBarTitle.text = ""
                    binding.backButton.isVisible = false
                }
            }
        }

        binding.backButton.setOnClickListener {
            if (navController.currentDestination?.id == R.id.nfcConfirmFragment) {
                loginViewModel.clearSession()
                nfcConfirmViewModel.resetUi()
                navController.navigate(R.id.action_nfcConfirmFragment_to_loginFragment)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
