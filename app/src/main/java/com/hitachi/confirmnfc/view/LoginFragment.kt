package com.hitachi.confirmnfc.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentLoginBinding
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.enums.FragmentOpCmd
import com.hitachi.confirmnfc.util.ProgressDialog
import com.hitachi.confirmnfc.viewmodel.LoginCommand
import com.hitachi.confirmnfc.viewmodel.LoginState
import com.hitachi.confirmnfc.viewmodel.LoginViewModel
import com.hitachi.confirmnfc.viewmodel.MainViewModel
import com.hitachi.confirmnfc.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class LoginFragment : Fragment() {

    companion object {
        private const val TAG = "LoginFragment"

        @JvmStatic
        fun newInstance(): LoginFragment = LoginFragment()
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[LoginViewModel::class.java]
    }
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
        if (granted) {
            viewModel.applyPhonePermissionResult(true, null)
        } else {
            viewModel.applyPhonePermissionResult(false, null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.loginViewModel = viewModel
        binding.userIdInput.apply {
            filters = arrayOf(EditTextFilter("^[!-~]{0,128}$"))
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        ProgressDialog.init(requireActivity() as MainActivity)
        observeState()

        if (savedInstanceState == null) {
            val hasPermission = hasPhonePermission()
            Log.i(TAG, "LoginFragment init. hasPermission=$hasPermission")
            if (hasPermission) {
                viewModel.init(true, null)
            } else {
                viewModel.init(false, null)
            }
        }
    }

    private fun observeState() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoginState.Success -> {
                    mainViewModel.changeFragment(ActionEnum.NFC_CONFIRM, FragmentOpCmd.OP_MOVE)
                    viewModel.resetState()
                }

                else -> Unit
            }
        }

        viewModel.progressMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrBlank()) {
                ProgressDialog.hide()
            } else {
                ProgressDialog.show(R.string.login_in_progress)
            }
        }

        viewModel.command.observe(viewLifecycleOwner) { command ->
            when (command) {
                LoginCommand.RequestPhonePermission -> {
                    Log.i(TAG, "Requesting phone permissions")
                    permissionLauncher.launch(permissions)
                }

                LoginCommand.OpenPermissionSettings -> openAppPermissionSettings()
                LoginCommand.FetchPhoneNumber -> fetchPhoneNumberAndLoginAsync()
                null -> Unit
            }
            viewModel.consumeCommand()
        }
    }

    private fun fetchPhoneNumberAndLoginAsync() {
        if (!hasPhonePermission()) {
            viewModel.applyPhonePermissionResult(false, null)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val phoneNumber = withContext(Dispatchers.IO) {
                readPhoneNumberOrNull()
            }
            Log.i(TAG, "Phone number loaded. hasValue=${!phoneNumber.isNullOrBlank()}")
            viewModel.onPhoneNumberFetched(phoneNumber)
        }
    }

    private fun hasPhonePermission(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun readPhoneNumberOrNull(): String? {
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
        Log.i(TAG, "LoginFragment onDestroyView")
        super.onDestroyView()
        ProgressDialog.hide()
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
