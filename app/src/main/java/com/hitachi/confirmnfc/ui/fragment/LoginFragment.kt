package com.hitachi.confirmnfc.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentLoginBinding
import com.hitachi.confirmnfc.ui.viewmodel.AppViewModel
import com.hitachi.confirmnfc.ui.viewmodel.LoginState

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val userId = binding.userIdInput.text?.toString()?.trim().orEmpty()
            if (userId.isBlank()) {
                binding.loginMessage.text = getString(R.string.input_user_id_required)
                return@setOnClickListener
            }
            startLogin()
        }

        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Idle -> {
                    binding.loginMessage.text = ""
                }
                is LoginState.Loading -> {
                    binding.loginMessage.text = ""
                }
                is LoginState.Success -> {
                    binding.loginMessage.text = getString(R.string.login_success)
                    findNavController().navigate(R.id.action_loginFragment_to_nfcConfirmFragment)
                }
                is LoginState.Error -> {
                    binding.loginMessage.text = state.message
                }
            }
        }

        viewModel.progressMessage.observe(viewLifecycleOwner) { message ->
            val existing = childFragmentManager.findFragmentByTag(ProgressDialogFragment.TAG)
            if (message.isNullOrBlank()) {
                (existing as? ProgressDialogFragment)?.dismissAllowingStateLoss()
            } else {
                if (existing == null) {
                    ProgressDialogFragment.newInstance(message)
                        .show(childFragmentManager, ProgressDialogFragment.TAG)
                }
            }
        }
    }

    private fun startLogin() {
        val userId = binding.userIdInput.text?.toString()?.trim().orEmpty()
        viewModel.login(userId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
