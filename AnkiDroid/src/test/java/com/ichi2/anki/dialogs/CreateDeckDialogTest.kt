/****************************************************************************************
 * Copyright (c) 2021 Akshay Jadhav <jadhavakshay0701@gmail.com>                        *
 * Copyright (c) 2024 David Allison <davidallisongithub@gmail.com>                      *
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

package com.ichi2.anki.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.IntroductionActivity
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.dialogs.CreateDeckDialog.DeckDialogType
import com.ichi2.anki.dialogs.utils.input
import com.ichi2.anki.libanki.DeckId
import com.ichi2.utils.getInputTextLayout
import com.ichi2.utils.positiveButton
import okhttp3.internal.closeQuietly
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class CreateDeckDialogTest : RobolectricTest() {
    private lateinit var activityScenario: ActivityScenario<DeckPicker>

    override fun setUp() {
        super.setUp()
        getPreferences().edit { putBoolean(IntroductionActivity.INTRODUCTION_SLIDES_SHOWN, true) }
        ensureCollectionLoadIsSynchronous()
        activityScenario =
            ActivityScenario.launch(DeckPicker::class.java).apply {
                moveToState(Lifecycle.State.STARTED)
            }
    }

    override fun tearDown() {
        super.tearDown()
        activityScenario.closeQuietly()
    }

    @Test
    fun testCreateFilteredDeckFunction() {
        val deckName = "filteredDeck"
        ensureExecutionOfScenario(DeckDialogType.FILTERED_DECK) { createDeckDialog, assertionCalled ->
            createDeckDialog.onNewDeckCreated = { id: DeckId ->
                // a deck was created
                assertThat(id, equalTo(col.decks.id(deckName)))
                assertionCalled()
            }
            createDeckDialog.createFilteredDeck(deckName)
        }
    }

    @Test
    fun testCreateSubDeckFunction() {
        val deckParentId = col.decks.id("Deck Name")
        val deckName = "filteredDeck"
        ensureExecutionOfScenario(DeckDialogType.SUB_DECK, deckParentId) { createDeckDialog, assertionCalled ->
            createDeckDialog.onNewDeckCreated = { id: DeckId ->
                val deckNameWithParentName = col.decks.getSubdeckName(deckParentId, deckName)
                assertThat(id, equalTo(col.decks.id(deckNameWithParentName!!)))
                assertionCalled()
            }
            createDeckDialog.createSubDeck(deckParentId, deckName)
        }
    }

    @Test
    fun testCreateDeckFunction() {
        val deckName = "Deck Name"
        ensureExecutionOfScenario(DeckDialogType.DECK) { createDeckDialog, assertionCalled ->
            createDeckDialog.onNewDeckCreated = { id: DeckId ->
                // a deck was created
                assertThat(id, equalTo(col.decks.byName(deckName)!!.getLong("id")))
                assertionCalled()
            }
            createDeckDialog.createDeck(deckName)
        }
    }

    @Test
    fun testCreateDeckWithQuotes() {
        val deckNameWithQuotes = "New \"Quoted\" Deck"
        ensureExecutionOfScenario(DeckDialogType.DECK) { createDeckDialog, assertionCalled ->
            createDeckDialog.onNewDeckCreated = { id: DeckId ->
                // Verify that quotes are preserved in deck names when creating
                assertThat(
                    "Quotes should be preserved in deck names when creating",
                    col.decks.name(id),
                    equalTo(deckNameWithQuotes),
                )
                assertionCalled()
            }
            createDeckDialog.createDeck(deckNameWithQuotes)
        }
    }

    @Test
    fun testRenameDeckFunction() {
        val deckName = "Deck Name"
        val deckNewName = "New Deck Name"
        ensureExecutionOfScenario(DeckDialogType.RENAME_DECK) { createDeckDialog, assertionCalled ->
            createDeckDialog.deckName = deckName
            createDeckDialog.onNewDeckCreated = { id: DeckId ->
                // a deck name was renamed
                assertThat(deckNewName, equalTo(col.decks.name(id)))
                assertionCalled()
            }
            createDeckDialog.renameDeck(deckNewName)
        }
    }

    @Test
    fun testRenameDeckWithQuotes() {
        val deckName = "Deck Name"
        val deckNewNameWithQuotes = "New \"Quoted\" Deck Name"
        ensureExecutionOfScenario(DeckDialogType.RENAME_DECK) { createDeckDialog, assertionCalled ->
            createDeckDialog.deckName = deckName
            createDeckDialog.onNewDeckCreated = { id: DeckId ->
                // Verify that the quotes are preserved in the renamed deck
                assertThat(
                    "Quotes should be preserved in deck names when renaming",
                    col.decks.name(id),
                    equalTo(deckNewNameWithQuotes),
                )
                assertionCalled()
            }
            createDeckDialog.renameDeck(deckNewNameWithQuotes)
        }
    }

    @Test
    fun `deck ordering hint`() {
        // The correct way to order a deck is ['01', '02', '10']
        val expectedText = "If you have deck ordering issues (e.g. ‘10’ appears before ‘2’), replace ‘2’ with ‘02’"
        testDialog(DeckDialogType.DECK) {
            fun assertHelperText(
                reason: String?,
                matcher: Matcher<in CharSequence?>,
            ) = assertThat(reason, getInputTextLayout().helperText, matcher)

            input = "test"
            assertHelperText("no number suggestion if text-only", nullValue())
            input = "1. Cheese"
            assertHelperText("no number suggestion if number is less than 10", nullValue())
            input = "10. Cheese"
            assertHelperText(
                "Number suggestion if number is greater than or equal to 10",
                equalTo(expectedText),
            )
            input = "1. Cheese"
            assertHelperText("hint is removed if the number is removed", nullValue())
        }
    }

    @Test
    fun nameMayNotBeZeroLength() {
        testDialog(DeckDialogType.DECK) {
            assertThat("Ok is disabled if zero length input", positiveButton.isEnabled, equalTo(false))
            input = "NotEmpty"
            assertThat("Ok is enabled if not zero length input", positiveButton.isEnabled, equalTo(true))
            input = "A::B"
            assertThat("OK is enabled if fully qualified input provided ('A::B')", positiveButton.isEnabled, equalTo(true))
        }
    }

    @Test
    fun searchDecksIconVisibilityDeckCreationTest() =
        runTest {
            // await deckpicker
            val deckPicker =
                suspendCoroutine { coro ->
                    activityScenario.onActivity { deckPicker ->
                        coro.resume(deckPicker)
                    }
                }

            suspend fun decksCount() = withCol { decks.count() }
            val deckCounter = AtomicInteger(1)

            for (i in 0 until 10) {
                val createDeckDialog =
                    CreateDeckDialog(
                        deckPicker,
                        R.string.new_deck,
                        DeckDialogType.DECK,
                        null,
                    )
                val did =
                    suspendCoroutine { coro ->
                        createDeckDialog.onNewDeckCreated = { did: DeckId ->
                            coro.resume(did)
                        }
                        createDeckDialog.createDeck("Deck$i")
                    }
                assertEquals(deckCounter.incrementAndGet(), decksCount())

                assertEquals(deckCounter.get(), decksCount())

                updateSearchDecksIcon(deckPicker)
                assertEquals(
                    deckPicker.optionsMenuState?.searchIcon,
                    decksCount() >= 10,
                )

                // After the last deck was created, delete a deck
                if (decksCount() >= 10) {
                    deckPicker.viewModel.deleteDeck(did).join()
                    assertEquals(deckCounter.decrementAndGet(), decksCount())

                    assertEquals(deckCounter.get(), decksCount())

                    updateSearchDecksIcon(deckPicker)
                    assertFalse(deckPicker.optionsMenuState?.searchIcon ?: true)
                }
            }
        }

    private suspend fun updateSearchDecksIcon(deckPicker: DeckPicker) {
        // the icon update requires a call to refreshState() and subsequent menu
        // rebuild; access it directly instead so the test passes
        deckPicker.updateMenuState()
    }

    @Test
    fun searchDecksIconVisibilitySubdeckCreationTest() =
        runTest {
            val deckPicker =
                suspendCoroutine { coro -> activityScenario.onActivity { coro.resume(it) } }
            deckPicker.updateMenuState()
            assertEquals(deckPicker.optionsMenuState!!.searchIcon, false)
            // a single top-level deck with lots of subdecks should turn the icon on
            withCol {
                decks.id(deckTreeName(0, 10, "Deck"))
            }
            deckPicker.updateMenuState()
            assertEquals(deckPicker.optionsMenuState!!.searchIcon, true)
        }

    @Test
    fun positiveButtonEnabledOnMatchingDeckNames() {
        val previousDeckName = "Deck Name"
        testDialog(DeckDialogType.RENAME_DECK) {
            input = previousDeckName
            assertThat("no error is displayed", getInputTextLayout().error, nullValue())
        }
    }

    @Test
    fun `filtered decks - duplicate creation`() {
        fun allDeckNames() = col.decks.allNamesAndIds().map { it.name }

        fun createDeck(
            deckName: String,
            expectedReturnValue: Boolean = true,
        ) {
            withCreateDeckDialog(DeckDialogType.FILTERED_DECK) {
                onNewDeckCreated = { }
                assertThat("createFilteredDeck", createFilteredDeck(deckName), equalTo(expectedReturnValue))
            }
        }

        val duplicatedName = col.sched.getOrCreateFilteredDeck(did = 0).name

        createDeck(duplicatedName)
        assertThat("initial filtered deck created", allDeckNames(), hasItem(duplicatedName))

        createDeck(duplicatedName)
        assertThat("initial filtered deck", allDeckNames(), hasItem(duplicatedName))
        assertThat("duplicate deck is created", allDeckNames(), hasItem("$duplicatedName+"))

        repeat(9) {
            createDeck(duplicatedName)
        }

        assertThat("final duplicate deck is created", allDeckNames(), hasItem("$duplicatedName${"+".repeat(10)}"))
        createDeck(duplicatedName, expectedReturnValue = false)
    }

    /**
     * Executes [callback] on the [AlertDialog] created from [CreateDeckDialog]
     */
    private fun testDialog(
        deckDialogType: DeckDialogType,
        parentId: DeckId? = null,
        callback: (AlertDialog.() -> Unit),
    ) {
        withCreateDeckDialog(deckDialogType, parentId) {
            callback(this.showDialog())
        }
    }

    /**
     * Creates a test instance of [CreateDeckDialog]
     */
    private fun withCreateDeckDialog(
        deckDialogType: DeckDialogType,
        parentId: DeckId? = null,
        callback: (CreateDeckDialog.() -> Unit),
    ) {
        activityScenario.onActivity { activity: DeckPicker ->
            val createDeckDialog = CreateDeckDialog(activity, R.string.new_deck, deckDialogType, parentId)
            callback(createDeckDialog)
        }
    }

    /**
     * Tests a scenario with a [DeckPicker] hosting a [CreateDeckDialog].
     * The second parameter of the callback ('assertionCalled') must be called for this to pass
     */
    private fun ensureExecutionOfScenario(
        deckDialogType: DeckDialogType,
        parentId: DeckId? = null,
        callback: ((CreateDeckDialog, (() -> Unit)) -> Unit),
    ) {
        activityScenario.onActivity { activity: DeckPicker ->
            val assertionCalled = AtomicReference(false)
            callback(CreateDeckDialog(activity, R.string.new_deck, deckDialogType, parentId)) {
                assertionCalled.set(true)
            }
            assertThat("no call to assertionCalled()", assertionCalled.get(), equalTo(true))
        }
    }

    @Suppress("SameParameterValue")
    private fun deckTreeName(
        start: Int,
        end: Int,
        prefix: String,
    ): String =
        List(end - start + 1) { "${prefix}${it + start}" }
            .joinToString("::")
}

/** Test of [CreateDeckDialog] */
class CreateDeckDialogNonAndroidTest {
    @Test
    fun `number larger than nine detection`() {
        fun assertLargerThanNine(
            reason: String?,
            input: String,
            result: Boolean,
        ) = assertThat(reason, input.containsNumberLargerThanNine(), equalTo(result))

        assertLargerThanNine("empty string", "", false)
        assertLargerThanNine("text", "deck name", false)
        assertLargerThanNine("less than ten", "9. - Chemicals", false)
        assertLargerThanNine("Ten or greater", "10. - Chemicals", true)
        assertLargerThanNine("Ten or greater", "99. - Chemicals", true)
        assertLargerThanNine("zero prefix", "09. - Chemicals", false)
        assertLargerThanNine("time", "10:50:59", false)
        assertLargerThanNine("time", "Filtered Deck 22:34", false)
        assertLargerThanNine("suffix", "Deck 34", true)
    }
}
