/*
 *  Copyright (c) 2022 SanjaySargam <sargamsanjaykumar@gmail.com>
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

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.R
import com.ichi2.anki.browser.CardBrowserViewModel
import com.ichi2.anki.dialogs.BrowserOptionsDialog
import com.ichi2.anki.model.CardsOrNotes
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class BrowserOptionsDialogTest {

    @Mock
    private lateinit var viewModel: CardBrowserViewModel

    @Mock
    private lateinit var dialogView: View

    @Mock
    private lateinit var radioGroup: RadioGroup

    @Mock
    private lateinit var cardsModeRadioButton: RadioButton

    @Mock
    private lateinit var notesModeRadioButton: RadioButton

    @Mock
    private lateinit var truncateCheckBox: CheckBox

    @Mock
    private lateinit var dialogInterface: DialogInterface

    @Mock
    private lateinit var alertDialogBuilder: AlertDialog.Builder

    private lateinit var browserOptionsDialog: BrowserOptionsDialog
    private val mockViewModel: CardBrowserViewModel = mock(CardBrowserViewModel::class.java)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        val mockLayoutInflater = mock(LayoutInflater::class.java)
        val mockContext = mock(Context::class.java)

        // Mocking dialog view inflation
        `when`(LayoutInflater.from(mockContext)).thenReturn(mockLayoutInflater)
        `when`(mockLayoutInflater.inflate(anyInt(), any())).thenReturn(dialogView)

        // Mocking view initialization
        `when`(dialogView.findViewById<RadioGroup>(R.id.select_browser_mode)).thenReturn(radioGroup)
        `when`(dialogView.findViewById<RadioButton>(R.id.select_cards_mode)).thenReturn(cardsModeRadioButton)
        `when`(dialogView.findViewById<RadioButton>(R.id.select_notes_mode)).thenReturn(notesModeRadioButton)
        `when`(dialogView.findViewById<CheckBox>(R.id.truncate_checkbox)).thenReturn(truncateCheckBox)

        // Mocking dialog builder
        `when`(alertDialogBuilder.setView(dialogView)).thenReturn(alertDialogBuilder)
        `when`(alertDialogBuilder.setTitle(anyString())).thenReturn(alertDialogBuilder)
        `when`(alertDialogBuilder.setNegativeButton(anyString(), any())).thenReturn(alertDialogBuilder)
        `when`(alertDialogBuilder.setPositiveButton(anyString(), any())).thenReturn(alertDialogBuilder)
        `when`(alertDialogBuilder.create()).thenReturn(mock(AlertDialog::class.java))

        browserOptionsDialog = BrowserOptionsDialog(CardsOrNotes.CARDS, false).apply {
            viewModel = mockViewModel
        }
    }

    @Test
    fun testPositiveButtonClick() {
        // Set up mock behavior for radio group
        `when`(radioGroup.checkedRadioButtonId).thenReturn(R.id.select_cards_mode)

        // Set up mock behavior for checkbox
        `when`(truncateCheckBox.isChecked).thenReturn(true)

        // Call the positiveButtonClick function
        browserOptionsDialog.positiveButtonClick(dialogInterface, 0)

        // Verify if the viewModel methods are called with correct parameters
        verify(viewModel).setCardsOrNotes(CardsOrNotes.CARDS)
        verify(viewModel).setTruncated(true)
    }
}
