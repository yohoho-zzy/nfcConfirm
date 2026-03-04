package com.hitachi.confirmnfc.viewmodel

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.view.MainActivity
import java.lang.ref.WeakReference

/**
 * Fragment遷移の共通機能を提供する基底ViewModel
 */
open class BaseViewModel(context: Activity) : ViewModel() {

    /** Activity依存処理を行うためのMainActivity参照 */
    @SuppressLint("StaticFieldLeak")
    protected val context: MainActivity = context as MainActivity

    /**
     * FragmentManagerとコンテナIDを保持して遷移を可能にする
     */
    fun configureNavigator(fragmentManager: FragmentManager, containerId: Int) {
        managerRef = WeakReference(fragmentManager)
        hostContainerId = containerId
    }

    /** 現在表示中の画面アクション（必要なら参照用） */
    var currentAction: ActionEnum = ActionEnum.LOGIN
        private set

    /**
     * 指定画面へ遷移（常に replace）
     */
    fun changeFragment(
        to: ActionEnum,
        args: Map<String, String>? = null
    ) {
        val fm = managerRef?.get() ?: return
        if (hostContainerId == 0) return

        // state保存後のcommitで例外回避
        if (fm.isStateSaved) return

        val bundle = args?.let {
            Bundle().apply { for ((k, v) in it) putString(k, v) }
        }

        val fragmentTag = to.toString()
        val toFragment: Fragment = to.fragmentInstance().apply {
            arguments = bundle
        }

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .replace(hostContainerId, toFragment, fragmentTag)
            .commit()

        currentAction = to
        setCurrentView()
    }

    /**
     * 画面切替後に必要なUI更新があればサブクラスで実装する
     */
    protected open fun setCurrentView() = Unit

    companion object {
        /** FragmentManager参照を弱参照で保持する（複数ViewModelで共有） */
        private var managerRef: WeakReference<FragmentManager>? = null

        /** 遷移先Fragmentを配置するコンテナID（複数ViewModelで共有） */
        private var hostContainerId: Int = 0
    }
}