package com.hitachi.confirmnfc

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hitachi.confirmnfc.viewmodel.ActionEnum
import com.hitachi.confirmnfc.viewmodel.FragmentOpCmd
import com.hitachi.confirmnfc.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel.configureNavigator(supportFragmentManager, R.id.frameContainer)
        if (savedInstanceState == null) {
            mainViewModel.changeFragment(ActionEnum.LOGIN, FragmentOpCmd.OP_REPLACE)
        }
    }
}
