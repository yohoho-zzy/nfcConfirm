package com.example.nfcconfirm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.nfcconfirm.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by activityViewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            startLogin()
        } else {
            binding.loginMessage.text = "電話番号の権限が必要です。"
        }
    }

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
            checkPermissionAndLogin()
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

    private fun checkPermissionAndLogin() {
        val context = requireContext()
        val phoneStateGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val phoneNumberGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED

        if (phoneStateGranted && phoneNumberGranted) {
            startLogin()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
                )
            )
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
