package com.hitachi.confirmnfc.view

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.util.ProgressDialog
import com.hitachi.confirmnfc.viewmodel.LoginSessionStore
import com.hitachi.confirmnfc.viewmodel.MainViewModel
import com.hitachi.confirmnfc.viewmodel.ViewModelFactory

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val mainViewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(this))[MainViewModel::class.java]
    }

    var back: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ProgressDialog.init(this)

        Log.i(TAG, "onCreate savedInstanceState=${savedInstanceState != null}")
        mainViewModel.configureNavigator(supportFragmentManager, R.id.frameContainer)
        ensureInitialLoginPage(savedInstanceState)
    }

    private fun ensureInitialLoginPage(savedInstanceState: Bundle?) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameContainer)
        val shouldForceLogin = LoginSessionStore.csvRecords.isEmpty() && currentFragment !is LoginFragment
        Log.i(
            TAG,
            "ensureInitialLoginPage restored=${savedInstanceState != null}, " +
                "current=${currentFragment?.javaClass?.simpleName}, forceLogin=$shouldForceLogin"
        )

        if (currentFragment == null || shouldForceLogin) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.frameContainer, LoginFragment(), ActionEnum.LOGIN.toString())
                .commit()
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
