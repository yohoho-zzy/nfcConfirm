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

/**
 * 単一Activity構成でFragmentをホストするエントリ画面。
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** ログ出力用タグ。 */
        private const val TAG = "MainActivity"
    }

    /** 画面遷移を管理するViewModel。 */
    private val mainViewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(this))[MainViewModel::class.java]
    }

    /** カスタム戻る処理のコールバック。 */
    var back: () -> Unit = {}

    /**
     * Activity生成時に初期画面をセットアップする。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ProgressDialog.init(this)

        Log.i(TAG, "onCreate savedInstanceState=${savedInstanceState != null}")
        mainViewModel.configureNavigator(supportFragmentManager, R.id.frameContainer)
        ensureInitialLoginPage(savedInstanceState)
    }

    /**
     * 復元状態を考慮しつつ、未ログイン時はログイン画面を表示する。
     */
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

    /**
     * ナビゲーション状態に応じて戻る処理を切り替える。
     */
    override fun onBackPressed() {
        if (mainViewModel.navigationVisible) {
            back()
        } else {
            super.onBackPressed()
        }
    }
}
