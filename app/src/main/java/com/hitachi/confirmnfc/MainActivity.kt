package com.hitachi.confirmnfc

import android.content.Intent
import android.nfc.NfcAdapter
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTopBar()
        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
            viewModel.onTagDetected(tag)
        }
    }

    private fun setupTopBar() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    binding.topBarTitle.text = "ログイン"
                    binding.backButton.isVisible = false
                }

                R.id.nfcConfirmFragment -> {
                    binding.topBarTitle.text = "NFC確認"
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
}
