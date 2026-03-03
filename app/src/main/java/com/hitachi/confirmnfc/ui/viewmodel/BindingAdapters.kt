package com.hitachi.confirmnfc.ui.viewmodel

import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.hitachi.confirmnfc.R

/**
 * NFC読み取り履歴を縦並びのTextViewとして描画するBindingAdapter。
 */
@BindingAdapter(value = ["scanItems", "selectedIndex"])
fun bindScanItems(
    container: LinearLayout,
    items: List<NfcConfirmViewModel.ScanItem>?,
    selectedIndex: Int?
) {
    renderScanItems(container, items?.value, selectedIndex?.value)
}

/**
 * LiveData を `.value` 展開して渡すレイアウト式向けのオーバーロード。
 */
@BindingAdapter(value = ["scanItems", "selectedIndex"])
fun bindScanItems(
    container: LinearLayout,
    items: List<NfcConfirmViewModel.ScanItem>?,
    selectedIndex: Int?
) {
    renderScanItems(container, items, selectedIndex)
}

private fun renderScanItems(
    container: LinearLayout,
    items: List<NfcConfirmViewModel.ScanItem>?,
    selectedIndex: Int?
) {
    container.removeAllViews()
    val currentItems = items.orEmpty()
    val currentSelected = selectedIndex ?: -1

    currentItems.forEachIndexed { index, item ->
        val row = TextView(container.context).apply {
            text = context.getString(R.string.nfc_row_format, index + 1, item.serial)
            textSize = 18f
            setPadding(12, 12, 12, 12)
            typeface = if (index == currentSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        container.addView(row)
    }
}

/** BooleanをViewの表示/非表示に変換するBindingAdapter。 */
@BindingAdapter("visibleOrGone")
fun bindVisibleOrGone(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}
