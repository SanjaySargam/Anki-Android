/****************************************************************************************
 * Copyright (c) 2024 Sanjay Sargam <sargamsanjaykumar@gmail.com>                          *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki

import android.app.Activity
import android.os.Bundle
import com.ichi2.anki.noteeditor.OpenNoteEditorDestination
import com.ichi2.themes.Themes
import com.ichi2.themes.Themes.disableXiaomiForceDarkMode
import timber.log.Timber

/**
 * This activity serves as an intermediate handler to process various types of intents and forward them to the NoteEditor fragment hosted within the SingleFragmentActivity.
 * It handles the following scenarios:
 * 1. Sharing an image to the NoteEditor.
 * 2. Sharing text to the NoteEditor.
 * 3. Process text to the NoteEditor
 * 4. Open NoteEditor from `Add Note` shortcut
 */
class IntentHandler2 : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Themes.setTheme(this)
        disableXiaomiForceDarkMode(this)
        setContentView(R.layout.progress_bar)
        if (NoteEditor.intentLaunchedWithImage(intent)) {
            Timber.i("Intent contained an image")
            intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_ADD_IMAGE)
        }
        val noteEditorIntent = OpenNoteEditorDestination.PassArguments(intent.extras!!).getIntent(this, intent.action)
        noteEditorIntent.setDataAndType(intent.data, intent.type)
        startActivity(noteEditorIntent)
        finish()
    }
}
