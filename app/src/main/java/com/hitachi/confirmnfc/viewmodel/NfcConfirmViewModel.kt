package com.hitachi.confirmnfc.viewmodel

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hitachi.confirmnfc.data.AppData
import com.hitachi.confirmnfc.model.CsvRecord
import com.hitachi.confirmnfc.model.MatchedItem
import com.hitachi.confirmnfc.util.ProgressDialog
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * NFC確認画面の状態を保持するViewModel。
 */
class NfcConfirmViewModel(context: Activity) : BaseViewModel(context) {

    /** 検索結果 */
    val matchedList = MutableLiveData(
        listOf(MatchedItem("", "", CsvRecord(emptyList())))
    )
    /** NFC番号 */
    val lastNormalizedTag = MutableLiveData("")

    /**
     * 画面表示初期化。
     */
    fun init() {
        matchedList.value = listOf(MatchedItem("", "", CsvRecord(emptyList())))
        lastNormalizedTag.value = ""
    }

    /**
     * NFCタグを読み取り、CSVデータと照合して画面表示を更新する。
     */
    fun onTagRead(tagHex: String) {
        val normalizedTag = normalizeKey(tagHex)
        lastNormalizedTag.postValue(normalizedTag)
        viewModelScope.launch {
            ProgressDialog.show()
            val indexTag = 0
            val indexName = 2
            val indexCode = 1

            val results = mutableListOf<MatchedItem>()

            for (record in AppData.csvRecords) {
                val cols = record.columns
                if (cols.isEmpty()) continue

                val isMatched = normalizeKey(cols.getOrNull(indexTag) ?: "") == normalizedTag
                if (!isMatched) continue

                val nameValue = cols.getOrNull(indexName).orEmpty().trim()
                val codeValue = cols.getOrNull(indexCode).orEmpty().trim()

                results.add(
                    MatchedItem(
                        nameValue = nameValue,
                        codeValue = codeValue,
                        record = record
                    )
                )
            }

            if (results.isEmpty()) {
                matchedList.value = listOf(MatchedItem("", "", CsvRecord(emptyList())))
            } else {
                matchedList.value = results
            }
            ProgressDialog.hide()
        }
    }

    /**
     * NFCキー比較のため文字列を正規化する。
     */
    private fun normalizeKey(value: String): String {
        return value.trim()
            .replace(Regex("[^0-9A-Fa-f]"), "")
            .uppercase(Locale.US)
    }
}