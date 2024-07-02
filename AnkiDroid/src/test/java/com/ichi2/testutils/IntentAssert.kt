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
package com.ichi2.testutils

import android.os.Bundle
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import kotlin.test.assertNotNull

object IntentAssert {
    /**
     * Check that bundle does noe have extra key extraKey associated to value
     */
    fun doesNotHaveExtra(arguments: Bundle?, extraKey: String?) {
        val keySet = assertNotNull(arguments).keySet()
        assertThat("Intent should not have extra '$extraKey'", keySet, not(hasItem(extraKey)))
    }

    /**
     * Check that bundle has extra key extraKey associated to value.
     */
    fun hasExtra(arguments: Bundle?, extraKey: String?, value: Long) {
        val keySet = assertNotNull(arguments).keySet()
        assertThat("Intent should have extra '$extraKey'", keySet, hasItem(extraKey))

        assertThat(arguments.getLong(extraKey, -1337), equalTo(value))
    }
}
