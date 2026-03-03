package com.hitachi.confirmnfc.fragment

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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentLoginBinding
import com.hitachi.confirmnfc.viewmodel.LoginCommand
import com.hitachi.confirmnfc.viewmodel.LoginState
import com.hitachi.confirmnfc.viewmodel.LoginViewModel
import com.hitachi.confirmnfc.viewmodel.NfcConfirmViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private val nfcViewModel: NfcConfirmViewModel by activityViewModels()

    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = permissions.all { result[it] == true }
        viewModel.applyPhonePermissionResult(granted, getPhoneNumber())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.loginViewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeState()
        viewModel.onScreenStarted(hasPhonePermission(), getPhoneNumber())
    }

    private fun observeState() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoginState.Success -> {
                    nfcViewModel.resetUi()
                    findNavController().navigate(R.id.action_loginFragment_to_nfcConfirmFragment)
                    viewModel.resetState()
                }

                else -> Unit
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

        viewModel.command.observe(viewLifecycleOwner) { command ->
            when (command) {
                LoginCommand.RequestPhonePermission -> permissionLauncher.launch(permissions)
                LoginCommand.OpenPermissionSettings -> openAppPermissionSettings()
                null -> Unit
            }
            viewModel.consumeCommand()
        }
    }

    private fun hasPhonePermission(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getPhoneNumber(): String? {
        if (!hasPhonePermission()) return null
        val telephonyManager = requireContext().getSystemService(TelephonyManager::class.java)
        return runCatching { telephonyManager?.line1Number }.getOrNull()
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
