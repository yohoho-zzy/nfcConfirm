package com.hitachi.confirmnfc.viewmodel

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.hitachi.confirmnfc.view.MainActivity
import com.hitachi.confirmnfc.enums.ActionEnum
import com.hitachi.confirmnfc.enums.FragmentOpCmd
import java.lang.ref.WeakReference

open class BaseViewModel(context: Activity) : ViewModel() {

    protected val context: MainActivity = context as MainActivity

    var navigationVisible: Boolean = false

    fun configureNavigator(fragmentManager: FragmentManager, containerId: Int) {
        managerRef = WeakReference(fragmentManager)
        hostContainerId = containerId
    }

    fun setBack(args: Map<String, String>? = null) {
        onNavigationClick { back(args) }
    }

    fun back(args: Map<String, String>? = null) {
        changeFragment(previousAction, args = args)
    }

    fun onNavigationClick(onClick: () -> Unit) {
        navigationVisible = true
        context.back = onClick
    }

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
        val fragmentTransaction = fragmentManager.beginTransaction()
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

        fragmentTransaction.commitAllowingStateLoss()
        previousAction = currentAction
        currentAction = to
        setCurrentView()
    }

    protected open fun setCurrentView() = Unit

    private fun getFragmentManager(): FragmentManager? = managerRef?.get()

    companion object {
        private var managerRef: WeakReference<FragmentManager>? = null
        private var hostContainerId: Int = 0
        var currentAction: ActionEnum = ActionEnum.LOGIN
            private set
        var previousAction: ActionEnum = ActionEnum.LOGIN
            private set
    }
}
