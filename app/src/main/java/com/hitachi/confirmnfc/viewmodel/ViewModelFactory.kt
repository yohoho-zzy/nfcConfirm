package com.hitachi.confirmnfc.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Activityコンテキストを注入してViewModelを生成するFactory。
 */
class ViewModelFactory(
    /** ViewModel生成時に渡すActivity。 */
    private val context: Activity
) : ViewModelProvider.Factory {

    /**
     * 要求されたViewModel型に応じてインスタンスを返す。
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(context) as T
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(context) as T
            modelClass.isAssignableFrom(NfcConfirmViewModel::class.java) -> NfcConfirmViewModel(context) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
