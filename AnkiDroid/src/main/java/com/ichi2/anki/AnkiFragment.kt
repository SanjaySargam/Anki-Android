/*
 Copyright (c) 2024 Sanjay Sargam <sargamsanjaykumar@gmail.com>

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
package com.ichi2.anki

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.ThemeUtils
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.ichi2.anki.workarounds.AppLoadedFromBackupWorkaround.showedActivityFailedScreen
import com.ichi2.async.CollectionLoader
import com.ichi2.libanki.Collection
import com.ichi2.utils.increaseHorizontalPaddingOfOverflowMenuIcons
import com.ichi2.utils.tintOverflowMenuIcons
import timber.log.Timber
/**
 * Base class for fragments in the AnkiDroid.
 * This class provides common functionality and convenience methods for all fragments that extend it.
 *
 * Why Extend AnkiFragment:
 * - **Consistency**: Provides a consistent setup for fragments, ensuring they all handle common tasks in the same way.
 * - **Helper Methods**: Contains helper methods to reduce boilerplate code in descendant classes.
 * - **Common Initialization**: Ensures fragments have consistent initialization, such as setting navigation bar colors or checking storage permissions.
 *
 * @param layout Resource ID of the layout to be used for this fragment.
 */
open class AnkiFragment(@LayoutRes layout: Int) : Fragment(layout) {

    val getColUnsafe: Collection
        get() = CollectionManager.getColUnsafe()

    lateinit var ankiActivity: AnkiActivity

    lateinit var mainToolbar: Toolbar

    private fun colIsOpenUnsafe(): Boolean {
        return CollectionManager.isOpenUnsafe()
    }

    protected fun animationEnabled(): Boolean {
        return !ankiActivity.animationDisabled()
    }

    protected fun openUrl(url: Uri) {
        ankiActivity.openUrl(url)
    }

    protected suspend fun userAcceptsSchemaChange(): Boolean {
        return ankiActivity.userAcceptsSchemaChange()
    }

    protected fun enableToolbar(view: View) {
        mainToolbar = view.findViewById(R.id.toolbar)
    }

    fun <T : View> findViewById(id: Int): T {
        return requireView().findViewById(id)
    }

    private fun hideProgressBar() {
        ankiActivity.hideProgressBar()
    }

    private fun showProgressBar() {
        ankiActivity.showProgressBar()
    }

    protected fun registerReceiver(unmountReceiver: BroadcastReceiver?, iFilter: IntentFilter) {
        requireContext().registerReceiver(unmountReceiver, iFilter)
    }

    protected fun unregisterReceiver(unmountReceiver: BroadcastReceiver?) {
        requireActivity().unregisterReceiver(unmountReceiver)
    }

    protected fun increaseHorizontalPaddingOfOverflowMenuIcons(menu: Menu) {
        requireContext().increaseHorizontalPaddingOfOverflowMenuIcons(menu)
    }

    protected fun tintOverflowMenuIcons(menu: Menu) {
        requireContext().tintOverflowMenuIcons(menu)
    }

    protected fun invalidateMenu() {
        requireActivity().invalidateMenu()
    }

    protected open fun onCollectionLoaded(col: Collection) {
        hideProgressBar()
    }

    protected fun setTitle(title: Int) {
        mainToolbar.setTitle(title)
    }

    protected open fun startLoadingCollection() {
        Timber.d("AnkiFragment.startLoadingCollection()")
        if (colIsOpenUnsafe()) {
            Timber.d("Synchronously calling onCollectionLoaded")
            onCollectionLoaded(getColUnsafe)
            return
        }
        // Open collection asynchronously if it hasn't already been opened
        showProgressBar()
        CollectionLoader.load(
            this
        ) { col: com.ichi2.libanki.Collection? ->
            if (col != null) {
                Timber.d("Asynchronously calling onCollectionLoaded")
                onCollectionLoaded(col)
            } else {
                onCollectionLoadError()
            }
        }
    }

    protected open fun showDialogFragment(newFragment: DialogFragment) {
        ankiActivity.showDialogFragment(newFragment)
    }

    protected suspend fun <T> Fragment.withProgress(
        message: String = resources.getString(R.string.dialog_processing),
        block: suspend () -> T
    ): T =
        requireActivity().withProgress(message, block)

    private fun onCollectionLoadError() {
        val deckPicker = Intent(requireContext(), DeckPicker::class.java)
        deckPicker.putExtra("collectionLoadError", true) // don't currently do anything with this
        deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(deckPicker)
    }

    /**
     * Show a failure screen if there is some initialization error.
     * If this is the case, there's no point in continuing with fragment setup.
     * By returning early, we prevent further execution and ensure that the fragment does not proceed with its setup, maintaining a safe and consistent state.
     */
    protected fun showedActivityFailedScreen(savedInstanceState: Bundle?) =
        requireActivity().showedActivityFailedScreen(
            savedInstanceState = savedInstanceState,
            activitySuperOnCreate = { state -> super.onCreate(state) }
        )

    /**
     * If storage permissions are not granted, shows a toast message and finishes the activity.
     *
     * This should be called AFTER a call to `super.`[onCreate]
     *
     * @return `true`: activity may continue to start, `false`: [onCreate] should stop executing
     * as storage permissions are mot granted
     */
    protected fun ensureStoragePermissions(): Boolean {
        if (IntentHandler.grantedStoragePermissions(requireContext(), showToast = true)) {
            return true
        }
        Timber.w("finishing activity. No storage permission")
        requireActivity().finish()
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ankiActivity = activity as AnkiActivity
        ankiActivity.setNavigationBarColor(R.attr.toolbarBackgroundColor)
        requireActivity().window.statusBarColor = ThemeUtils.getThemeAttrColor(requireContext(), R.attr.appBarColor)
        super.onViewCreated(view, savedInstanceState)
    }
}
