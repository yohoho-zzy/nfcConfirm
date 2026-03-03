package com.hitachi.confirmnfc.viewmodel

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hitachi.confirmnfc.AppData
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.model.CsvRecord
import java.util.Locale

/**
 * NFC確認画面の状態を保持するViewModel。
 */
class NfcConfirmViewModel(context: Activity) : BaseViewModel(context) {

    /** 文字列リソース参照のためのApplication。 */
    private val app = context.applicationContext as android.app.Application

    /** NFC読み取りメッセージの内部状態。 */
    private val _nfcMessage = MutableLiveData(app.getString(R.string.nfc_instruction))

    /** NFC読み取りメッセージ。 */
    val nfcMessage: LiveData<String> = _nfcMessage

    /** 氏名表示テキストの内部状態。 */
    private val _nameText = MutableLiveData(app.getString(R.string.serial_default))

    /** 氏名表示テキスト。 */
    val nameText: LiveData<String> = _nameText

    /** 顧客コード表示の内部状態。 */
    private val _customerCodeText = MutableLiveData(app.getString(R.string.serial_default))

    /** 顧客コード表示。 */
    val customerCodeText: LiveData<String> = _customerCodeText

    /** 住所表示の内部状態。 */
    private val _addressText = MutableLiveData(app.getString(R.string.serial_default))

    /** 住所表示。 */
    val addressText: LiveData<String> = _addressText

    /** 複数件表示フラグ。 */
    val hasMultipleItems = MutableLiveData(false)

    /** 一件以上存在するフラグ。 */
    val hasAnyItems = MutableLiveData(false)

    /** ページインジケータ表示文字列。 */
    val pageIndicator = MutableLiveData("")

    /**
     * 画面表示初期化。
     */
    fun init() {
        resetUi()
    }

    /**
     * NFCタグを読み取り、CSVデータと照合して画面表示を更新する。
     */
    fun onTagRead(tagHex: String) {
        val normalizedTag = normalizeKey(tagHex)
        val matched = AppData.csvRecords.firstOrNull { record ->
            record.columns.drop(3).any { key -> normalizeKey(key) == normalizedTag }
        }

        if (matched == null) {
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

    /**
     * 画面表示を初期状態へ戻す。
     */
    fun resetUi() {
        _nfcMessage.value = app.getString(R.string.nfc_instruction)
        _nameText.value = app.getString(R.string.serial_default)
        _customerCodeText.value = app.getString(R.string.serial_default)
        _addressText.value = app.getString(R.string.serial_default)
        hasMultipleItems.value = false
        hasAnyItems.value = false
        pageIndicator.value = ""
    }

    /**
     * NFCキー比較のため文字列を正規化する。
     */
    private fun normalizeKey(value: String): String {
        return value.trim()
            .replace(Regex("[^0-9A-Fa-f]"), "")
            .uppercase(Locale.US)
    }

    /**
     * 指定列が空ならデフォルト値を返す拡張関数。
     */
    private fun CsvRecord.columnOrDefault(index: Int, default: String): String {
        return columns.getOrNull(index)?.takeIf { it.isNotBlank() } ?: default
    }
}
