package com.hitachi.confirmnfc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.model.CsvRecord
import java.util.Locale

/**
 * NFC確認画面の状態を保持するViewModel。
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

    val hasMultipleItems = MutableLiveData(false)
    val hasAnyItems = MutableLiveData(false)
    val pageIndicator = MutableLiveData("")

    /** 初期処理。 */
    fun init() {
        resetUi()
    }

    /** NFCタグを読み取り、CSVデータと照合して画面表示を更新する。 */
    fun onTagRead(tagHex: String) {
        val normalizedTag = normalizeKey(tagHex)
        val matched = LoginSessionStore.csvRecords.firstOrNull { record ->
            record.columns.drop(3).any { key -> normalizeKey(key) == normalizedTag }
        }

        if (matched == null) {
            //_nfcMessage.postValue(app.getString(R.string.not_registered))
            _nfcMessage.postValue(app.getString(R.string.match_success, tagHex))
            _nameText.postValue(app.getString(R.string.serial_default))
            _customerCodeText.postValue(app.getString(R.string.serial_default))
            _addressText.postValue(app.getString(R.string.serial_default))
            hasAnyItems.postValue(false)
            hasMultipleItems.postValue(false)
            pageIndicator.postValue("")
            return
        }

        _nfcMessage.postValue(app.getString(R.string.match_success, tagHex))
        _nameText.postValue(matched.columnOrDefault(0, app.getString(R.string.serial_default)))
        _customerCodeText.postValue(matched.columnOrDefault(1, app.getString(R.string.serial_default)))
        _addressText.postValue(matched.columnOrDefault(2, app.getString(R.string.serial_default)))
        hasAnyItems.postValue(true)
        hasMultipleItems.postValue(false)
        pageIndicator.postValue("")
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
    }

    private fun normalizeKey(value: String): String {
        return value.trim()
            .replace(Regex("[^0-9A-Fa-f]"), "")
            .uppercase(Locale.US)
    }

    private fun CsvRecord.columnOrDefault(index: Int, default: String): String {
        return columns.getOrNull(index)?.takeIf { it.isNotBlank() } ?: default
    }
}
