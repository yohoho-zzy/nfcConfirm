package com.hitachi.confirmnfc.ui.viewmodel

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hitachi.confirmnfc.data.LoginRepository
import com.hitachi.confirmnfc.model.CsvRecord
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

    private val _nfcMessage = MutableLiveData("カードをかざしてください")
    val nfcMessage: LiveData<String> = _nfcMessage

    private val _serialText = MutableLiveData("------")
    val serialText: LiveData<String> = _serialText

    private val _progressMessage = MutableLiveData<String?>(null)
    val progressMessage: LiveData<String?> = _progressMessage

    private var csvRecords: List<CsvRecord> = emptyList()

    fun login(userId: String) {
        val temporaryPhoneNumber = "09012345678"

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            _progressMessage.value = "ログイン中..."
            val result = repository.fetchCsv(userId, temporaryPhoneNumber)
            result.onSuccess {
                csvRecords = it
                _loginState.value = LoginState.Success(it)
                _nfcMessage.value = "ログイン成功。NFCをかざしてください"
                _progressMessage.value = null
            }.onFailure {
                _loginState.value = LoginState.Error(it.message ?: "ログインに失敗しました。")
                _progressMessage.value = null
            }
        }
    }

    fun onTagDetected(tag: Tag?) {
        viewModelScope.launch {
            _progressMessage.value = "取得中..."

            if (tag == null) {
                _nfcMessage.value = "NFCタグを認識できませんでした。"
                _progressMessage.value = null
                return@launch
            }

            val serial = tag.id?.joinToString(separator = "") { "%02X".format(it) } ?: ""
            if (serial.isBlank()) {
                _nfcMessage.value = "シリアル番号を取得できませんでした。"
                _progressMessage.value = null
                return@launch
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

            _progressMessage.value = null
        }
    }

    fun logout() {
        csvRecords = emptyList()
        _loginState.value = LoginState.Idle
        _nfcMessage.value = "カードをかざしてください"
        _serialText.value = "------"
        _progressMessage.value = null
    }
}
