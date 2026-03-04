package com.hitachi.confirmnfc.view

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.data.AppData
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.util.ProgressDialog
import com.hitachi.confirmnfc.viewmodel.MainViewModel
import com.hitachi.confirmnfc.viewmodel.ViewModelFactory

/**
 * 単一Activity構成でFragmentをホストするエントリ画面
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    /** 画面遷移を管理するViewModel */
    private val mainViewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(this))[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ProgressDialog.init(this)
        Log.d(TAG, "onCreate savedInstanceState=${savedInstanceState != null}")
        mainViewModel.configureNavigator(supportFragmentManager, R.id.frameContainer)
        ensureInitialLoginPage()
    }

    /**
     * 復元状態を考慮しつつ、未ログイン時はログイン画面を表示する
     */
    private fun ensureInitialLoginPage() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameContainer)
        val shouldForceLogin = AppData.csvRecords.isEmpty() && currentFragment !is LoginFragment
        if (currentFragment == null || shouldForceLogin) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.frameContainer, LoginFragment(), ActionEnum.LOGIN.toString())
                .commit()
        }
    }
}