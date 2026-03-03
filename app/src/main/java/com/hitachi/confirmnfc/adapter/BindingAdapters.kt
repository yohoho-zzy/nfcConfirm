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
 * DataBindingのLiveData自動アンラップを前提とし、XML側は
 * `@{nfcViewModel.scanItems}` / `@{nfcViewModel.selectedIndex}` を渡す。
 */
object NfcConfirmBindingAdapters {

    @JvmStatic
    @BindingAdapter(value = ["scanItems", "selectedIndex"], requireAll = false)
    fun bindScanItems(
        container: LinearLayout,
        items: List<NfcConfirmViewModel.ScanItem>?,
        selectedIndex: Int?
    ) {
        container.removeAllViews()

        val currentItems = items.orEmpty()
        val currentSelectedIndex = selectedIndex ?: -1

        currentItems.forEachIndexed { index, item ->
            val row = TextView(container.context).apply {
                text = context.getString(R.string.nfc_row_format, index + 1, item.serial)
                textSize = 18f
                setPadding(12, 12, 12, 12)
                typeface = if (index == currentSelectedIndex) {
                    Typeface.DEFAULT_BOLD
                } else {
                    Typeface.DEFAULT
                }
            }
            container.addView(row)
        }
    }

    /** BooleanをViewの表示/非表示に変換するBindingAdapter。 */
    @JvmStatic
    @BindingAdapter("visibleOrGone")
    fun bindVisibleOrGone(view: View, visible: Boolean?) {
        view.visibility = if (visible == true) View.VISIBLE else View.GONE
    }
}
