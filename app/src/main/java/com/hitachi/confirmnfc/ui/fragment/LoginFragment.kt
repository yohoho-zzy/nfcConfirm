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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.databinding.FragmentLoginBinding
import com.hitachi.confirmnfc.ui.viewmodel.LoginViewModel
import com.hitachi.confirmnfc.ui.viewmodel.LoginState
import com.hitachi.confirmnfc.ui.viewmodel.NfcConfirmViewModel

/**
 * ログイン画面Fragment
 *
 * ■主な処理内容
 * ・電話番号取得のための権限要求
 * ・ユーザーID入力チェック
 * ・ログイン処理の実行
 * ・ログイン結果に応じた画面遷移
 */
class LoginFragment : Fragment() {

    // ViewBinding保持用（メモリリーク防止のためnullable）
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // FragmentスコープのViewModel（ログイン専用）
    private val viewModel: LoginViewModel by viewModels()

    // 電話番号権限が拒否されているかどうかのフラグ
    private var phonePermissionDenied = false

    // 要求する権限一覧（電話番号取得用）
    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE
    )

    /**
     * 複数権限要求のランチャー
     * ユーザーの許可／拒否結果を受け取る
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->

        // 全ての権限が許可されているか確認
        val granted = permissions.all { result[it] == true }

        if (granted) {
            phonePermissionDenied = false
            updateLoginButtonText()
            showPhoneNumber() // 電話番号を取得して表示
        } else {
            phonePermissionDenied = true
            binding.phoneInput.setText("")
            binding.loginMessage.text =
                getString(R.string.phone_permission_required)
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

    /**
     * 画面生成後の初期処理
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ログインボタン押下時の処理
        binding.loginButton.setOnClickListener {

            // 権限拒否状態の場合はアプリ設定画面へ遷移
            if (phonePermissionDenied) {
                openAppPermissionSettings()
                return@setOnClickListener
            }

            val userId =
                binding.userIdInput.text?.toString()?.trim().orEmpty()

            // ユーザーID未入力チェック
            if (userId.isBlank()) {
                binding.loginMessage.text =
                    getString(R.string.input_user_id_required)
                return@setOnClickListener
            }

            startLogin()
        }

        /**
         * ログイン状態の監視
         * ViewModelからの状態変化に応じてUIを更新
         */
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {

                is LoginState.Idle ->
                    binding.loginMessage.text = ""

                is LoginState.Loading ->
                    binding.loginMessage.text = ""

                is LoginState.Success -> {
                    binding.loginMessage.text =
                        getString(R.string.login_success)

                    // NFC確認画面へ遷移
                    findNavController().navigate(
                        R.id.action_loginFragment_to_nfcConfirmFragment
                    )

                    // 状態をリセット（再遷移防止）
                    viewModel.resetState()
                }

                is LoginState.Error ->
                    binding.loginMessage.text = state.message
            }
        }

        /**
         * プログレスダイアログ表示制御
         * progressMessageが存在する場合のみ表示
         */
        viewModel.progressMessage.observe(viewLifecycleOwner) { message ->
            val existing =
                childFragmentManager.findFragmentByTag(
                    ProgressDialogFragment.TAG
                )

            if (message.isNullOrBlank()) {
                (existing as? ProgressDialogFragment)
                    ?.dismissAllowingStateLoss()
            } else if (existing == null) {
                ProgressDialogFragment
                    .newInstance(message)
                    .show(
                        childFragmentManager,
                        ProgressDialogFragment.TAG
                    )
            }
        }

        // 画面表示時に権限確認
        requestPhonePermissionAtEntry()

        // ボタン文言初期化
        updateLoginButtonText()
    }

    /**
     * 画面表示時の権限確認処理
     */
    private fun requestPhonePermissionAtEntry() {

        val granted = permissions.all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (granted) {
            phonePermissionDenied = false
            showPhoneNumber()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    /**
     * 端末の電話番号を取得して入力欄に設定
     */
    private fun showPhoneNumber() {

        val telephonyManager =
            requireContext().getSystemService(
                TelephonyManager::class.java
            )

        val number = runCatching {
            telephonyManager?.line1Number
        }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.phone_number_unknown)

        binding.phoneInput.setText(number)
    }

    /**
     * ログインボタンの表示文言更新
     */
    private fun updateLoginButtonText() {
        binding.loginButton.text =
            if (phonePermissionDenied) {
                getString(R.string.permission_settings_button)
            } else {
                getString(R.string.login_button)
            }
    }

    /**
     * ログイン処理開始
     */
    private fun startLogin() {
        val userId =
            binding.userIdInput.text?.toString()?.trim().orEmpty()
        val phoneNumber =
            binding.phoneInput.text?.toString()?.trim().orEmpty()

        viewModel.login(userId, phoneNumber)
    }

    /**
     * アプリの権限設定画面を開く
     */
    private fun openAppPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts(
                "package",
                requireContext().packageName,
                null
            )
        )
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // メモリリーク防止
    }
}