package com.hitachi.confirmnfc.util

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.view.MainActivity
import java.lang.ref.WeakReference

/**
 * 画面共通で利用する進捗ダイアログ管理クラス
 */
class ProgressDialog {
    companion object {
        /** 表示中ダイアログ */
        private var alertDialog: AlertDialog? = null

        /** メッセージ表示用ラベル */
        @SuppressLint("StaticFieldLeak")
        private var lblMessage: TextView? = null

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
         * ダイアログUIを生成する
         */
        @SuppressLint("ResourceAsColor")
        private fun createProgressDialog(activity: MainActivity) {
            val padding = 30
            val linear = LinearLayout(activity)
            linear.orientation = LinearLayout.HORIZONTAL
            linear.setPadding(padding, padding, padding, padding)
            linear.gravity = Gravity.CENTER

            var linearParam = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            linearParam.gravity = Gravity.CENTER
            linear.layoutParams = linearParam

            val progressBar = ProgressBar(activity)
            progressBar.isIndeterminate = true
            progressBar.setPadding(0, 0, padding, 0)
            progressBar.layoutParams = linearParam

            linearParam = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            linearParam.gravity = Gravity.CENTER

            lblMessage = TextView(activity).apply {
                text = ""
                setTextColor("#000000".toColorInt())
                textSize = 20F
                layoutParams = linearParam
            }

            linear.addView(progressBar)
            linear.addView(lblMessage)

            val builder = AlertDialog.Builder(activity)
            builder.setCancelable(false)
            builder.setView(linear)

            alertDialog = builder.create()
        }

        /**
         * 進捗ダイアログを表示する
         */
        fun show(msgId: Int = R.string.strProgressDefault) {
            val activity = activityRef?.get() ?: return
            activity.runOnUiThread {
                dismissInternal()
                createProgressDialog(activity)

                lblMessage?.text = activity.resources.getString(msgId)
                alertDialog?.show()

                val window = alertDialog?.window
                if (window != null) {
                    val layoutParams = WindowManager.LayoutParams()
                    layoutParams.copyFrom(window.attributes)
                    layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                    layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                    window.attributes = layoutParams
                }
            }
        }

        /**
         * 進捗ダイアログを非表示にする
         */
        fun hide() {
            val activity = activityRef?.get() ?: return
            activity.runOnUiThread {
                dismissInternal()
            }
        }

        /**
         * ダイアログ表示中かを返す
         */
        fun isShowing(): Boolean = alertDialog?.isShowing == true

        /**
         * 内部ダイアログ参照を破棄する
         */
        private fun dismissInternal() {
            alertDialog?.dismiss()
            alertDialog = null
            lblMessage = null
        }
    }
}
