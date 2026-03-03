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

/**
 * ログイン画面のUI制御を担うFragment。
 */
class LoginFragment : Fragment() {

    companion object {
        /** ログ出力用のタグ。 */
        private const val TAG = "LoginFragment"
    }

    /** ViewBindingの退避領域。 */
    private var _binding: FragmentLoginBinding? = null

    /** null非許容で利用するBinding参照。 */
    private val binding get() = _binding!!

    /** ログイン処理を担当するViewModel。 */
    private val viewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(requireActivity()))[LoginViewModel::class.java]
    }

    /** 画面で要求する電話関連パーミッション。 */
    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE
    )

    /** 権限要求の結果を受け取るランチャー。 */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // すべての必要権限が許可済みかを判定する。
        val granted = permissions.all { result[it] == true }
        viewModel.applyPhonePermissionResult(granted)
    }

    /**
     * 画面生成時にDataBindingと初期イベントを設定する。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        // LiveDataの更新をレイアウトへ即時反映する。
        binding.lifecycleOwner = viewLifecycleOwner
        // XML側の`loginViewModel`変数へViewModelを関連付ける。
        binding.loginViewModel = viewModel

        if (!hasPhonePermission()) {
            // 初回表示時に未許可なら、その場で権限ダイアログを起動する。
            permissionLauncher.launch(permissions)
        }

        // 入力欄やメッセージの初期表示を整える。
        viewModel.init()

        binding.loginButton.setOnClickListener {
            if (viewModel.phonePermissionDenied) {
                openAppPermissionSettings()
                return@setOnClickListener
            }
            if (!viewModel.checkOrganizationCode()) {
                return@setOnClickListener
            }
            // 入力と権限が揃った場合のみ電話番号取得〜ログインへ進む。
            fetchPhoneNumberAndLoginAsync()
        }
        return binding.root
    }

    /**
     * 設定画面から復帰したタイミングで権限状態を再評価する。
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
    private fun readPhoneNumberOrNull(): String? {
        if (!hasPhonePermission()) return null
        val telephonyManager = requireContext().getSystemService(TelephonyManager::class.java)
        return runCatching { telephonyManager?.line1Number }.getOrNull()
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
        Log.i(TAG, "LoginFragment onDestroyView")
        super.onDestroyView()
        ProgressDialog.hide()
        _binding = null
    }
}
