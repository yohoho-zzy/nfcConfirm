@file:JvmName("NfcConfirmBindingAdapters")

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
 *
 * ✅ 重要:
 * - Kotlinのトップレベル関数だと DataBinding に拾われない環境があるため、
 *   object + @JvmStatic で「public static」として確実に認識させる。
 * - scanItems / selectedIndex は List/Int と LiveData<List>/LiveData<Int> の両方に対応。
 * - 値は View.tag に保持し、片方だけ更新されても再描画できる。
 */
object NfcConfirmBindingAdapters {

    private const val TAG_KEY_SCAN_ITEMS = 0x20000001
    private const val TAG_KEY_SELECTED_INDEX = 0x20000002

    // ----------------------------
    // scanItems : List
    // ----------------------------
    @JvmStatic
    @BindingAdapter("scanItems")
    fun bindScanItems(
        container: LinearLayout,
        items: List<NfcConfirmViewModel.ScanItem>?
    ) {
        container.setTag(TAG_KEY_SCAN_ITEMS, items)
        val selectedIndex = container.getTag(TAG_KEY_SELECTED_INDEX) as? Int
        renderScanItems(container, items, selectedIndex)
    }

    // ----------------------------
    // scanItems : LiveData<List>
    // ----------------------------
    @JvmStatic
    @BindingAdapter("scanItems")
    fun bindScanItemsLiveData(
        container: LinearLayout,
        itemsLiveData: LiveData<List<NfcConfirmViewModel.ScanItem>>?
    ) {
        val items = itemsLiveData?.value
        container.setTag(TAG_KEY_SCAN_ITEMS, items)
        val selectedIndex = container.getTag(TAG_KEY_SELECTED_INDEX) as? Int
        renderScanItems(container, items, selectedIndex)
    }

    // ----------------------------
    // selectedIndex : Int
    // ----------------------------
    @JvmStatic
    @BindingAdapter("selectedIndex")
    fun bindSelectedIndex(
        container: LinearLayout,
        selectedIndex: Int?
    ) {
        container.setTag(TAG_KEY_SELECTED_INDEX, selectedIndex)
        @Suppress("UNCHECKED_CAST")
        val items = container.getTag(TAG_KEY_SCAN_ITEMS) as? List<NfcConfirmViewModel.ScanItem>
        renderScanItems(container, items, selectedIndex)
    }

    // ----------------------------
    // selectedIndex : LiveData<Int>
    // ----------------------------
    @JvmStatic
    @BindingAdapter("selectedIndex")
    fun bindSelectedIndexLiveData(
        container: LinearLayout,
        selectedIndexLiveData: LiveData<Int>?
    ) {
        val selectedIndex = selectedIndexLiveData?.value
        container.setTag(TAG_KEY_SELECTED_INDEX, selectedIndex)
        @Suppress("UNCHECKED_CAST")
        val items = container.getTag(TAG_KEY_SCAN_ITEMS) as? List<NfcConfirmViewModel.ScanItem>
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
    @JvmStatic
    @BindingAdapter("visibleOrGone")
    fun bindVisibleOrGone(view: View, visible: Boolean?) {
        view.visibility = if (visible == true) View.VISIBLE else View.GONE
    }
}