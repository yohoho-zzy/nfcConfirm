package com.hitachi.confirmnfc.viewmodel

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.enums.FragmentOpCmd
import com.hitachi.confirmnfc.view.MainActivity
import java.lang.ref.WeakReference

/**
 * Fragment遷移の共通機能を提供する基底ViewModel。
 */
open class BaseViewModel(context: Activity) : ViewModel() {

    /** Activity依存処理を行うためのMainActivity参照。 */
    protected val context: MainActivity = context as MainActivity

    /** ナビゲーションUIの表示状態。 */
    var navigationVisible: Boolean = false

    /**
     * FragmentManagerとコンテナIDを保持して遷移を可能にする。
     */
    fun configureNavigator(fragmentManager: FragmentManager, containerId: Int) {
        managerRef = WeakReference(fragmentManager)
        hostContainerId = containerId
    }

    /**
     * 戻る処理をナビゲーションクリックへ紐づける。
     */
    fun setBack(args: Map<String, String>? = null) {
        onNavigationClick { back(args) }
    }

    /**
     * 1つ前の画面へ戻る遷移を実行する。
     */
    fun back(args: Map<String, String>? = null) {
        changeFragment(previousAction, args = args)
    }

    /**
     * ナビゲーションボタン押下時の処理を登録する。
     */
    fun onNavigationClick(onClick: () -> Unit) {
        navigationVisible = true
        context.back = onClick
    }

    /**
     * 指定コマンドに従ってFragmentを切り替える。
     */
    fun changeFragment(
        to: ActionEnum,
        cmd: FragmentOpCmd = FragmentOpCmd.OP_MOVE,
        args: Map<String, String>? = null
    ) {
        val bundle = if (args != null) {
            Bundle().apply {
                for ((key, value) in args) {
                    putString(key, value)
                }
            }
        } else {
            null
        }

        val fragmentManager = getFragmentManager() ?: return
        if (fragmentManager.isStateSaved) return
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setReorderingAllowed(true)
        val fragmentTag = to.toString()

        when (cmd) {
            FragmentOpCmd.OP_REPLACE -> {
                val toFragment: Fragment = to.fragmentInstance()
                toFragment.arguments = bundle
                fragmentTransaction.replace(hostContainerId, toFragment, fragmentTag)
            }

            FragmentOpCmd.OP_SWITCH, FragmentOpCmd.OP_MOVE -> {
                var toFragment: Fragment? = fragmentManager.findFragmentByTag(fragmentTag)
                if (toFragment != null) {
                    toFragment.arguments = bundle
                    fragmentTransaction.show(toFragment)
                } else {
                    toFragment = to.fragmentInstance()
                    toFragment.arguments = bundle
                    fragmentTransaction.add(hostContainerId, toFragment, fragmentTag)
                }

                val fromFragment = fragmentManager.findFragmentByTag(currentAction.toString())
                if (fromFragment != null) {
                    if (cmd == FragmentOpCmd.OP_SWITCH) {
                        fragmentTransaction.hide(fromFragment)
                    } else {
                        fragmentTransaction.remove(fromFragment)
                    }
                }
            }
        }

        if (fragmentManager.isStateSaved) {
            fragmentTransaction.commitAllowingStateLoss()
        } else {
            fragmentTransaction.commit()
        }
        previousAction = currentAction
        currentAction = to
        setCurrentView()
    }

    /**
     * 画面切替後に必要なUI更新があればサブクラスで実装する。
     */
    protected open fun setCurrentView() = Unit

    /**
     * 保持中のFragmentManagerを取得する。
     */
    private fun getFragmentManager(): FragmentManager? = managerRef?.get()

    companion object {
        /** FragmentManager参照を弱参照で保持する。 */
        private var managerRef: WeakReference<FragmentManager>? = null

        /** 遷移先Fragmentを配置するコンテナID。 */
        private var hostContainerId: Int = 0

        /** 現在表示中の画面アクション。 */
        var currentAction: ActionEnum = ActionEnum.LOGIN
            private set

        /** 1つ前に表示していた画面アクション。 */
        var previousAction: ActionEnum = ActionEnum.LOGIN
            private set
    }
}
