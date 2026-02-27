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
                binding.loginMessage.text = "ユーザーIDを入力してください。"
                return@setOnClickListener
            }
            startLogin()
        }

        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Idle -> {
                    binding.loginProgress.visibility = View.GONE
                    binding.loginMessage.text = ""
                }
                is LoginState.Loading -> {
                    binding.loginProgress.visibility = View.VISIBLE
                    binding.loginMessage.text = "ログイン中..."
                }
                is LoginState.Success -> {
                    binding.loginProgress.visibility = View.GONE
                    binding.loginMessage.text = "ログイン成功"
                    findNavController().navigate(R.id.action_loginFragment_to_nfcConfirmFragment)
                }
                is LoginState.Error -> {
                    binding.loginProgress.visibility = View.GONE
                    binding.loginMessage.text = state.message
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
