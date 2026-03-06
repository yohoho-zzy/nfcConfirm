package com.hitachi.confirmnfc.viewmodel

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.data.local.AppDatabase
import com.hitachi.confirmnfc.data.local.CsvRowEntity
import com.hitachi.confirmnfc.model.CsvRecord
import com.hitachi.confirmnfc.model.MatchedItem
import com.hitachi.confirmnfc.util.CsvKeyNormalizer
import com.hitachi.confirmnfc.util.MessageDialog
import com.hitachi.confirmnfc.util.ProgressDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * NFC確認画面の状態を保持するViewModel。
 */
class NfcConfirmViewModel(context: Activity) : BaseViewModel(context) {

    private val csvDao = AppDatabase.getInstance(context.applicationContext).csvDao()

    /** NFC照合処理の実行ジョブ */
    private var searchJob: Job? = null

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
        viewModelScope.launch(Dispatchers.Main.immediate) {
            lastNormalizedTag.value = normalizedTag
            searchJob?.cancel()
            MessageDialog.hide()
            searchJob = launch {
                ProgressDialog.show(R.string.strProgressSearch)
                try {
                    val results = withContext(Dispatchers.IO) {
                        findMatchedItems(normalizedTag)
                    }
                    matchedList.value = results.ifEmpty {
                        MessageDialog.show(R.string.msgNotRegistered)
                        listOf(MatchedItem("", "", CsvRecord(emptyList())))
                    }
                } finally {
                    ProgressDialog.hide()
                }
            }
        }
    }

    /** CSV一覧を検索して一致項目を返す。 */
    private suspend fun findMatchedItems(normalizedTag: String): List<MatchedItem> {
        return csvDao.findByTagKey(normalizedTag).map { row -> row.toMatchedItem() }
    }

    /**
     * NFCキー比較のため文字列を正規化する。
     */
    private fun normalizeKey(value: String): String {
        return CsvKeyNormalizer.normalize(value)
    }

    private fun CsvRowEntity.toMatchedItem(): MatchedItem {
        return MatchedItem(
            nameValue = nameValue,
            codeValue = codeValue,
            record = CsvRecord(emptyList())
        )
    }
}
