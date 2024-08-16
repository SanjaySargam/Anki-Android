/*
 *  Copyright (c) 2024 David Allison <davidallisongithub@gmail.com>
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

import android.content.Context
import androidx.annotation.StringRes
import com.ichi2.anki.CollectionManager.TR
import net.ankiweb.rsdroid.Translations

sealed interface StringResource {
    /**
     * @param labelRes The string resource ID for the shortcut label.
     */
    data class AndroidTranslation(@StringRes val labelRes: Int) : StringResource {
        override fun toDisplayString(context: Context): String = context.getString(labelRes)
    }

    /**
     * Represents a function returning a translated string from the Anki Backend
     *
     * **Usage**
     * ```kotlin
     * AnkiBackendTranslation { editingTags() }
     * ```
     */
    data class AnkiBackendTranslation(val getTranslation: Translations.() -> String) : StringResource {
        override fun toDisplayString(context: Context): String = getTranslation(TR)
    }

    // TODO: Parameters are not supported
    fun toDisplayString(context: Context): String
}
