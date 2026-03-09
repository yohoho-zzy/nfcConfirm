package com.hitachi.confirmnfc.viewmodel

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hitachi.confirmnfc.data.AppData
import com.hitachi.confirmnfc.R
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.repository.LoginRepository
import com.hitachi.confirmnfc.util.ProgressDialog
import kotlinx.coroutines.launch

/**
 * ログイン画面の状態と処理を管理するViewModel
 */
class LoginViewModel(context: Activity) : BaseViewModel(context) {

    /** 文字列リソース参照のためのApplication */
    private val app = context.applicationContext as android.app.Application

    /** CSV取得処理を行うRepository */
    private val repository = LoginRepository(app)

    /** 入力された組織コード */
    val organizationCode = MutableLiveData("")

    /** ログイン画面に表示するメッセージ */
    val loginMessage = MutableLiveData("")

    /** 電話権限が拒否されているかどうか */
    var phonePermissionDenied = MutableLiveData(false)

    /**
     * 初期状態へリセットする
     */
    fun init() {
        organizationCode.value = ""
        loginMessage.value = ""
        phonePermissionDenied.value = false
    }

    /**
     * 権限要求の結果をUI状態へ反映する
     */
    fun applyPhonePermissionResult(granted: Boolean) {
        if (granted) {
            // 許可済みなら通常ログイン状態へ戻す
            phonePermissionDenied.value = false
            loginMessage.value = ""
        } else {
            // 拒否時、設定メッセージを表示する
            phonePermissionDenied.value = true
            loginMessage.value = app.getString(R.string.msgPhonePermissionRequired)
        }
    }

    /**
     * 電話番号取得後にログイン処理を継続する
     */
    fun onPhoneNumberFetched(detectedPhoneNumber: String?) {
        if (!checkOrganizationCode()) {
            return
        }
        if (detectedPhoneNumber?.isNotBlank() == true) {
            // 入力済みの組織コード + 取得電話番号で認証を試行する
            login(organizationCode.value!!, detectedPhoneNumber)
        } else {
            // 電話番号未取得時はユーザーへ再確認を促す
            loginMessage.value = app.getString(R.string.msgPhoneNumberUnknown)
            ProgressDialog.hide()
        }
    }

    /**
     * CSV取得を実行してログイン可否を更新する
     */
    private fun login(userId: String, phoneNumber: String) {
        viewModelScope.launch {
            loginMessage.value = ""
            val result = repository.fetchCsv(userId, phoneNumber)

            result.onSuccess {
                AppData.isLoggedIn = true
                loginMessage.value = app.getString(R.string.msgLoginSuccess)
                changeFragment(ActionEnum.NFC_CONFIRM)
                ProgressDialog.hide()
            }.onFailure {
                AppData.isLoggedIn = false
                loginMessage.value = it.message ?: app.getString(R.string.msgLoginFailed)
                ProgressDialog.hide()
            }
        }
    }

    /**
     * 組織コード入力の妥当性を確認する
     */
    fun checkOrganizationCode(): Boolean {
        val id = organizationCode.value?.trim().orEmpty()
        if (id.isBlank()) {
            loginMessage.value = app.getString(R.string.msgCodeInputRequired)
            return false
        }
        return true
    }
}
