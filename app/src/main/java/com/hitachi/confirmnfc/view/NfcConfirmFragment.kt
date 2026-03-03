package com.hitachi.confirmnfc.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentNfcConfirmBinding
import com.hitachi.confirmnfc.util.NfcUtil
import com.hitachi.confirmnfc.viewmodel.NfcConfirmViewModel

class NfcConfirmFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): NfcConfirmFragment = NfcConfirmFragment()
    }

    private var _binding: FragmentNfcConfirmBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NfcConfirmViewModel by activityViewModels()
    private var nfcUtil: NfcUtil? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_nfc_confirm, container, false)
        binding.nfcViewModel = viewModel

        viewModel.init()
        nfcUtil = NfcUtil(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
    }

    override fun onResume() {
        super.onResume()
        nfcUtil?.start(
            onRead = { tag ->
                val sn = tag.id.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
                viewModel.onTagRead(sn)
            },
            onFailure = { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onPause() {
        nfcUtil?.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nfcUtil?.stop()
        nfcUtil = null
        _binding = null
    }
}
