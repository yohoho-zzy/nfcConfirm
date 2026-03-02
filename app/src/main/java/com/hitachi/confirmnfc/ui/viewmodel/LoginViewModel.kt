package com.hitachi.confirmnfc.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.data.LoginRepository
import com.hitachi.confirmnfc.model.CsvRecord
import kotlinx.coroutines.launch

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

/** 画面側が実行する必要がある副作用コマンド。 */
sealed class LoginCommand {
    data object RequestPhonePermission : LoginCommand()
    data object OpenPermissionSettings : LoginCommand()
}

object LoginSessionStore {
    var csvRecords: List<CsvRecord> = emptyList()
}

/**
 * ログイン画面の状態と処理を管理するViewModel。
 * 画面側には最小限の副作用処理だけを残し、判定ロジックはここに集約する。
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LoginRepository(application)
    private val app = getApplication<Application>()

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    private val _progressMessage = MutableLiveData<String?>(null)
    val progressMessage: LiveData<String?> = _progressMessage

    private val _command = MutableLiveData<LoginCommand?>(null)
    val command: LiveData<LoginCommand?> = _command

    val userId = MutableLiveData("")
    val phoneNumber = MutableLiveData("")
    val loginMessage = MutableLiveData("")
    val loginButtonText = MutableLiveData(app.getString(R.string.login_button))
    val isLoggedIn = MutableLiveData(false)

    private var phonePermissionDenied = false

    /** 初回表示時の権限状態を反映する。 */
    fun onScreenStarted(hasPermission: Boolean, detectedPhoneNumber: String?) {
        if (hasPermission) {
            applyPhonePermissionResult(true, detectedPhoneNumber)
        } else {
            _command.value = LoginCommand.RequestPhonePermission
        }
    }

    /** 権限要求の結果をUI状態へ反映する。 */
    fun applyPhonePermissionResult(granted: Boolean, detectedPhoneNumber: String?) {
        if (granted) {
            phonePermissionDenied = false
            phoneNumber.value = detectedPhoneNumber?.takeIf { it.isNotBlank() }
                ?: app.getString(R.string.phone_number_unknown)
            loginMessage.value = ""
        } else {
            phonePermissionDenied = true
            phoneNumber.value = ""
            loginMessage.value = app.getString(R.string.phone_permission_required)
        }
        loginButtonText.value = if (phonePermissionDenied) {
            app.getString(R.string.permission_settings_button)
        } else {
            app.getString(R.string.login_button)
        }
    }

    /** ログインボタン押下時の判定を行う。 */
    fun onLoginButtonClicked() {
        if (phonePermissionDenied) {
            _command.value = LoginCommand.OpenPermissionSettings
            return
        }

        val id = userId.value?.trim().orEmpty()
        val phone = phoneNumber.value?.trim().orEmpty()

        if (id.isBlank()) {
            loginMessage.value = app.getString(R.string.input_user_id_required)
            return
        }

        login(id, phone)
    }

    fun login(userId: String, phoneNumber: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            loginMessage.value = ""
            _progressMessage.value = app.getString(R.string.login_in_progress)

            val result = repository.fetchCsv(userId, phoneNumber)
            result.onSuccess {
                LoginSessionStore.csvRecords = it
                _loginState.value = LoginState.Success
                _progressMessage.value = null
                loginMessage.value = app.getString(R.string.login_success)
                isLoggedIn.value = true
            }.onFailure {
                _loginState.value = LoginState.Error(
                    it.message ?: app.getString(R.string.login_failed)
                )
                _progressMessage.value = null
                loginMessage.value = it.message ?: app.getString(R.string.login_failed)
            }
        }
    }


    /** 互換用: 旧Fragment実装から呼ばれる状態リセット。 */
    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    /** 画面側で副作用を実行した後に呼び、イベントを消費する。 */
    fun consumeCommand() {
        _command.value = null
    }

    /** 戻る操作でログイン画面へ戻す。 */
    fun clearSession() {
        LoginSessionStore.csvRecords = emptyList()
        _loginState.value = LoginState.Idle
        _progressMessage.value = null
        loginMessage.value = ""
        isLoggedIn.value = false
    }
}
