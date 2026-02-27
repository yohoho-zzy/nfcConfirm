package com.hitachi.confirmnfc.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentNfcConfirmBinding
import com.hitachi.confirmnfc.ui.viewmodel.AppViewModel

class NfcConfirmFragment : Fragment() {
    private var _binding: FragmentNfcConfirmBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by activityViewModels()

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
        viewModel.serialText.observe(viewLifecycleOwner) { serial ->
            binding.nfc1Value.text = serial
        }

        binding.backButton.setOnClickListener {
            viewModel.logout()
            findNavController().popBackStack(R.id.loginFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
