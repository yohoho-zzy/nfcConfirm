package com.hitachi.confirmnfc.util

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.SystemClock

/**
 * NFC ReaderModeの開始・停止と重複読み取り抑止を提供するユーティリティ。
 */
class NfcUtil(context: Context) {

    /** ReaderModeに必要なActivity参照。 */
    private val activity = context as? Activity

    /** 端末のNFCアダプター。 */
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    /** 直近で読み取ったタグID。 */
    private var lastTagHex: String? = null

    /** 直近読取時刻(経過ミリ秒)。 */
    private var lastReadAtMs: Long = 0L

    /**
     * NFC読み取りを開始する。
     */
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

    /**
     * NFC読み取りを停止する。
     */
    fun stop() {
        activity?.let { nfcAdapter?.disableReaderMode(it) }
    }

    private companion object {
        /** 同一タグ再読込を抑止する閾値(ms)。 */
        private const val DUPLICATE_SUPPRESS_MS = 800L
    }
}
