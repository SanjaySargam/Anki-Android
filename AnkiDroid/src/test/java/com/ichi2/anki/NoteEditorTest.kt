/*
 Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free Software
 Foundation; either version 3 of the License, or (at your option) any later
 version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program.  If not, see <http://www.gnu.org/licenses/>.
 */
@file:Suppress("SameParameterValue")

package com.ichi2.anki

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.config.ConfigKey
import com.ichi2.anim.ActivityTransitionAnimation.Direction.DEFAULT
import com.ichi2.anki.NoteEditorTest.FromScreen.DECK_LIST
import com.ichi2.anki.NoteEditorTest.FromScreen.REVIEWER
import com.ichi2.anki.api.AddContentApi.Companion.DEFAULT_DECK_ID
import com.ichi2.anki.dialogs.DeckSelectionDialog.SelectableDeck
import com.ichi2.anki.multimediacard.activity.MultimediaEditFieldActivity
import com.ichi2.anki.noteeditor.EditCardDestination
import com.ichi2.anki.noteeditor.toIntent
import com.ichi2.anki.utils.ext.isImageOcclusion
import com.ichi2.libanki.Consts
import com.ichi2.libanki.DeckId
import com.ichi2.libanki.Decks.Companion.CURRENT_DECK
import com.ichi2.libanki.Note
import com.ichi2.libanki.NotetypeJson
import com.ichi2.testutils.AnkiAssert.assertDoesNotThrow
import com.ichi2.testutils.getString
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class NoteEditorTest : RobolectricTest() {
    @Test
    @Config(qualifiers = "en")
    fun verifyCardsList() {
        val activity = getNoteEditorEditingExistingBasicNote("Test", "Note", DECK_LIST)
        val n = activity.supportFragmentManager.fragments.first() as NoteEditor
        assertThat("Cards list is correct", (n.requireView().findViewById<TextView>(R.id.CardEditorCardsButton)).text.toString(), equalTo("Cards: Card 1"))
    }

    @Test
    fun whenEditingMultimediaEditUsesCurrentValueOfFields() {
        // Arrange
        val fieldIndex = 0
        val n = getNoteEditorEditingExistingBasicNote("Hello", "World", REVIEWER)
        val editor = n.supportFragmentManager.fragments.first() as NoteEditor
        enterTextIntoField(editor, fieldIndex, "Good Afternoon")

        // Act
        openAdvancedTextEditor(editor, fieldIndex)

        // Assert
        val intent = shadowOf(n).nextStartedActivityForResult
        val actualField = MultimediaEditFieldActivity.getFieldFromIntent(intent.intent)!!
        assertThat("Provided value should be the updated value", actualField.second.formattedValue, equalTo("Good Afternoon"))
    }

    @Test
    fun errorSavingNoteWithNoFirstFieldDisplaysNoFirstField() = runTest {
        val activity = getNoteEditorAdding(NoteType.BASIC)
            .withNoFirstField()
            .build()
        val noteEditor = activity.supportFragmentManager.fragments.first() as NoteEditor
        noteEditor.saveNote()
        val actualResourceId = noteEditor.snackbarErrorText
        assertThat(actualResourceId, equalTo(CollectionManager.TR.addingTheFirstFieldIsEmpty()))
    }

    @Test
    fun testErrorMessageNull() = runTest {
        val activity = getNoteEditorAdding(NoteType.BASIC)
            .withNoFirstField()
            .build()
        val noteEditor = activity.supportFragmentManager.fragments.first() as NoteEditor
        noteEditor.saveNote()
        assertThat(noteEditor.addNoteErrorMessage, equalTo(CollectionManager.TR.addingTheFirstFieldIsEmpty()))

        noteEditor.setFieldValueFromUi(0, "Hello")

        noteEditor.saveNote()
        assertThat(noteEditor.addNoteErrorMessage, equalTo(null))
    }

//    @Test
//    @RustCleanup("needs update for new backend")
//    fun errorSavingInvalidNoteWithAllFieldsDisplaysInvalidTemplate() {
//        val noteEditor = getNoteEditorAdding(NoteType.THREE_FIELD_INVALID_TEMPLATE)
//            .withFirstField("A")
//            .withSecondField("B")
//            .withThirdField("C")
//            .build()
//        val actualResourceId = noteEditor.addNoteErrorResource
//        assertThat(actualResourceId, equalTo(R.string.note_editor_no_cards_created_all_fields))
//    }
//
//    @Test
//    @RustCleanup("needs update for new backend")
//    fun errorSavingInvalidNoteWitSomeFieldsDisplaysEnterMore() {
//        val noteEditor = getNoteEditorAdding(NoteType.THREE_FIELD_INVALID_TEMPLATE)
//            .withFirstField("A")
//            .withThirdField("C")
//            .build()
//        val actualResourceId = noteEditor.addNoteErrorResource
//        assertThat(actualResourceId, equalTo(R.string.note_editor_no_cards_created))
//    }

    @Test
    fun errorSavingClozeNoteWithNoFirstFieldDisplaysClozeError() = runTest {
        val activity = getNoteEditorAdding(NoteType.CLOZE)
            .withNoFirstField()
            .build()
        val noteEditor = activity.supportFragmentManager.fragments.first() as NoteEditor
        noteEditor.saveNote()
        val actualResourceId = noteEditor.snackbarErrorText
        assertThat(actualResourceId, equalTo(CollectionManager.TR.addingTheFirstFieldIsEmpty()))
    }

    @Test
    fun errorSavingClozeNoteWithNoClozeDeletionsDisplaysClozeError() = runTest {
        val activity = getNoteEditorAdding(NoteType.CLOZE)
            .withFirstField("NoCloze")
            .build()
        val noteEditor = activity.supportFragmentManager.fragments.first() as NoteEditor
        noteEditor.saveNote()
        val actualResourceId = noteEditor.snackbarErrorText
        assertThat(
            actualResourceId,
            equalTo(CollectionManager.TR.addingYouHaveAClozeDeletionNote())
        )
    }

    @Test
    fun errorSavingNoteWithNoTemplatesShowsNoCardsCreated() = runTest {
        val activity = getNoteEditorAdding(NoteType.BACK_TO_FRONT)
            .withFirstField("front is not enough")
            .build()
        val noteEditor = activity.supportFragmentManager.fragments.first() as NoteEditor
        noteEditor.saveNote()
        val actualResourceId = noteEditor.snackbarErrorText
        assertThat(actualResourceId, equalTo(getString(R.string.note_editor_no_cards_created)))
    }

    @Test
    fun clozeNoteWithNoClozeDeletionsDoesNotSave() = runTest {
        val initialCards = cardCount
        val activity = getNoteEditorAdding(NoteType.CLOZE)
            .withFirstField("no cloze deletions")
            .build()
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        editor.saveNote()
        assertThat(cardCount, equalTo(initialCards))
    }

    @Test
    fun clozeNoteWithClozeDeletionsDoesSave() = runTest {
        val initialCards = cardCount
        val activity = getNoteEditorAdding(NoteType.CLOZE)
            .withFirstField("{{c1::AnkiDroid}} is fantastic")
            .build()
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        editor.saveNote()
        assertThat(cardCount, equalTo(initialCards + 1))
    }

    @Test
    @Ignore("Not yet implemented")
    fun clozeNoteWithClozeInWrongFieldDoesNotSave() = runTest {
        // Anki Desktop blocks with "Continue?", we should just block to match the above test
        val initialCards = cardCount
        val activity = getNoteEditorAdding(NoteType.CLOZE)
            .withSecondField("{{c1::AnkiDroid}} is fantastic")
            .build()
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        editor.saveNote()
        assertThat(cardCount, equalTo(initialCards))
    }

    @Test
    fun verifyStartupAndCloseWithNoCollectionDoesNotCrash() {
        enableNullCollection()
        ActivityScenario.launchActivityForResult(SingleFragmentActivity::class.java).use { scenario ->
            scenario.onActivity { noteEditor ->
                noteEditor.onBackPressedDispatcher.onBackPressed()
                assertThat("Pressing back should finish the activity", noteEditor.isFinishing)
            }
            val result = scenario.result
            assertThat("Activity should be cancelled as no changes were made", result.resultCode, equalTo(Activity.RESULT_CANCELED))
        }
    }

    @Test
    fun copyNoteCopiesDeckId() {
        val currentDid = addDeck("Basic::Test")
        col.config.set(CURRENT_DECK, currentDid)
        val n = super.addNoteUsingBasicModel("Test", "Note")
        n.notetype.put("did", currentDid)
        val activity1 = getNoteEditorEditingExistingBasicNote("Test", "Note", DECK_LIST)
        val editor = activity1.supportFragmentManager.fragments.first() as NoteEditor
        col.config.set(CURRENT_DECK, Consts.DEFAULT_DECK_ID) // Change DID if going through default path
        val copyNoteIntent = getCopyNoteIntent(editor)
        copyNoteIntent.putExtra(SingleFragmentActivity.FRAGMENT_NAME_EXTRA, NoteEditor::class.jvmName)
        val activity = super.startActivityNormallyOpenCollectionWithIntent(SingleFragmentActivity::class.java, copyNoteIntent)
        val newNoteEditor = activity.supportFragmentManager.fragments.first() as NoteEditor
        assertThat("Selected deck ID should be the current deck id", editor.deckId, equalTo(currentDid))
        assertThat("Deck ID in the intent should be the selected deck id", copyNoteIntent.getLongExtra(NoteEditor.EXTRA_DID, -404L), equalTo(currentDid))
        assertThat("Deck ID in the new note should be the ID provided in the intent", newNoteEditor.deckId, equalTo(currentDid))
    }

    @Test
    fun stickyFieldsAreUnchangedAfterAdd() = runTest {
        // #6795 - newlines were converted to <br>
        val basic = makeNoteForType(NoteType.BASIC)

        // Enable sticky "Front" field
        basic!!.getJSONArray("flds").getJSONObject(0).put("sticky", true)
        val initFirstField = "Hello"
        val initSecondField = "unused"
        val newFirstField = "Hello" + FieldEditText.NEW_LINE + "World" // /r/n on Windows under Robolectric
        val activity = getNoteEditorAdding(NoteType.BASIC)
            .withFirstField(initFirstField)
            .withSecondField(initSecondField)
            .build()
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        assertThat(editor.currentFieldStrings.toList(), contains(initFirstField, initSecondField))
        editor.setFieldValueFromUi(0, newFirstField)
        assertThat(editor.currentFieldStrings.toList(), contains(newFirstField, initSecondField))

        editor.saveNote()
        waitForAsyncTasksToComplete()
        val actual = editor.currentFieldStrings.toList()

        assertThat("newlines should be preserved, second field should be blanked", actual, contains(newFirstField, ""))
    }

    @Test
    fun processTextIntentShouldCopyFirstField() {
        ensureCollectionLoadIsSynchronous()

        val i = Intent(Intent.ACTION_PROCESS_TEXT)
        val args = Bundle().apply {
            putString(Intent.EXTRA_PROCESS_TEXT, "hello\nworld")
        }
        i.putExtra(SingleFragmentActivity.FRAGMENT_NAME_EXTRA, NoteEditor::class.jvmName)
        i.putExtra(SingleFragmentActivity.FRAGMENT_ARGS_EXTRA, args)
        val editor = startActivityNormallyOpenCollectionWithIntent(SingleFragmentActivity::class.java, i)
        val noteEditor = editor.supportFragmentManager.fragments.first() as NoteEditor
        val actual = noteEditor.currentFieldStrings.toList()

        assertThat(actual, contains("hello\nworld", ""))
    }

    @Test
    fun previewWorksWithNoError() {
        // #6923 regression test - Low value - Could not make this fail as onSaveInstanceState did not crash under Robolectric.
        val activity = getNoteEditorAddingNote(DECK_LIST, SingleFragmentActivity::class.java)
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        assertDoesNotThrow { runBlocking { editor.performPreview() } }
    }

    @Test
    fun clearFieldWorks() {
        // #7522
        val activity = getNoteEditorAddingNote(DECK_LIST, SingleFragmentActivity::class.java)
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        editor.setFieldValueFromUi(1, "Hello")
        assertThat(editor.currentFieldStrings[1], equalTo("Hello"))
        editor.clearField(1)
        assertThat(editor.currentFieldStrings[1], equalTo(""))
    }

    @Test
    fun insertIntoFocusedFieldStartsAtSelection() {
        val activity = getNoteEditorAddingNote(DECK_LIST, SingleFragmentActivity::class.java)
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        val field: EditText = editor.getFieldForTest(0)
        editor.insertStringInField(field, "Hello")
        field.setSelection(3)
        editor.insertStringInField(field, "World")
        assertThat(editor.getFieldForTest(0).text.toString(), equalTo("HelWorldlo"))
    }

    @Test
    fun insertIntoFocusedFieldReplacesSelection() {
        val activity = getNoteEditorAddingNote(DECK_LIST, SingleFragmentActivity::class.java)
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        val field: EditText = editor.getFieldForTest(0)
        editor.insertStringInField(field, "12345")
        field.setSelection(2, 3) // select "3"
        editor.insertStringInField(field, "World")
        assertThat(editor.getFieldForTest(0).text.toString(), equalTo("12World45"))
    }

    @Test
    fun insertIntoFocusedFieldReplacesSelectionIfBackwards() {
        // selections can be backwards if the user uses keyboards
        val activity = getNoteEditorAddingNote(DECK_LIST, SingleFragmentActivity::class.java)
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        val field: EditText = editor.getFieldForTest(0)
        editor.insertStringInField(field, "12345")
        field.setSelection(3, 2) // select "3" (right to left)
        editor.insertStringInField(field, "World")
        assertThat(editor.getFieldForTest(0).text.toString(), equalTo("12World45"))
    }

    @Test
    fun defaultsToCapitalized() {
        // Requested in #3758, this seems like a sensible default
        val activity = getNoteEditorAddingNote(DECK_LIST, SingleFragmentActivity::class.java)
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        assertThat("Fields should have their first word capitalized by default", editor.getFieldForTest(0).isCapitalized, equalTo(true))
    }

    @Test
    fun pasteHtmlAsPlainTextTest() {
        val activity = getNoteEditorAddingNote(DECK_LIST, SingleFragmentActivity::class.java)
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        editor.setCurrentlySelectedModel(col.notetypes.byName("Basic")!!.getLong("id"))
        val field = editor.getFieldForTest(0)
        field.clipboard!!.setPrimaryClip(ClipData.newHtmlText("text", "text", """<span style="color: red">text</span>"""))
        assertTrue(field.clipboard!!.hasPrimaryClip())
        assertNotNull(field.clipboard!!.primaryClip)

        // test pasting in the middle (cursor mode: selecting)
        editor.setField(0, "012345")
        field.setSelection(1, 2) // selecting "1"
        assertTrue(field.pastePlainText())
        assertEquals("0text2345", field.fieldText)
        assertEquals(5, field.selectionStart)
        assertEquals(5, field.selectionEnd)

        // test pasting in the middle (cursor mode: selecting backwards)
        editor.setField(0, "012345")
        field.setSelection(2, 1) // selecting "1"
        assertTrue(field.pastePlainText())
        assertEquals("0text2345", field.fieldText)
        assertEquals(5, field.selectionStart)
        assertEquals(5, field.selectionEnd)

        // test pasting in the middle (cursor mode: normal)
        editor.setField(0, "012345")
        field.setSelection(4) // after "3"
        assertTrue(field.pastePlainText())
        assertEquals("0123text45", field.fieldText)
        assertEquals(8, field.selectionStart)
        assertEquals(8, field.selectionEnd)

        // test pasting at the start
        editor.setField(0, "012345")
        field.setSelection(0) // before "0"
        assertTrue(field.pastePlainText())
        assertEquals("text012345", field.fieldText)
        assertEquals(4, field.selectionStart)
        assertEquals(4, field.selectionEnd)

        // test pasting at the end
        editor.setField(0, "012345")
        field.setSelection(6) // after "5"
        assertTrue(field.pastePlainText())
        assertEquals("012345text", field.fieldText)
        assertEquals(10, field.selectionStart)
        assertEquals(10, field.selectionEnd)
    }

    @Test
    fun `can open with corrupt current deck - Issue 14096`() {
        col.config.set(CURRENT_DECK, '"' + "1688546411954" + '"')
        getNoteEditorAddingNote(DECK_LIST, SingleFragmentActivity::class.java).apply {
            val editor = this.supportFragmentManager.fragments.first() as NoteEditor
            assertThat("current deck is default after corruption", editor.deckId, equalTo(DEFAULT_DECK_ID))
        }
    }

    @Test
    fun `can switch two image occlusion note types 15579`() {
        val otherOcclusion = getSecondImageOcclusionNoteType()
        getNoteEditorAdding(NoteType.IMAGE_OCCLUSION).build().apply {
            val noteEditor = this.supportFragmentManager.fragments.first() as NoteEditor
            val position = requireNotNull(noteEditor.noteTypeSpinner!!.getItemIndex(otherOcclusion.name)) { "could not find ${otherOcclusion.name}" }
            noteEditor.noteTypeSpinner!!.setSelection(position)
        }
    }

    @Test
    fun `edit note in filtered deck from reviewer - 15919`() {
        // TODO: As a future extension, the filtered deck should be displayed
        // in the UI
        addDeck("A")

        // by default, the first deck is selected, so move the card to the second deck
        val homeDeckId = addDeck("B", setAsSelected = true)
        val note = addNoteUsingBasicModel().updateCards { did = homeDeckId }
        moveToDynamicDeck(note)

        // ensure note is correctly setup
        assertThat("home deck", note.firstCard().oDid, equalTo(homeDeckId))
        assertThat("current deck", note.firstCard().did, not(equalTo(homeDeckId)))

        // act
        val activity = getNoteEditorEditingExistingBasicNote(note, REVIEWER, SingleFragmentActivity::class.java)
        val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
        // assert
        assertThat("current deck is the home deck", editor.deckId, equalTo(homeDeckId))
        assertThat("no unsaved changes", !editor.hasUnsavedChanges())
    }

    @Test
    fun `decide by note type preference - 13931`() = runTest {
        col.config.setBool(ConfigKey.Bool.ADDING_DEFAULTS_TO_CURRENT_DECK, false)
        addDeck("Basic")
        val reversedDeckId = addDeck("Reversed", setAsSelected = true)

        assertThat("setup: deckId", col.notetypes.byName("Basic")!!.did, equalTo(1))

        getNoteEditorAdding(NoteType.BASIC).build().also { activity ->
            val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
            editor.onDeckSelected(SelectableDeck(reversedDeckId, "Reversed"))
            editor.setField(0, "Hello")
            editor.saveNote()
        }

        col.notetypes._clear_cache()

        assertThat("a note was added", col.noteCount(), equalTo(1))
        assertThat("note type deck is updated", col.notetypes.byName("Basic")!!.did, equalTo(reversedDeckId))

        getNoteEditorAdding(NoteType.BASIC).build().also { activity ->
            val editor = activity.supportFragmentManager.fragments.first() as NoteEditor
            assertThat("Deck ID is remembered", editor.deckId, equalTo(reversedDeckId))
        }
    }

    @Test
    fun `editing card in filtered deck retains deck`() = runTest {
        val homeDeckId = addDeck("A")
        val note = addNoteUsingBasicModel().updateCards { did = homeDeckId }
        moveToDynamicDeck(note)

        // ensure note is correctly setup
        assertThat("home deck", note.firstCard().oDid, equalTo(homeDeckId))
        assertThat("current deck", note.firstCard().did, not(equalTo(homeDeckId)))

        getNoteEditorEditingExistingBasicNote(note, REVIEWER, SingleFragmentActivity::class.java).apply {
            val editor = this.supportFragmentManager.fragments.first() as NoteEditor
            editor.setField(0, "Hello")
            editor.saveNote()
        }

        // ensure note is correctly setup
        assertThat("after: home deck", note.firstCard().oDid, equalTo(homeDeckId))
        assertThat("after: current deck", note.firstCard().did, not(equalTo(homeDeckId)))
    }

    private fun moveToDynamicDeck(note: Note): DeckId {
        val dyn = addDynamicDeck("All")
        col.decks.select(dyn)
        col.sched.rebuildDyn()
        assertThat("card is in dynamic deck", note.firstCard().did, equalTo(dyn))
        return dyn
    }

    private fun getSecondImageOcclusionNoteType(): NotetypeJson {
        val imageOcclusionNotes = col.notetypes.filter { it.isImageOcclusion }
        return if (imageOcclusionNotes.size >= 2) {
            imageOcclusionNotes.first { it.name != "Image Occlusion" }
        } else {
            col.notetypes.byName("Image Occlusion")!!.createClone()
        }
    }

    private fun getCopyNoteIntent(editor: NoteEditor): Intent {
        val editorShadow = shadowOf(editor.requireActivity())
        editor.copyNote()
        return editorShadow.peekNextStartedActivityForResult().intent
    }

    private fun Spinner.getItemIndex(toFind: Any): Int? {
        for (i in 0 until count) {
            if (this.getItemAtPosition(i) != toFind) continue
            return i
        }
        return null
    }

    private val cardCount: Int
        get() = col.cardCount()

    private fun getNoteEditorAdding(noteType: NoteType): NoteEditorTestBuilder {
        val n = makeNoteForType(noteType)
        return NoteEditorTestBuilder(n)
    }

    private fun makeNoteForType(noteType: NoteType): NotetypeJson? {
        return when (noteType) {
            NoteType.BASIC -> col.notetypes.byName("Basic")
            NoteType.CLOZE -> col.notetypes.byName("Cloze")
            NoteType.BACK_TO_FRONT -> {
                val name = super.addNonClozeModel("Reversed", arrayOf("Front", "Back"), "{{Back}}", "{{Front}}")
                col.notetypes.byName(name)
            }
            NoteType.THREE_FIELD_INVALID_TEMPLATE -> {
                val name = super.addNonClozeModel("Invalid", arrayOf("Front", "Back", "Side"), "", "")
                col.notetypes.byName(name)
            }
            NoteType.IMAGE_OCCLUSION -> col.notetypes.byName("Image Occlusion")
        }
    }

    private fun openAdvancedTextEditor(n: NoteEditor, fieldIndex: Int) {
        n.startAdvancedTextEditor(fieldIndex)
    }

    private fun enterTextIntoField(n: NoteEditor, i: Int, newText: String) {
        n.setFieldValueFromUi(i, newText)
    }

    private fun <T : SingleFragmentActivity?> getNoteEditorAddingNote(from: FromScreen, clazz: Class<T>): T {
        ensureCollectionLoadIsSynchronous()
        val i = Intent()
        val args = Bundle().apply {
            when (from) {
                REVIEWER -> putInt(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_REVIEWER_ADD)
                DECK_LIST -> putInt(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_DECKPICKER)
            }
        }
        i.putExtra(SingleFragmentActivity.FRAGMENT_NAME_EXTRA, NoteEditor::class.jvmName)
        i.putExtra(SingleFragmentActivity.FRAGMENT_ARGS_EXTRA, args)
        return super.startActivityNormallyOpenCollectionWithIntent(clazz, i)
    }

    private fun getNoteEditorEditingExistingBasicNote(front: String, back: String, from: FromScreen): SingleFragmentActivity {
        val n = super.addNoteUsingBasicModel(front, back)
        return getNoteEditorEditingExistingBasicNote(n, from, SingleFragmentActivity::class.java)
    }

    private fun <T : SingleFragmentActivity?> getNoteEditorEditingExistingBasicNote(n: Note, from: FromScreen, clazz: Class<T>): T {
        var i = Intent()
        if (from == REVIEWER) {
            i = EditCardDestination(n.firstCard().id).toIntent(targetContext, DEFAULT)
        }
        i.putExtra(SingleFragmentActivity.FRAGMENT_NAME_EXTRA, NoteEditor::class.jvmName)
        val args = Bundle().apply {
            when (from) {
                REVIEWER -> {
                    putInt(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_EDIT)
                    putLong(NoteEditor.EXTRA_CARD_ID, n.firstCard().id)
                    putParcelable(AnkiActivity.FINISH_ANIMATION_EXTRA, DEFAULT as Parcelable)
                }
                DECK_LIST -> putInt(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_DECKPICKER)
            }
        }
        i.putExtra(SingleFragmentActivity.FRAGMENT_ARGS_EXTRA, args)
        return super.startActivityNormallyOpenCollectionWithIntent(clazz, i)
    }

    private enum class FromScreen {
        DECK_LIST, REVIEWER
    }

    /** We don't use constants here to allow for additional note types to be defined  */
    private enum class NoteType {
        BASIC, CLOZE,

        /**Basic, but Back is on the front  */
        BACK_TO_FRONT, THREE_FIELD_INVALID_TEMPLATE,
        IMAGE_OCCLUSION
        ;
    }

    inner class NoteEditorTestBuilder(notetype: NotetypeJson?) {
        private val notetype: NotetypeJson
        private var firstField: String? = null
        private var secondField: String? = null
        fun build(): SingleFragmentActivity {
            val editor = build(SingleFragmentActivity::class.java)
            advanceRobolectricLooper()
            advanceRobolectricLooper()
            advanceRobolectricLooper()
            advanceRobolectricLooper()
            // 4 is insufficient
            advanceRobolectricLooper()
            advanceRobolectricLooper()
            return editor
        }

        fun <T : SingleFragmentActivity?> build(clazz: Class<T>): T {
            col.notetypes.setCurrent(notetype)
            val activity = getNoteEditorAddingNote(REVIEWER, clazz)!!
            val noteEditor = activity.supportFragmentManager.fragments.first() as NoteEditor
            advanceRobolectricLooper()
            // image occlusion does not need a first field
            if (this.firstField != null) {
                noteEditor.setFieldValueFromUi(0, firstField)
            }
            if (secondField != null) {
                noteEditor.setFieldValueFromUi(1, secondField)
            }
            return activity
        }

        fun withNoFirstField(): NoteEditorTestBuilder {
            return this
        }

        fun withFirstField(text: String?): NoteEditorTestBuilder {
            firstField = text
            return this
        }

        fun withSecondField(text: String?): NoteEditorTestBuilder {
            secondField = text
            return this
        }

        init {
            assertNotNull(notetype) { "model was null" }
            this.notetype = notetype
        }
    }
}
