package com.hitachi.confirmnfc.enums

import androidx.fragment.app.Fragment
import com.hitachi.confirmnfc.view.LoginFragment
import com.hitachi.confirmnfc.view.NfcConfirmFragment

enum class ActionEnum {
    LOGIN,
    NFC_CONFIRM;

    fun fragmentInstance(): Fragment {
        return when (this) {
            LOGIN -> LoginFragment()
            NFC_CONFIRM -> NfcConfirmFragment()
        }
    }
}
