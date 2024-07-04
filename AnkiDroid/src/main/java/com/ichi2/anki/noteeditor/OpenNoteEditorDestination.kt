/*
 *  Copyright (c) 2024 Sanjay Sargam <sargamsanjaykumar@gmail.com>
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

package com.ichi2.anki.noteeditor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.ichi2.anim.ActivityTransitionAnimation
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.NoteEditor
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.browser.CardBrowserViewModel
import com.ichi2.libanki.CardId
import com.ichi2.libanki.DeckId

sealed interface OpenNoteEditorDestination {

    fun getIntent(context: Context, action: String? = null) =
        SingleFragmentActivity.getIntent(context, NoteEditor::class, toBundle(), action)

    fun toBundle(): Bundle

    data class ImageOcclusion(val imageUri: Uri?) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            NoteEditor.EXTRA_CALLER to NoteEditor.CALLER_IMG_OCCLUSION,
            NoteEditor.EXTRA_IMG_OCCLUSION to imageUri
        )
    }

    data class PassArguments(val arguments: Bundle) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle {
            return arguments
        }
    }

    data class AddNote(val deckId: DeckId? = null) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            NoteEditor.EXTRA_CALLER to NoteEditor.CALLER_DECKPICKER
        ).also { bundle ->
            deckId?.let { deckId -> bundle.putLong(NoteEditor.EXTRA_DID, deckId) }
        }
    }

    data class AddNoteFromCardBrowser(val viewModel: CardBrowserViewModel) :
        OpenNoteEditorDestination {
        override fun toBundle(): Bundle {
            val bundle = bundleOf(
                NoteEditor.EXTRA_CALLER to NoteEditor.CALLER_CARDBROWSER_ADD,
                NoteEditor.EXTRA_TEXT_FROM_SEARCH_VIEW to viewModel.searchTerms
            )
            if (viewModel.lastDeckId?.let { id -> id > 0 } == true) {
                bundle.putLong(NoteEditor.EXTRA_DID, viewModel.lastDeckId!!)
            }
            return bundle
        }
    }

    data class AddNoteFromReviewer(val animation: ActivityTransitionAnimation.Direction? = null) :
        OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            NoteEditor.EXTRA_CALLER to NoteEditor.CALLER_REVIEWER_ADD
        ).also { bundle ->
            animation?.let { animation ->
                bundle.putParcelable(
                    AnkiActivity.FINISH_ANIMATION_EXTRA,
                    animation as Parcelable
                )
            }
        }
    }

    data class AddInstantNote(val sharedText: String?) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            NoteEditor.EXTRA_CALLER to NoteEditor.INSTANT_NOTE_EDITOR,
            Intent.EXTRA_TEXT to sharedText
        )
    }

    data class EditCard(val cardId: CardId, val animation: ActivityTransitionAnimation.Direction) :
        OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            NoteEditor.EXTRA_CALLER to NoteEditor.CALLER_EDIT,
            NoteEditor.EXTRA_CARD_ID to cardId,
            AnkiActivity.FINISH_ANIMATION_EXTRA to animation as Parcelable
        )
    }

    data class EditNoteFromPreviewer(val cardId: Long) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            NoteEditor.EXTRA_CALLER to NoteEditor.CALLER_PREVIEWER_EDIT,
            NoteEditor.EXTRA_EDIT_FROM_CARD_ID to cardId
        )
    }

    data class CopyNote(
        val deckId: DeckId,
        val fieldsText: String,
        val tags: List<String>? = null
    ) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            NoteEditor.EXTRA_CALLER to NoteEditor.CALLER_NOTEEDITOR,
            NoteEditor.EXTRA_DID to deckId,
            NoteEditor.EXTRA_CONTENTS to fieldsText
        ).also { bundle ->
            tags?.let { tags -> bundle.putStringArray(NoteEditor.EXTRA_TAGS, tags.toTypedArray()) }
        }
    }
}
