package com.hitachi.confirmnfc.ui.viewmodel

import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import com.hitachi.confirmnfc.R

/**
 * NFC読み取り履歴を縦並びのTextViewとして描画するBindingAdapter。
 */
@BindingAdapter(value = ["scanItems", "selectedIndex"])
fun bindScanItems(
    container: LinearLayout,
    items: LiveData<List<NfcConfirmViewModel.ScanItem>>?,
    selectedIndex: LiveData<Int>?
) {
    container.removeAllViews()
    val currentItems = items?.value.orEmpty()
    val currentSelected = selectedIndex?.value ?: -1

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
