@file:JvmName("NfcConfirmBindingAdapters")

package com.hitachi.confirmnfc.adapter

import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.viewmodel.NfcConfirmViewModel

/**
 * NFC読み取り履歴を縦並びのTextViewとして描画するBindingAdapter群。
 *
 * プロジェクト/AGPのDataBinding解決差異で
 * - LiveData<List<ScanItem>> / LiveData<Int>
 * - List<ScanItem> / Int
 * のどちらが渡ってきても描画できるように、受け口を `Any?` にして正規化する。
 */
object NfcConfirmBindingAdapters {

    @JvmStatic
    @BindingAdapter(value = ["scanItems", "selectedIndex"], requireAll = false)
    fun bindScanItems(
        container: LinearLayout,
        itemsSource: Any?,
        selectedIndexSource: Any?
    ) {
        val items = when (itemsSource) {
            is LiveData<*> -> itemsSource.value as? List<NfcConfirmViewModel.ScanItem>
            is List<*> -> itemsSource.filterIsInstance<NfcConfirmViewModel.ScanItem>()
            else -> null
        }.orEmpty()

        val selectedIndex = when (selectedIndexSource) {
            is LiveData<*> -> selectedIndexSource.value as? Int
            is Int -> selectedIndexSource
            else -> null
        } ?: -1

        container.removeAllViews()
        items.forEachIndexed { index, item ->
            val row = TextView(container.context).apply {
                text = context.getString(R.string.nfc_row_format, index + 1, item.serial)
                textSize = 18f
                setPadding(12, 12, 12, 12)
                typeface = if (index == selectedIndex) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            container.addView(row)
        }
    }

    @JvmStatic
    @BindingAdapter("visibleOrGone")
    fun bindVisibleOrGone(view: View, visible: Boolean?) {
        view.visibility = if (visible == true) View.VISIBLE else View.GONE
    }
}
