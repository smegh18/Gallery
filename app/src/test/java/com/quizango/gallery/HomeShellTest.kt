package com.quizango.gallery

import com.quizango.gallery.ui.home.HOME_TABS
import com.quizango.gallery.ui.home.HomeUiState
import com.quizango.gallery.ui.home.MoreMenuRow
import com.quizango.gallery.ui.home.PAGE_ALBUMS
import com.quizango.gallery.ui.home.PAGE_EXPLORE
import com.quizango.gallery.ui.home.PAGE_PHOTOS
import com.quizango.gallery.ui.home.TopBarSlot
import com.quizango.gallery.ui.home.moreMenuRows
import com.quizango.gallery.ui.home.topBarSlots
import com.quizango.gallery.ui.home.clampColumnCount
import com.quizango.gallery.ui.home.DeleteAction
import com.quizango.gallery.ui.home.MOVE_TO_RECYCLE_BIN_DEFAULT
import com.quizango.gallery.ui.home.deleteActionFor
import com.quizango.gallery.ui.home.settledBarOffset
import com.quizango.gallery.ui.home.barSettleEasing
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeShellTest {

    @Test
    fun tabsAreInReferenceOrder() {
        assertEquals(
            listOf(R.string.home_shell_photos, R.string.home_shell_album, R.string.explore),
            HOME_TABS.map { it.title }
        )
        assertEquals(
            listOf(R.string.home_shell_photos, R.string.albums, R.string.explore),
            HOME_TABS.map { it.label }
        )
    }

    @Test
    fun opensOnPhotos() {
        assertEquals(PAGE_PHOTOS, HomeUiState().selectedTab)
    }

    @Test
    fun columnCountStaysWithinThreeToFive() {
        assertEquals(listOf(3, 3, 4, 5, 5), listOf(2, 3, 4, 5, 9).map { clampColumnCount(it) })
        assertEquals(3, HomeUiState().columnCount)
    }

    @Test
    fun firstSyncStartsUnfinished() {
        assertEquals(false, HomeUiState().initialSyncDone)
    }

    @Test
    fun topBarSlotsPerTab() {
        assertEquals(listOf(TopBarSlot.AD_FREE, TopBarSlot.SEARCH, TopBarSlot.CAMERA, TopBarSlot.MORE), topBarSlots(PAGE_PHOTOS, true))
        assertEquals(listOf(TopBarSlot.AD_FREE, TopBarSlot.ADD_ALBUM, TopBarSlot.SEARCH, TopBarSlot.MORE), topBarSlots(PAGE_ALBUMS, true))
        assertEquals(listOf(TopBarSlot.AD_FREE, TopBarSlot.SEARCH), topBarSlots(PAGE_EXPLORE, true))
    }

    @Test
    fun addAlbumAndMoreNeedMediaPermission() {
        assertEquals(listOf(TopBarSlot.AD_FREE, TopBarSlot.SEARCH, TopBarSlot.CAMERA), topBarSlots(PAGE_PHOTOS, false))
        assertEquals(listOf(TopBarSlot.AD_FREE, TopBarSlot.SEARCH), topBarSlots(PAGE_ALBUMS, false))
    }

    @Test
    fun sortRowIsOnPhotosOnly() {
        assertEquals(listOf(MoreMenuRow.SORT, MoreMenuRow.RECYCLE_BIN, MoreMenuRow.SETTINGS), moreMenuRows(PAGE_PHOTOS))
        assertEquals(listOf(MoreMenuRow.RECYCLE_BIN, MoreMenuRow.SETTINGS), moreMenuRows(PAGE_ALBUMS))
    }

    @Test
    fun deleteCheckboxMapsToDeleteKind() {
        assertEquals(DeleteAction.MOVE_TO_RECYCLE_BIN, deleteActionFor(moveToRecycleBinChecked = true))
        assertEquals(DeleteAction.DELETE_PERMANENTLY, deleteActionFor(false))
    }

    @Test
    fun recycleBinIsCheckedByDefault() {
        // S272/oa/j.smali: setChecked(true) in onCreate
        assertEquals(true, MOVE_TO_RECYCLE_BIN_DEFAULT)
    }

    @Test
    fun barSettlesHiddenOnlyPastHalf() {
        assertEquals(0f, settledBarOffset(offset = 30f, height = 100f), 0f)
        assertEquals(0f, settledBarOffset(50f, 100f), 0f) // strictly greater than half hides
        assertEquals(100f, settledBarOffset(51f, 100f), 0f)
        assertEquals(0f, settledBarOffset(0f, 0f), 0f) // no tab row, e.g. selection mode
    }

    @Test
    fun barSettleIsEaseOutCubic() {
        assertEquals(0f, barSettleEasing(0f), 1e-6f)
        assertEquals(1f, barSettleEasing(1f), 1e-6f)
        assertEquals(0.875f, barSettleEasing(0.5f), 1e-6f)
    }
}
