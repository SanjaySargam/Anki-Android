/*
 *  Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>
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

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.NoteEditor.Companion.intentLaunchedWithImage
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.GrantStoragePermission
import com.ichi2.testutils.common.Flaky
import com.ichi2.testutils.common.OS
import com.ichi2.utils.AssetHelper.TEXT_PLAIN
import junit.framework.TestCase.assertFalse
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class NoteEditorIntentTest : InstrumentedTest() {
    @get:Rule
    var runtimePermissionRule: TestRule? = GrantStoragePermission.instance

    @get:Rule
    var activityRuleIntent: ActivityScenarioRule<SingleFragmentActivity>? = ActivityScenarioRule(
        noteEditorTextIntent
    )

    @Test
    @Flaky(OS.ALL, "Issue 15707 - java.lang.ArrayIndexOutOfBoundsException: length=0; index=0")
    fun launchActivityWithIntent() {
        val scenario = activityRuleIntent!!.scenario
        scenario.moveToState(Lifecycle.State.RESUMED)
        onActivity(scenario) { activity ->
            val editor = activity.supportFragmentManager.findFragmentById(R.id.fragment_container) as NoteEditor
            val currentFieldStrings = editor.currentFieldStrings
            MatcherAssert.assertThat(currentFieldStrings[0], Matchers.equalTo("sample text"))
        }
    }

    @Test
    fun intentLaunchedWithNonImageIntent() {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = TEXT_PLAIN
        }
        assertFalse(intentLaunchedWithImage(intent))
    }

    private val noteEditorTextIntent: Intent
        get() {
            val bundle = Bundle().apply {
                putString(Intent.EXTRA_TEXT, "sample text")
                putString("action", Intent.ACTION_SEND)
            }
            return NoteEditor.getIntent(testContext, bundle)
        }
}

@Throws(Throwable::class)
private fun onActivity(
    scenario: ActivityScenario<SingleFragmentActivity>,
    noteEditorActivityAction: ActivityScenario.ActivityAction<SingleFragmentActivity>
) {
    val wrapped = AtomicReference<Throwable?>(null)
    scenario.onActivity { a: SingleFragmentActivity ->
        try {
            noteEditorActivityAction.perform(a)
        } catch (t: Throwable) {
            wrapped.set(t)
        }
    }
    wrapped.get()?.let { throw it }
}
