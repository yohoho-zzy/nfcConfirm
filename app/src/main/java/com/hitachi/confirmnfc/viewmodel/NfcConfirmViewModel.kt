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

/**
 * NFC確認画面の状態を保持するViewModel。
 * タグ照合、履歴管理、ページング状態をここで一元管理する。
 */
class NfcConfirmViewModel(application: Application) : AndroidViewModel(application) {
    data class ScanItem(
        val serial: String,
        val name: String,
        val customerCode: String,
        val address: String
    )

    private val app = getApplication<Application>()

    private val _nfcMessage = MutableLiveData(app.getString(R.string.nfc_instruction))
    val nfcMessage: LiveData<String> = _nfcMessage

    private val _scanItems = MutableLiveData<List<ScanItem>>(emptyList())
    val scanItems: LiveData<List<ScanItem>> = _scanItems

    private val _selectedIndex = MutableLiveData(-1)
    val selectedIndex: LiveData<Int> = _selectedIndex

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

    /** NFCタグ検出時の照合処理。 */
    fun onTagDetected(tag: Tag?) {
        viewModelScope.launch {
            if (LoginSessionStore.csvRecords.isEmpty()) {
                _nfcMessage.value = app.getString(R.string.nfc_instruction)
                return@launch
            }

            Log.i(TAG, "onTagDetected called. hasTag=${tag != null}, csvRecordCount=${LoginSessionStore.csvRecords.size}")
            if (tag == null) {
                _nfcMessage.value = app.getString(R.string.nfc_tag_not_recognized)
                Log.w(TAG, "NFC tag is null")
                return@launch
            }

            vibrateOnTagDetected()
            logTagDetails(tag)

            val serial = tag.id?.joinToString(separator = "") { "%02X".format(it) } ?: ""
            if (serial.isBlank()) {
                _nfcMessage.value = app.getString(R.string.serial_not_available)
                return@launch
            }

            val match = LoginSessionStore.csvRecords.firstOrNull { record ->
                record.columns.any { column -> column.equals(serial, ignoreCase = true) }
            }

            if (match != null) {
                addScanItem(
                    ScanItem(
                        serial = serial,
                        name = match.columns.getOrNull(0).orEmpty().ifBlank { app.getString(R.string.serial_default) },
                        customerCode = match.columns.getOrNull(1).orEmpty().ifBlank { app.getString(R.string.serial_default) },
                        address = match.columns.getOrNull(2).orEmpty().ifBlank { app.getString(R.string.serial_default) }
                    )
                )
                _nfcMessage.value = app.getString(R.string.nfc_instruction)
            } else {
                addScanItem(
                    ScanItem(
                        serial = serial,
                        name = app.getString(R.string.serial_default),
                        customerCode = app.getString(R.string.serial_default),
                        address = app.getString(R.string.serial_default)
                    )
                )
                _nfcMessage.value = app.getString(R.string.nfc_instruction)
                _notFoundDialogMessage.value = app.getString(R.string.not_registered)
            }
        }
    }

    /** 前の履歴へ移動する。 */
    fun showPreviousItem() {
        val current = _selectedIndex.value ?: -1
        if (current > 0) showItemAt(current - 1)
    }

    /** 次の履歴へ移動する。 */
    fun showNextItem() {
        val current = _selectedIndex.value ?: -1
        val lastIndex = (_scanItems.value?.size ?: 0) - 1
        if (current in 0 until lastIndex) showItemAt(current + 1)
    }

    private fun addScanItem(item: ScanItem) {
        val updated = (_scanItems.value ?: emptyList()) + item
        _scanItems.value = updated
        showItemAt(updated.lastIndex)
    }

    private fun showItemAt(index: Int) {
        val items = _scanItems.value ?: return
        if (index !in items.indices) return

        _selectedIndex.value = index
        val selected = items[index]
        _nameText.value = selected.name
        _customerCodeText.value = selected.customerCode
        _addressText.value = selected.address

        hasMultipleItems.value = items.size > 1
        hasAnyItems.value = items.isNotEmpty()
        pageIndicator.value = app.getString(R.string.page_indicator, index + 1, items.size)
    }

    fun onNotFoundDialogShown() {
        _notFoundDialogMessage.value = null
    }

    /** ログアウト時に画面状態を初期化する。 */
    fun resetUi() {
        _nfcMessage.value = app.getString(R.string.nfc_instruction)
        _scanItems.value = emptyList()
        _selectedIndex.value = -1
        _nameText.value = app.getString(R.string.serial_default)
        _customerCodeText.value = app.getString(R.string.serial_default)
        _addressText.value = app.getString(R.string.serial_default)
        hasMultipleItems.value = false
        hasAnyItems.value = false
        pageIndicator.value = ""
        _notFoundDialogMessage.value = null
    }

    /** タグ検出時に短いバイブを鳴らす。 */
    private fun vibrateOnTagDetected() {
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

    /** 調査用にタグ情報をログ出力する。 */
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
