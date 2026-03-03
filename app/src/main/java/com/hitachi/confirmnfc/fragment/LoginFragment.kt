package com.hitachi.confirmnfc.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentLoginBinding
import com.hitachi.confirmnfc.viewmodel.ActionEnum
import com.hitachi.confirmnfc.viewmodel.FragmentOpCmd
import com.hitachi.confirmnfc.viewmodel.LoginCommand
import com.hitachi.confirmnfc.viewmodel.LoginState
import com.hitachi.confirmnfc.viewmodel.LoginViewModel
import com.hitachi.confirmnfc.viewmodel.MainViewModel
import com.hitachi.confirmnfc.viewmodel.NfcConfirmViewModel
import com.hitachi.confirmnfc.viewmodel.ViewModelFactory
import java.util.regex.Pattern

class LoginFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(): LoginFragment = LoginFragment()
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[LoginViewModel::class.java]
    }
    private val nfcViewModel: NfcConfirmViewModel by activityViewModels()
    private val mainViewModel by lazy {
        ViewModelProvider(requireActivity(), ViewModelFactory(requireActivity()))[MainViewModel::class.java]
    }

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
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.lifecycleOwner = this
        binding.loginViewModel = viewModel
        binding.userIdInput.apply {
            filters = arrayOf(EditTextFilter("^[!-~]{0,128}$"))
        }

        viewModel.init(hasPhonePermission(), getPhoneNumber())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
    }

    private fun observeState() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoginState.Success -> {
                    nfcViewModel.resetUi()
                    mainViewModel.changeFragment(ActionEnum.NFC_CONFIRM, FragmentOpCmd.OP_MOVE)
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

    inner class EditTextFilter(private val regex: String) : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val builder = StringBuilder(dest ?: "")
            builder.replace(dstart, dend, source?.subSequence(start, end).toString())
            val newText = builder.toString()
            if (!Pattern.matches(regex, newText)) {
                return ""
            }
            return null
        }
    }
}
