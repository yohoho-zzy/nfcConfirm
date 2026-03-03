package com.hitachi.confirmnfc.util

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.hitachi.confirmnfc.view.MainActivity
import com.hitachi.confirmnfc.R

class ProgressDialog {
    companion object {
        private val instance: ProgressDialog? = null
        private lateinit var alertDialog: AlertDialog
        private lateinit var lblMessage: TextView
        private lateinit var activity: MainActivity

        fun init(activity: MainActivity) {
            if (instance != null) {
                return
            }
            this.activity = activity
            createProgressDialog()
        }

        private fun createProgressDialog() {
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

            lblMessage = TextView(activity)
            lblMessage.text = ""
            lblMessage.setTextColor(Color.parseColor("#000000"))
            lblMessage.textSize = 20F
            lblMessage.layoutParams = linearParam

            linear.addView(progressBar)
            linear.addView(lblMessage)

            val builder = AlertDialog.Builder(activity)
            builder.setCancelable(false)
            builder.setView(linear)

            alertDialog = builder.create()
        }

        fun show(msgId: Int = R.string.progress_default) {
            activity.runOnUiThread {
                if (isShowing()) {
                    alertDialog.dismiss()
                }
                createProgressDialog()

                lblMessage.text = activity.resources.getString(msgId)
                alertDialog.show()

                val window = alertDialog.window
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
            activity.runOnUiThread {
                if (this::alertDialog.isInitialized && alertDialog.isShowing) {
                    alertDialog.hide()
                    alertDialog.dismiss()
                }
            }
        }

        fun isShowing(): Boolean {
            return this::alertDialog.isInitialized && alertDialog.isShowing
        }
    }
}
