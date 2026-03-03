package com.hitachi.confirmnfc.view

import android.Manifest
import android.annotation.SuppressLint
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * ログイン画面のUI制御を担うFragment
 */
class LoginFragment : Fragment() {

    companion object {
        /** ログ出力用のタグ */
        private const val TAG = "LoginFragment"
    }

    /** Binding */
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    /** ViewModel */
    private val viewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(requireActivity()))[LoginViewModel::class.java]
    }

    /** 画面で要求する電話関連パーミッション */
    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE
    )

    /** 権限要求の結果を受け取るランチャー */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // すべての必要権限が許可済みかを判定する。
        val granted = permissions.all { result[it] == true }
        viewModel.applyPhonePermissionResult(granted)
    }

    /**
     * onCreateView:画面生成時にDataBindingと初期イベントを設定する。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.loginViewModel = viewModel

        viewModel.init()

        if (!hasPhonePermission()) {
            // 初回表示時に未許可なら、その場で権限ダイアログを起動する。
            permissionLauncher.launch(permissions)
        }

        // ログインボタン
        binding.loginButton.setOnClickListener {
            if (!viewModel.checkOrganizationCode()) {
                return@setOnClickListener
            }
            fetchPhoneNumberAndLoginAsync()
        }
        // ログインボタン
        binding.settingButton.setOnClickListener {
            openAppPermissionSettings()
        }
        return binding.root
    }

    /**
     * onResume:設定画面から復帰したタイミングで権限状態を再評価する
     */
    override fun onResume() {
        super.onResume()
        viewModel.applyPhonePermissionResult(hasPhonePermission())
    }

    /**
     * 電話番号を非同期に取得してログイン処理を続行する。
     */
    private fun fetchPhoneNumberAndLoginAsync() {
        if (!hasPhonePermission()) {
            // 権限が外れていた場合、UIを「設定へ誘導」状態へ戻す。
            viewModel.applyPhonePermissionResult(false)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            // 通信開始前に進捗ダイアログを表示する。
            ProgressDialog.show()
            val phoneNumber = withContext(Dispatchers.IO) {
                // 電話番号取得はI/Oスレッドで実行する。
                readPhoneNumberOrNull()
            }
            Log.i(TAG, "Phone number loaded. hasValue=${!phoneNumber.isNullOrBlank()}")
            // 取得結果をViewModelへ渡し、以降の判定を委譲する。
            viewModel.onPhoneNumberFetched(phoneNumber)
        }
    }

    /**
     * 必要な電話権限がすべて許可済みか確認する。
     */
    private fun hasPhonePermission(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * TelephonyManagerから電話番号を取得し、失敗時はnullを返す。
     */
    @SuppressLint("HardwareIds")
    private suspend fun readPhoneNumberOrNull(): String? {
        if (!hasPhonePermission()) return null

        val context = requireContext()
        val telephonyManager =
            context.getSystemService(TelephonyManager::class.java) ?: return null

        return try {
            withTimeout(1500L) {   // 防止底层 Binder 卡死
                val number = telephonyManager.line1Number
                number
                    ?.takeIf { it.isNotBlank() }
                    ?.trim()
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to read phone number", e)
            null
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Reading phone number timeout")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error reading phone number", e)
            null
        }
    }

    /**
     * アプリ詳細設定画面を開いて権限変更を促す。
     */
    private fun openAppPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }

    /**
     * View破棄時にダイアログとBindingを解放する。
     */
    override fun onDestroyView() {
        super.onDestroyView()
        ProgressDialog.hide()
        _binding = null
    }
}
