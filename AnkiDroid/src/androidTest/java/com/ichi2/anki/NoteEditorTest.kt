/*
 *  Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>
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

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ichi2.anki.testutil.GrantStoragePermission
import com.ichi2.utils.KotlinCleanup
import org.hamcrest.Matchers.*
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule

abstract class NoteEditorTest protected constructor() {
    @get:Rule
    var runtimePermissionRule: TestRule? = GrantStoragePermission.instance

    @Before
    fun before() {
        for (invalid in invalidSdksImpl) {
            Assume.assumeThat(
                "Test fails on Travis API $invalid",
                Build.VERSION.SDK_INT,
                not(
                    equalTo(invalid)
                )
            )
        }
    }

    private val invalidSdksImpl: List<Int>
        get() {
            // TODO: Look into these assumptions and see if they can be diagnosed - both work on my emulators.
            // If we fix them, we might be able to use instrumentation.sendKeyDownUpSync
            /*
             java.lang.AssertionError: Activity never becomes requested state "[DESTROYED]" (last lifecycle transition = "PAUSED")
             at androidx.test.core.app.ActivityScenario.waitForActivityToBecomeAnyOf(ActivityScenario.java:301)
              */
            val invalid = Build.VERSION_CODES.N_MR1
            val integers = ArrayList(listOf(invalid))
            integers.addAll(invalidSdks!!)
            return integers
        }
    protected open val invalidSdks: List<Int>?
        get() = ArrayList()

    init {
        @KotlinCleanup("change to variable init")
        // Rules mean that we get a failure on API 25.
        // Even if we ignore the tests, the rules cause a failure.
        // We can't ignore the test in @BeforeClass ("Test run failed to complete. Expected 150 tests, received 149")
        // and @Before executes after the rule.
        // So, disable the rules in the constructor, and ignore in before.
        if (invalidSdksImpl.contains(Build.VERSION.SDK_INT)) {
            runtimePermissionRule = null
        }
    }

    protected fun launchFragment(): FragmentScenario<NoteEditor> {
        val fragmentArgs = Bundle().apply {
            putInt(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_DECKPICKER)
        }
        return launchFragmentInContainer<NoteEditor>(
            fragmentArgs
        )
    }
}
