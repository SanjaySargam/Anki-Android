/*
 *  Copyright (c) 2023 Brayan Oliveira <brayandso.dev@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ichi2.anki

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.ichi2.compat.CompatV24
import com.ichi2.compat.ShortcutGroupProvider
import com.ichi2.utils.getInstanceFromClassName
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Activity aimed to host a fragment on the entire screen.
 * For that, it uses [R.layout.single_fragment_activity], which has only a [FragmentContainerView]
 *
 * Useful to avoid creating a Activity for every new screen
 * while being able to reuse the fragment on other places.
 *
 * [getIntent] can be used as an easy way to build a [SingleFragmentActivity]
 */
open class SingleFragmentActivity : AnkiActivity() {
    // The displayed fragment.
    lateinit var fragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) {
            return
        }
        super.onCreate(savedInstanceState)
        if (!ensureStoragePermissions()) {
            return
        }
        setContentView(R.layout.single_fragment_activity)

        // avoid recreating the fragment on configuration changes
        // the fragment should handle state restoration
        if (savedInstanceState != null) {
            return
        }

        val fragmentClassName = requireNotNull(intent.getStringExtra(FRAGMENT_NAME_EXTRA)) {
            "'$FRAGMENT_NAME_EXTRA' extra should be provided"
        }
        fragment = getInstanceFromClassName<Fragment>(fragmentClassName).apply {
            arguments = intent.getBundleExtra(FRAGMENT_ARGS_EXTRA)
        }
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)!!
        return if (fragment is DispatchKeyEventListener) {
            fragment.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    companion object {
        const val FRAGMENT_NAME_EXTRA = "fragmentName"
        const val FRAGMENT_ARGS_EXTRA = "fragmentArgs"

        fun getIntent(context: Context, fragmentClass: KClass<out Fragment>, arguments: Bundle? = null, intentAction: String? = null): Intent {
            return Intent(context, SingleFragmentActivity::class.java).apply {
                putExtra(FRAGMENT_NAME_EXTRA, fragmentClass.jvmName)
                putExtra(FRAGMENT_ARGS_EXTRA, arguments)
                action = intentAction
            }
        }
    }

    override val shortcuts: CompatV24.ShortcutGroup?
        get() = (fragment as? ShortcutGroupProvider)?.shortcuts
}

interface DispatchKeyEventListener {
    fun dispatchKeyEvent(event: KeyEvent): Boolean
}

interface KeyboardShortcutEventListener {
    /**
     * @see AnkiActivity.onProvideKeyboardShortcuts
     */
    fun onProvideKeyboardShortcuts(
        data: MutableList<KeyboardShortcutGroup>,
        menu: Menu?,
        deviceId: Int
    )
}
