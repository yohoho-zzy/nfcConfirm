package com.hitachi.confirmnfc.util

import androidx.appcompat.app.AlertDialog
import com.hitachi.confirmnfc.view.MainActivity
import java.lang.ref.WeakReference

/**
 * 画面共通で利用するメッセージダイアログ管理クラス
 */
class MessageDialog {
    companion object {
        /** 表示中ダイアログ */
        private var alertDialog: AlertDialog? = null

        /** Activity参照を弱参照で保持する */
        private var activityRef: WeakReference<MainActivity>? = null

        /**
         * 利用対象Activityを初期化する
         */
        fun init(activity: MainActivity) {
            val current = activityRef?.get()
            if (current !== activity) {
                dismissInternal()
                activityRef = WeakReference(activity)
            }
        }

        /**
         * メッセージダイアログを表示する
         */
        fun show(msgId: Int) {
            val activity = activityRef?.get() ?: return
            activity.runOnUiThread {
                dismissInternal()
                alertDialog = AlertDialog.Builder(activity)
                    .setMessage(activity.getString(msgId))
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                alertDialog?.show()
            }
        }

        /**
         * ダイアログ表示中かを返す
         */
        fun isShowing(): Boolean = alertDialog?.isShowing == true

        /**
         * ダイアログを閉じる
         */
        fun hide() {
            val activity = activityRef?.get() ?: return
            activity.runOnUiThread {
                dismissInternal()
            }
        }

        /**
         * 内部ダイアログ参照を破棄する
         */
        private fun dismissInternal() {
            alertDialog?.dismiss()
            alertDialog = null
        }
    }
}