package com.hitachi.confirmnfc.util

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.view.MainActivity
import java.lang.ref.WeakReference

class ProgressDialog {
    companion object {
        private var alertDialog: AlertDialog? = null
        private var lblMessage: TextView? = null
        private var activityRef: WeakReference<MainActivity>? = null

        fun init(activity: MainActivity) {
            val current = activityRef?.get()
            if (current !== activity) {
                dismissInternal()
                activityRef = WeakReference(activity)
            }
        }

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
                setTextColor(Color.parseColor("#000000"))
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

        fun show(msgId: Int = R.string.progress_default) {
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

        fun hide() {
            val activity = activityRef?.get() ?: return
            activity.runOnUiThread {
                dismissInternal()
            }
        }

        fun isShowing(): Boolean = alertDialog?.isShowing == true

        private fun dismissInternal() {
            alertDialog?.dismiss()
            alertDialog = null
            lblMessage = null
        }
    }
}
