package com.hitachi.confirmnfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.util.Log
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import com.hitachi.confirmnfc.databinding.ActivityMainBinding
import com.hitachi.confirmnfc.ui.viewmodel.AppViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: AppViewModel by viewModels()

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

        Log.i(TAG, "handleNfcIntent start, action=${intent.action}")

        if (
            intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            Log.i(TAG, "NFC intent action=${intent.action}, extras=${intent.extras?.keySet()?.joinToString()}")
            val tag = getTagFromIntent(intent)
            Log.i(TAG, "Parsed NFC tag success=${tag != null}")
            viewModel.onTagDetected(tag)
        } else {
            Log.i(TAG, "Intent is not NFC related, ignored. action=${intent.action}")
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
                viewModel.logout()
                navController.popBackStack(R.id.loginFragment, false)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
