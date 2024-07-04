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

// TODO: Combine with NoteEditorDestination
sealed interface OpenNoteEditorDestination {

    fun getIntent(context: Context) =
        SingleFragmentActivity.getIntent(context, NoteEditor::class, toBundle())

    fun toBundle(): Bundle

    data class ImageOcclusion(val imageUri: Uri?) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            NoteEditor.EXTRA_CALLER to NoteEditor.CALLER_IMG_OCCLUSION,
            NoteEditor.EXTRA_IMG_OCCLUSION to imageUri
        )
    }

    data class PassText(val sampleText: String) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            Intent.EXTRA_TEXT to sampleText
        )
    }

    data class Action(val arguments: Bundle, val action: String) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle {
            arguments.putString("action", action)
            return arguments
        }
    }

    data class ProcessText(val sampleText: String) : OpenNoteEditorDestination {
        override fun toBundle(): Bundle = bundleOf(
            Intent.EXTRA_PROCESS_TEXT to sampleText,
            "action" to Intent.ACTION_PROCESS_TEXT
        )
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
