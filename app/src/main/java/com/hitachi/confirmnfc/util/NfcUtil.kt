package com.hitachi.confirmnfc.util

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.SystemClock

/** NFC読み取りユーティリティ。 */
class NfcUtil(context: Context) {

    private val activity = context as? Activity
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private var lastTagHex: String? = null
    private var lastReadAtMs: Long = 0L

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
            { tag ->
                val now = SystemClock.elapsedRealtime()
                val currentTagHex = tag.id.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
                val isDuplicate = currentTagHex == lastTagHex && now - lastReadAtMs < DUPLICATE_SUPPRESS_MS
                if (isDuplicate) return@enableReaderMode
                lastTagHex = currentTagHex
                lastReadAtMs = now
                onRead(tag)
            },
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000)
            }
        )
    }

    fun stop() {
        activity?.let { nfcAdapter?.disableReaderMode(it) }
    }

    private companion object {
        private const val DUPLICATE_SUPPRESS_MS = 800L
    }
}
