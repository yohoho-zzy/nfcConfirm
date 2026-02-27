package com.example.nfcconfirm

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.nfc.Tag
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data class Success(val records: List<CsvRecord>) : LoginState()
    data class Error(val message: String) : LoginState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LoginRepository()

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    private val _nfcMessage = MutableLiveData("カードをタップしてください。")
    val nfcMessage: LiveData<String> = _nfcMessage

    private val _serialText = MutableLiveData("------")
    val serialText: LiveData<String> = _serialText

    private var csvRecords: List<CsvRecord> = emptyList()

    fun login(userId: String) {
        val appContext = getApplication<Application>()
        val phoneNumber = readPhoneNumber(appContext)
        if (phoneNumber.isBlank()) {
            _loginState.value = LoginState.Error("電話番号を取得できません。権限とSIM状態を確認してください。")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = repository.fetchCsv(userId, phoneNumber)
            result.onSuccess {
                csvRecords = it
                _loginState.value = LoginState.Success(it)
                _nfcMessage.value = "ログイン成功。カードをタップしてください。"
            }.onFailure {
                _loginState.value = LoginState.Error(it.message ?: "ログインに失敗しました。")
            }
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readPhoneNumber(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val number = telephonyManager.line1Number ?: ""
        return number.trim()
    }

    fun onTagDetected(tag: Tag?) {
        if (tag == null) {
            _nfcMessage.value = "NFCタグを認識できませんでした。"
            return
        }
        val serial = tag.id?.joinToString(separator = "") { "%02X".format(it) } ?: ""
        if (serial.isBlank()) {
            _nfcMessage.value = "シリアル番号を取得できませんでした。"
            return
        }
        _serialText.value = serial

        val match = csvRecords.firstOrNull { record ->
            record.columns.any { column -> column.equals(serial, ignoreCase = true) }
        }

        _nfcMessage.value = if (match != null) {
            "照合成功: ${match.columns.joinToString(" / ")}"
        } else {
            "登録がありません。"
        }
    }

    fun logout() {
        csvRecords = emptyList()
        _loginState.value = LoginState.Idle
        _nfcMessage.value = "カードをタップしてください。"
        _serialText.value = "------"
    }
}
