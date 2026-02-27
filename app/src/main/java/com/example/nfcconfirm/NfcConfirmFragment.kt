package com.example.nfcconfirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.nfcconfirm.databinding.FragmentNfcConfirmBinding

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
            binding.resultView.text = message
        }
        viewModel.serialText.observe(viewLifecycleOwner) { serial ->
            binding.serialValue.text = serial
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
            findNavController().popBackStack(R.id.loginFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
