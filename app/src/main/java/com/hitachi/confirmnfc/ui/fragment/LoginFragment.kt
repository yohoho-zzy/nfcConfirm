package com.hitachi.confirmnfc.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
    private var phonePermissionDenied = false

    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = permissions.all { result[it] == true }
        if (granted) {
            phonePermissionDenied = false
            updateLoginButtonText()
            showPhoneNumber()
        } else {
            phonePermissionDenied = true
            binding.passwordInput.setText("")
            binding.loginMessage.text = getString(R.string.phone_permission_required)
            updateLoginButtonText()
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
            if (phonePermissionDenied) {
                openAppPermissionSettings()
                return@setOnClickListener
            }

            val userId = binding.userIdInput.text?.toString()?.trim().orEmpty()
            if (userId.isBlank()) {
                binding.loginMessage.text = getString(R.string.input_user_id_required)
                return@setOnClickListener
            }
            startLogin()
        }

        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginState.Idle -> binding.loginMessage.text = ""
                is LoginState.Loading -> binding.loginMessage.text = ""
                is LoginState.Success -> {
                    binding.loginMessage.text = getString(R.string.login_success)
                    findNavController().navigate(R.id.action_loginFragment_to_nfcConfirmFragment)
                }
                is LoginState.Error -> binding.loginMessage.text = state.message
            }
        }

        viewModel.progressMessage.observe(viewLifecycleOwner) { message ->
            val existing = childFragmentManager.findFragmentByTag(ProgressDialogFragment.TAG)
            if (message.isNullOrBlank()) {
                (existing as? ProgressDialogFragment)?.dismissAllowingStateLoss()
            } else if (existing == null) {
                ProgressDialogFragment.newInstance(message)
                    .show(childFragmentManager, ProgressDialogFragment.TAG)
            }
        }

        requestPhonePermissionAtEntry()
        updateLoginButtonText()
    }

    private fun requestPhonePermissionAtEntry() {
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (granted) {
            phonePermissionDenied = false
            showPhoneNumber()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun showPhoneNumber() {
        val telephonyManager =
            requireContext().getSystemService(TelephonyManager::class.java)
        val number = runCatching { telephonyManager?.line1Number }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.phone_number_unknown)
        binding.passwordInput.setText(number)
    }

    private fun updateLoginButtonText() {
        binding.loginButton.text = if (phonePermissionDenied) {
            getString(R.string.permission_settings_button)
        } else {
            getString(R.string.login_button)
        }
    }

    private fun startLogin() {
        val userId = binding.userIdInput.text?.toString()?.trim().orEmpty()
        val phoneNumber = binding.passwordInput.text?.toString()?.trim().orEmpty()
        viewModel.login(userId, phoneNumber)
    }

    private fun openAppPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
