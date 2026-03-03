package com.hitachi.confirmnfc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hitachi.confirmnfc.R

/**
 * NFC確認画面の状態を保持するViewModel。
 * 実機NFC処理は行わず、画面表示用の状態のみ管理する。
 */
class NfcConfirmViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<Application>()

    private val _nfcMessage = MutableLiveData(app.getString(R.string.nfc_instruction))
    val nfcMessage: LiveData<String> = _nfcMessage

    private val _nameText = MutableLiveData(app.getString(R.string.serial_default))
    val nameText: LiveData<String> = _nameText

    private val _customerCodeText = MutableLiveData(app.getString(R.string.serial_default))
    val customerCodeText: LiveData<String> = _customerCodeText

    private val _addressText = MutableLiveData(app.getString(R.string.serial_default))
    val addressText: LiveData<String> = _addressText

    private val _notFoundDialogMessage = MutableLiveData<String?>(null)
    val notFoundDialogMessage: LiveData<String?> = _notFoundDialogMessage

    val hasMultipleItems = MutableLiveData(false)
    val hasAnyItems = MutableLiveData(false)
    val pageIndicator = MutableLiveData("")

    fun onNotFoundDialogShown() {
        _notFoundDialogMessage.value = null
    }

    /** ログアウト時に画面状態を初期化する。 */
    fun resetUi() {
        _nfcMessage.value = app.getString(R.string.nfc_instruction)
        _nameText.value = app.getString(R.string.serial_default)
        _customerCodeText.value = app.getString(R.string.serial_default)
        _addressText.value = app.getString(R.string.serial_default)
        hasMultipleItems.value = false
        hasAnyItems.value = false
        pageIndicator.value = ""
        _notFoundDialogMessage.value = null
    }
}
