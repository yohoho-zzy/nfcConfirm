package com.hitachi.confirmnfc.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.enums.FragmentOpCmd
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

        mainViewModel.configureNavigator(supportFragmentManager, R.id.frameContainer)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameContainer)
        if (savedInstanceState == null || currentFragment == null) {
            mainViewModel.changeFragment(ActionEnum.LOGIN, FragmentOpCmd.OP_REPLACE)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        // 一部端末で復元タイミングによりコンテナが空になるケースへの保険。
        if (supportFragmentManager.findFragmentById(R.id.frameContainer) == null) {
            mainViewModel.changeFragment(ActionEnum.LOGIN, FragmentOpCmd.OP_REPLACE)
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
