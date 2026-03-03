package com.hitachi.confirmnfc.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
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
import com.hitachi.confirmnfc.util.ProgressDialog
import com.hitachi.confirmnfc.viewmodel.LoginViewModel
import com.hitachi.confirmnfc.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    companion object {
        private const val TAG = "LoginFragment"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(requireActivity()))[LoginViewModel::class.java]
    }

    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = permissions.all { result[it] == true }
        viewModel.applyPhonePermissionResult(granted)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.loginViewModel = viewModel

        if (!hasPhonePermission()) {
            permissionLauncher.launch(permissions)
        }

        viewModel.init()

        binding.loginButton.setOnClickListener {
            if (viewModel.phonePermissionDenied) {
                openAppPermissionSettings()
            }
            if (!viewModel.checkOrganizationCode()) {
                return@setOnClickListener
            }
            fetchPhoneNumberAndLoginAsync()
        }
        return binding.root
    }

    private fun fetchPhoneNumberAndLoginAsync() {
        if (!hasPhonePermission()) {
            viewModel.applyPhonePermissionResult(false)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            ProgressDialog.show()
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
}
