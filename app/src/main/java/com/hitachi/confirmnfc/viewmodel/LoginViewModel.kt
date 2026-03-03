package com.hitachi.confirmnfc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.repository.LoginRepository
import com.hitachi.confirmnfc.model.CsvRecord
import com.hitachi.confirmnfc.util.ProgressDialog
import kotlinx.coroutines.launch

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

    val organizationCode = MutableLiveData("")
    val phoneNumber = MutableLiveData("")
    val loginMessage = MutableLiveData("")
    val loginButtonText = MutableLiveData(app.getString(R.string.login_button))
    val isLoggedIn = MutableLiveData(false)

    var phonePermissionDenied = false

    /** 初期処理。 */
    fun init() {
        organizationCode.value = ""
    }

    /** 権限要求の結果をUI状態へ反映する。 */
    fun applyPhonePermissionResult(granted: Boolean) {
        if (granted) {
            phonePermissionDenied = false
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

    /** 端末から電話番号取得後にログインを継続する。 */
    fun onPhoneNumberFetched(detectedPhoneNumber: String?) {
        if (!checkOrganizationCode()) {
            return
        }
        if (detectedPhoneNumber?.isNotBlank() == true) {
            login(organizationCode.value!!, detectedPhoneNumber)
        } else {
            loginMessage.value = app.getString(R.string.phone_number_unknown)
            ProgressDialog.hide()
        }
    }

    private fun login(userId: String, phoneNumber: String) {
        viewModelScope.launch {
            loginMessage.value = ""

            val result = repository.fetchCsv(userId, "09012345678")
            result.onSuccess {
                LoginSessionStore.csvRecords = it
                loginMessage.value = app.getString(R.string.login_success)
                isLoggedIn.value = true
                ProgressDialog.hide()
            }.onFailure {
                loginMessage.value = it.message ?: app.getString(R.string.login_failed)
                ProgressDialog.hide()
            }
        }
    }

    fun checkOrganizationCode() : Boolean {
        val id = organizationCode.value?.trim().orEmpty()
        if (id.isBlank()) {
            loginMessage.value = app.getString(R.string.input_user_id_required)
            return false
        }
        return true
    }
}
