package com.hitachi.confirmnfc.enums

import androidx.fragment.app.Fragment
import com.hitachi.confirmnfc.view.LoginFragment
import com.hitachi.confirmnfc.view.NfcConfirmFragment

/**
 * 画面遷移で扱うアクション種別。
 */
enum class ActionEnum {
    /** ログイン画面。 */
    LOGIN,

    /** NFC確認画面。 */
    NFC_CONFIRM;

    /**
     * アクションに対応するFragmentインスタンスを生成する。
     */
    fun fragmentInstance(): Fragment {
        return when (this) {
            LOGIN -> LoginFragment()
            NFC_CONFIRM -> NfcConfirmFragment()
        }
    }
}
