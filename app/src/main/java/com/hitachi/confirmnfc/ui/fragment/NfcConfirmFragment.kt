package com.hitachi.confirmnfc.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hitachi.confirmnfc.databinding.FragmentNfcConfirmBinding
import com.hitachi.confirmnfc.ui.viewmodel.NfcConfirmViewModel

class NfcConfirmFragment : Fragment() {

    private var _binding: FragmentNfcConfirmBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NfcConfirmViewModel by activityViewModels()
    private var notFoundDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNfcConfirmBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.nfcViewModel = viewModel
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
