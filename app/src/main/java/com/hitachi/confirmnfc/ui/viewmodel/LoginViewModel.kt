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

object LoginSessionStore {
    var csvRecords: List<CsvRecord> = emptyList()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LoginRepository(application)

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    private val _progressMessage = MutableLiveData<String?>(null)
    val progressMessage: LiveData<String?> = _progressMessage

    fun login(userId: String, phoneNumber: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            _progressMessage.value = getApplication<Application>().getString(R.string.login_in_progress)
            val result = repository.fetchCsv(userId, "09012345678")
            result.onSuccess {
                LoginSessionStore.csvRecords = it
                _loginState.value = LoginState.Success
                _progressMessage.value = null
            }.onFailure {
                _loginState.value = LoginState.Error(
                    it.message ?: getApplication<Application>().getString(R.string.login_failed)
                )
                _progressMessage.value = null
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    fun clearSession() {
        LoginSessionStore.csvRecords = emptyList()
        _loginState.value = LoginState.Idle
        _progressMessage.value = null
    }
}
