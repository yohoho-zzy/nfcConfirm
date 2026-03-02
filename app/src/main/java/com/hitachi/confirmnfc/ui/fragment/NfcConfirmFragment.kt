package com.hitachi.confirmnfc.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.nfcMessage.observe(viewLifecycleOwner) { message ->
            binding.nfcTitle.text = message
        }
        viewModel.nameText.observe(viewLifecycleOwner) { text ->
            binding.nameValue.text = text
        }
        viewModel.customerCodeText.observe(viewLifecycleOwner) { text ->
            binding.customerCodeValue.text = text
        }
        viewModel.addressText.observe(viewLifecycleOwner) { text ->
            binding.addressValue.text = text
        }

        viewModel.scanItems.observe(viewLifecycleOwner) {
            renderScanItems()
        }
        viewModel.selectedIndex.observe(viewLifecycleOwner) {
            renderScanItems()
        }

        binding.previousButton.setOnClickListener {
            viewModel.showPreviousItem()
        }
        binding.nextButton.setOnClickListener {
            viewModel.showNextItem()
        }

        viewModel.notFoundDialogMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrBlank()) {
                return@observe
            }

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

    private fun renderScanItems() {
        val items = viewModel.scanItems.value.orEmpty()
        val selectedIndex = viewModel.selectedIndex.value ?: -1
        binding.nfcListContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val row = TextView(requireContext()).apply {
                text = getString(
                    com.hitachi.confirmnfc.R.string.nfc_row_format,
                    index + 1,
                    item.serial
                )
                textSize = 18f
                setPadding(12, 12, 12, 12)
                typeface = if (index == selectedIndex) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            binding.nfcListContainer.addView(row)
        }

        val hasMultiple = items.size > 1
        binding.previousButton.visibility = if (hasMultiple) View.VISIBLE else View.GONE
        binding.nextButton.visibility = if (hasMultiple) View.VISIBLE else View.GONE
        binding.pageIndicator.visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
        binding.pageIndicator.text = if (items.isNotEmpty()) {
            getString(com.hitachi.confirmnfc.R.string.page_indicator, selectedIndex + 1, items.size)
        } else {
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notFoundDialog?.dismiss()
        notFoundDialog = null
        _binding = null
    }
}
