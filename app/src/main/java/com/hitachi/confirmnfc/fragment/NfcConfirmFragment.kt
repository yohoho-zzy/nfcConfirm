package com.hitachi.confirmnfc.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentNfcConfirmBinding
import com.hitachi.confirmnfc.viewmodel.NfcConfirmViewModel

class NfcConfirmFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): NfcConfirmFragment = NfcConfirmFragment()
    }

    private var _binding: FragmentNfcConfirmBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NfcConfirmViewModel by activityViewModels()
    private var notFoundDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_nfc_confirm, container, false)
        binding.lifecycleOwner = this
        binding.nfcViewModel = viewModel

        viewModel.init()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.notFoundDialogMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrBlank()) return@observe

            notFoundDialog?.dismiss()
            notFoundDialog = AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.onNotFoundDialogShown()
                }
                .setOnDismissListener {
                    viewModel.onNotFoundDialogShown()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notFoundDialog?.dismiss()
        notFoundDialog = null
        _binding = null
    }
}
