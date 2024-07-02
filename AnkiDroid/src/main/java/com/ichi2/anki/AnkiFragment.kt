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
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
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

    /**
     * True if the collection is open. Unsafe, as it has the potential to race.
     */
    private fun colIsOpenUnsafe(): Boolean {
        return CollectionManager.isOpenUnsafe()
    }

    /**
     * Whether animations should not be displayed
     * This is used to improve the UX for e-ink devices
     * Can be tested via Settings - Advanced - Safe display mode
     *
     * @see .animationEnabled
     */
    protected fun animationEnabled(): Boolean {
        return !ankiActivity.animationDisabled()
    }

    /**
     * Opens a URL in a custom tab, with fallback to a browser if no custom tab implementation is available.
     *
     * This method first checks if there is a web browser available on the device. If no browser is found,
     * a snackbar message is displayed informing the user. If a browser is available, a custom tab is
     * opened with customized appearance and animations.
     *
     * @param url The URI to be opened.
     */
    protected fun openUrl(url: Uri) {
        ankiActivity.openUrl(url)
    }

    /**
     * Checks if the user accepts a schema change in the Anki collection.
     *
     * This function first checks if the schema has already changed. If it has, the function returns true.
     * If the schema has not changed, it prompts the user with a dialog to accept the schema change.
     * The function is suspended until the user responds.
     *
     * If the user accepts the schema change, the schema modification is applied.
     *
     * @return true if the user accepts the schema change, false otherwise.
     */
    protected suspend fun userAcceptsSchemaChange(): Boolean {
        return ankiActivity.userAcceptsSchemaChange()
    }

    fun setNavigationBarColor(@AttrRes attr: Int) {
        requireActivity().window.navigationBarColor = ThemeUtils.getThemeAttrColor(requireContext(), attr)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ankiActivity = activity as AnkiActivity
//        ankiActivity.setNavigationBarColor(R.attr.toolbarBackgroundColor)
        requireActivity().window.statusBarColor = ThemeUtils.getThemeAttrColor(requireContext(), R.attr.appBarColor)
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Enables the toolbar by initializing the `mainToolbar` with the provided view's toolbar ID.
     *
     */
    protected fun enableToolbar() {
        mainToolbar = requireView().findViewById(R.id.toolbar)
    }

    /**
     * Finds a view in the fragment's layout by the specified ID.
     *
     * @param id The ID of the view to find.
     */
    fun <T : View> findViewById(id: Int): T {
        return requireView().findViewById(id)
    }

    /**
     * Hides progress bar after collection loaded.
     */
    private fun hideProgressBar() {
        ankiActivity.hideProgressBar()
    }

    /**
     * Shows progress until collection loaded.
     */
    private fun showProgressBar() {
        ankiActivity.showProgressBar()
    }

    /**
     * Registers a broadcast receiver with the application context using the provided intent filter.
     *
     * @param unmountReceiver The BroadcastReceiver instance to register.
     * @param iFilter The IntentFilter that specifies the types of intents the receiver should respond to.
     */
    protected fun registerReceiver(unmountReceiver: BroadcastReceiver?, iFilter: IntentFilter) {
        requireContext().registerReceiver(unmountReceiver, iFilter)
    }

    /**
     * Unregisters a previously registered broadcast receiver.
     *
     * @param unmountReceiver The BroadcastReceiver instance to unregister.
     */
    protected fun unregisterReceiver(unmountReceiver: BroadcastReceiver?) {
        requireActivity().unregisterReceiver(unmountReceiver)
    }

    /**
     * Increases the horizontal padding of overflow menu icons in the given Menu.
     */
    protected fun increaseHorizontalPaddingOfOverflowMenuIcons(menu: Menu) {
        requireContext().increaseHorizontalPaddingOfOverflowMenuIcons(menu)
    }

    /**
     * Sets color to icons of overflow menu items in given Menu.
     */
    protected fun tintOverflowMenuIcons(menu: Menu) {
        requireContext().tintOverflowMenuIcons(menu)
    }

    /**
     * Invalidates the options menu, causing it to be recreated.
     */
    protected fun invalidateMenu() {
        requireActivity().invalidateMenu()
    }

    /**
     * called when the Collection is loaded successfully.
     */
    protected open fun onCollectionLoaded(col: Collection) {
        hideProgressBar()
    }

    /**
     * Sets the title of the toolbar.
     */
    protected fun setTitle(title: Int) {
        mainToolbar.setTitle(title)
    }

    /**
     * Starts loading the Anki collection asynchronously if it hasn't been opened yet.
     * If the collection is already open, calls `onCollectionLoaded` synchronously.
     * Shows a progress bar during loading.
     */
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

    /**
     * Method to show dialog fragment including adding it to back stack Note: DO NOT call this from an async
     * task! If you need to show a dialog from an async task, use showAsyncDialogFragment()
     */
    protected open fun showDialogFragment(newFragment: DialogFragment) {
        ankiActivity.showDialogFragment(newFragment)
    }

    /**
     * Run the provided operation, showing a progress window with the provided
     * message until the operation completes.
     */
    protected suspend fun <T> Fragment.withProgress(
        message: String = resources.getString(R.string.dialog_processing),
        block: suspend () -> T
    ): T =
        requireActivity().withProgress(message, block)

    /**
     * Handles the error state when loading the Anki collection fails.
     * Starts the DeckPicker activity to manage decks.
     */
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
}
