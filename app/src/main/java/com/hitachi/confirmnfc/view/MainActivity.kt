package com.hitachi.confirmnfc.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.viewmodel.MainViewModel
import com.hitachi.confirmnfc.viewmodel.ViewModelFactory

class MainActivity : AppCompatActivity() {

    private val mainViewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(this))[MainViewModel::class.java]
    }

    var back: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel.resetNavigationState()
        mainViewModel.configureNavigator(supportFragmentManager, R.id.frameContainer)
        ensureInitialLoginPage(savedInstanceState)
    }

    private fun ensureInitialLoginPage(savedInstanceState: Bundle?) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameContainer)
        if (savedInstanceState == null || currentFragment == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.frameContainer, LoginFragment(), ActionEnum.LOGIN.toString())
                .commitNow()
        }
    }

    override fun onBackPressed() {
        if (mainViewModel.navigationVisible) {
            back()
        } else {
            super.onBackPressed()
        }
    }
}
