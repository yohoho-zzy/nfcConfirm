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
import kotlinx.coroutines.launch

class NfcConfirmViewModel(application: Application) : AndroidViewModel(application) {
    private val _nfcMessage = MutableLiveData(application.getString(R.string.nfc_instruction))
    val nfcMessage: LiveData<String> = _nfcMessage

    private val _serialText = MutableLiveData(application.getString(R.string.serial_default))
    val serialText: LiveData<String> = _serialText

    private val _notFoundDialogMessage = MutableLiveData<String?>(null)
    val notFoundDialogMessage: LiveData<String?> = _notFoundDialogMessage

    fun onTagDetected(tag: Tag?) {
        viewModelScope.launch {
            if (LoginSessionStore.csvRecords.isEmpty()) {
                _nfcMessage.value = getApplication<Application>().getString(R.string.nfc_instruction)
                return@launch
            }

            Log.i(TAG, "onTagDetected called. hasTag=${tag != null}, csvRecordCount=${LoginSessionStore.csvRecords.size}")
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

            val match = LoginSessionStore.csvRecords.firstOrNull { record ->
                record.columns.any { column -> column.equals(serial, ignoreCase = true) }
            }

            if (match != null) {
                _nfcMessage.value = getApplication<Application>().getString(
                    R.string.match_success,
                    match.columns.joinToString(" / ")
                )
            } else {
                _nfcMessage.value = getApplication<Application>().getString(R.string.login_success_nfc_prompt)
                _notFoundDialogMessage.value = getApplication<Application>().getString(R.string.not_registered)
            }
        }
    }

    fun onNotFoundDialogShown() {
        _notFoundDialogMessage.value = null
    }

    fun resetUi() {
        _nfcMessage.value = getApplication<Application>().getString(R.string.nfc_instruction)
        _serialText.value = getApplication<Application>().getString(R.string.serial_default)
        _notFoundDialogMessage.value = null
    }

    private fun vibrateOnTagDetected() {
        val app = getApplication<Application>()
        runCatching {
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
        }.onFailure {
            Log.w(TAG, "Unable to vibrate on tag detection", it)
        }
    }

    private fun logTagDetails(tag: Tag) {
        val serial = tag.id?.joinToString(separator = "") { "%02X".format(it) }.orEmpty()
        Log.i(TAG, "Tag id(hex)=$serial")
        Log.i(TAG, "Tag techList=${tag.techList.joinToString()}")

        NfcA.get(tag)?.let {
            Log.i(TAG, "NfcA atqa=${it.atqa?.joinToString { b -> "%02X".format(b) }} sak=${it.sak} maxTransceive=${it.maxTransceiveLength}")
        }
        NfcB.get(tag)?.let {
            Log.i(TAG, "NfcB appData=${it.applicationData?.joinToString { b -> "%02X".format(b) }} protocolInfo=${it.protocolInfo?.joinToString { b -> "%02X".format(b) }}")
        }
        NfcF.get(tag)?.let {
            Log.i(TAG, "NfcF manufacturer=${it.manufacturer?.joinToString { b -> "%02X".format(b) }} systemCode=${it.systemCode?.joinToString { b -> "%02X".format(b) }}")
        }
        NfcV.get(tag)?.let {
            Log.i(TAG, "NfcV dsfId=${it.dsfId} responseFlags=${it.responseFlags}")
        }
        Ndef.get(tag)?.let {
            Log.i(TAG, "Ndef type=${it.type} maxSize=${it.maxSize} isWritable=${it.isWritable} canMakeReadOnly=${it.canMakeReadOnly()}")
        }
        NdefFormatable.get(tag)?.let {
            Log.i(TAG, "NdefFormatable is available for this tag")
        }
    }

    companion object {
        private const val TAG = "NfcConfirmViewModel"
    }
}
