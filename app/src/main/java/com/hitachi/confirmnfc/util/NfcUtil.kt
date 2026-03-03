package com.hitachi.confirmnfc.util

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag

/** NFC読み取りユーティリティ。 */
class NfcUtil(context: Context) {

    private val activity = context as? Activity
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun start(onRead: (tag: Tag) -> Unit, onFailure: (message: String) -> Unit) {
        val currentActivity = activity
        if (currentActivity == null) {
            onFailure("Activityを取得できません")
            return
        }
        if (nfcAdapter == null) {
            onFailure("デバイスはNFCをサポートしていません")
            return
        }
        if (!nfcAdapter.isEnabled) {
            onFailure("NFC機能を有効にしてください")
            return
        }

        nfcAdapter.enableReaderMode(
            currentActivity,
            { tag -> onRead(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    fun stop() {
        activity?.let { nfcAdapter?.disableReaderMode(it) }
    }
}
