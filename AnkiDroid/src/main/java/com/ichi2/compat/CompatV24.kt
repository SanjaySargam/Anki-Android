/*
 *  Copyright (c) 2023 David Allison <davidallisongithub@gmail.com>
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

package com.ichi2.compat

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.icu.util.ULocale
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.MotionEvent
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.OnReceiveContentListener
import androidx.draganddrop.DropHelper
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.R
import com.ichi2.anki.common.utils.android.isRobolectric
import net.ankiweb.rsdroid.Translations
import timber.log.Timber
import java.util.Locale

/** Implementation of [Compat] for SDK level 24 and higher. Check [Compat]'s for more detail.  */
@TargetApi(24)
open class CompatV24 : CompatV23(), Compat {
    override fun normalize(locale: Locale): Locale {
        // ULocale isn't currently handled by Robolectric
        if (isRobolectric) {
            return super.normalize(locale)
        }
        return try {
            val uLocale = ULocale(locale.language, locale.country, locale.variant)
            Locale(uLocale.language, uLocale.country, uLocale.variant)
        } catch (e: Exception) {
            Timber.w("Failed to normalize locale %s", locale, e)
            locale
        }
    }

    override fun configureView(
        activity: Activity,
        view: View,
        mimeTypes: Array<String>,
        options: DropHelper.Options,
        onReceiveContentListener: OnReceiveContentListener
    ) {
        DropHelper.configureView(
            activity,
            view,
            mimeTypes,
            options,
            onReceiveContentListener
        )
    }

    override fun showKeyboardShortcutsDialog(activity: AnkiActivity) {
        val shortcutsGroup = getShortcuts(activity)
        // Don't show keyboard shortcuts dialog if there is no available shortcuts
        if (shortcutsGroup.size <= 1) return
        activity.requestShowKeyboardShortcuts()
    }

    override fun getShortcuts(activity: AnkiActivity): List<KeyboardShortcutGroup> {
        val generalShortcutGroup = ShortcutGroup(
            listOf(
                Shortcut("Alt+K", R.string.show_keyboard_shortcuts_dialog),
                Shortcut("Ctrl+Z", R.string.undo)
            ),
            R.string.pref_cat_general
        ).toShortcutGroup(activity)

        return listOfNotNull(activity.shortcuts?.toShortcutGroup(activity), generalShortcutGroup)
    }

    /**
     * Data class representing a keyboard shortcut.
     *
     * @param shortcut The string representation of the keyboard shortcut (e.g., "Ctrl+Alt+S").
     * @param label The string resource for the shortcut label.
     */
    data class Shortcut(val shortcut: String, val label: StringResource) {
        constructor(shortcut: String, @StringRes labelRes: Int) : this(shortcut, StringResource.AndroidTranslation(labelRes))
        constructor(shortcut: String, getTranslation: Translations.() -> String) : this(shortcut, StringResource.AnkiBackendTranslation(getTranslation))

        /**
         * Converts the shortcut string into a KeyboardShortcutInfo object.
         *
         * @param context The context used to retrieve the string label resource.
         * @return A KeyboardShortcutInfo object representing the keyboard shortcut.
         */
        fun toShortcutInfo(context: Context): KeyboardShortcutInfo {
            val label: String = label.toDisplayString(context)
            val parts = shortcut.split("+")
            val key = parts.last()
            val keycode: Int = KeyEvent.keyCodeFromString(key)
            val modifierFlags: Int = parts.dropLast(1).sumOf { getModifier(it) }

            return KeyboardShortcutInfo(label, keycode, modifierFlags)
        }

        /**
         * Maps a modifier string to its corresponding KeyEvent meta flag.
         *
         * @param modifier The modifier string (e.g., "Ctrl", "Alt", "Shift").
         * @return The corresponding KeyEvent meta flag.
         */
        private fun getModifier(modifier: String): Int {
            return when (modifier) {
                "Ctrl" -> KeyEvent.META_CTRL_ON
                "Alt" -> KeyEvent.META_ALT_ON
                "Shift" -> KeyEvent.META_SHIFT_ON
                else -> 0
            }
        }
    }

    data class ShortcutGroup(val shortcuts: List<Shortcut>, @StringRes val id: Int) {
        fun toShortcutGroup(activity: AnkiActivity): KeyboardShortcutGroup {
            val shortcuts = shortcuts.map { it.toShortcutInfo(activity) }
            val groupLabel = activity.getString(id)
            return KeyboardShortcutGroup(groupLabel, shortcuts)
        }
    }

    override val AXIS_RELATIVE_X: Int = MotionEvent.AXIS_RELATIVE_X
    override val AXIS_RELATIVE_Y: Int = MotionEvent.AXIS_RELATIVE_Y
}
