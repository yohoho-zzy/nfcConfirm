package com.hitachi.confirmnfc.ui.viewmodel

import android.app.Application
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hitachi.confirmnfc.R
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
    private val repository = LoginRepository(application)

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    private val _nfcMessage = MutableLiveData(application.getString(R.string.nfc_instruction))
    val nfcMessage: LiveData<String> = _nfcMessage

    private val _serialText = MutableLiveData(application.getString(R.string.serial_default))
    val serialText: LiveData<String> = _serialText

    private val _progressMessage = MutableLiveData<String?>(null)
    val progressMessage: LiveData<String?> = _progressMessage

    private var csvRecords: List<CsvRecord> = emptyList()

    fun login(userId: String, phoneNumber: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            _progressMessage.value = getApplication<Application>().getString(R.string.login_in_progress)
            val result = repository.fetchCsv(userId, phoneNumber)
            result.onSuccess {
                csvRecords = it
                _loginState.value = LoginState.Success(it)
                _nfcMessage.value = getApplication<Application>().getString(R.string.login_success_nfc_prompt)
                _progressMessage.value = null
            }.onFailure {
                _loginState.value = LoginState.Error(
                    it.message
                        ?: getApplication<Application>().getString(R.string.login_failed)
                )
                _progressMessage.value = null
            }
        }
    }

    fun onTagDetected(tag: Tag?) {
        viewModelScope.launch {
            if (tag == null) {
                _nfcMessage.value = getApplication<Application>().getString(R.string.nfc_tag_not_recognized)
                Log.w(TAG, "NFC tag is null")
                return@launch
            }

            vibrateOnTagDetected()
            logTagDetails(tag)

            val serial = tag.id?.joinToString(separator = "") { "%02X".format(it) } ?: ""
            if (serial.isBlank()) {
                _nfcMessage.value = getApplication<Application>().getString(R.string.serial_not_available)
                return@launch
            }

            _serialText.value = serial

            val match = csvRecords.firstOrNull { record ->
                record.columns.any { column -> column.equals(serial, ignoreCase = true) }
            }

            _nfcMessage.value = if (match != null) {
                getApplication<Application>().getString(
                    R.string.match_success,
                    match.columns.joinToString(" / ")
                )
            } else {
                getApplication<Application>().getString(R.string.not_registered)
            }
        }
    }

    private fun vibrateOnTagDetected() {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = app.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = app.getSystemService(Vibrator::class.java)
            @Suppress("DEPRECATION")
            vibrator?.vibrate(150)
        }
    }

    private fun logTagDetails(tag: Tag) {
        val serial = tag.id?.joinToString(separator = "") { "%02X".format(it) }.orEmpty()
        Log.i(TAG, "Tag id(hex)=$serial")
        Log.i(TAG, "Tag techList=${tag.techList.joinToString()}")

        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            Log.i(TAG, "NfcA atqa=${nfcA.atqa?.joinToString { "%02X".format(it) }} sak=${nfcA.sak} maxTransceive=${nfcA.maxTransceiveLength}")
        }

        val nfcB = NfcB.get(tag)
        if (nfcB != null) {
            Log.i(TAG, "NfcB appData=${nfcB.applicationData?.joinToString { "%02X".format(it) }} protocolInfo=${nfcB.protocolInfo?.joinToString { "%02X".format(it) }}")
        }

        val nfcF = NfcF.get(tag)
        if (nfcF != null) {
            Log.i(TAG, "NfcF manufacturer=${nfcF.manufacturer?.joinToString { "%02X".format(it) }} systemCode=${nfcF.systemCode?.joinToString { "%02X".format(it) }}")
        }

        val nfcV = NfcV.get(tag)
        if (nfcV != null) {
            Log.i(TAG, "NfcV dsfId=${nfcV.dsfId} responseFlags=${nfcV.responseFlags}")
        }

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            Log.i(TAG, "Ndef type=${ndef.type} maxSize=${ndef.maxSize} isWritable=${ndef.isWritable} canMakeReadOnly=${ndef.canMakeReadOnly()}")
        }

        val ndefFormatable = NdefFormatable.get(tag)
        if (ndefFormatable != null) {
            Log.i(TAG, "NdefFormatable is available for this tag")
        }
    }

    fun logout() {
        csvRecords = emptyList()
        _loginState.value = LoginState.Idle
        _nfcMessage.value = getApplication<Application>().getString(R.string.nfc_instruction)
        _serialText.value = getApplication<Application>().getString(R.string.serial_default)
        _progressMessage.value = null
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}
