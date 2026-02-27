package com.hitachi.confirmnfc.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.hitachi.confirmnfc.databinding.DialogProgressBinding

class ProgressDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogProgressBinding.inflate(LayoutInflater.from(requireContext()))
        binding.progressMessage.text = requireArguments().getString(ARG_MESSAGE).orEmpty()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create().apply {
                setCanceledOnTouchOutside(false)
                setCancelable(false)
            }
    }

    companion object {
        const val TAG = "ProgressDialogFragment"
        private const val ARG_MESSAGE = "message"

        fun newInstance(message: String): ProgressDialogFragment {
            return ProgressDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                }
            }
        }
    }
}
