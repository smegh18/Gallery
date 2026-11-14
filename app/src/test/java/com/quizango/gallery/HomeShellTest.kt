package com.quizango.gallery

import com.quizango.gallery.ui.home.HOME_TABS
import com.quizango.gallery.ui.home.HomeUiState
import com.quizango.gallery.ui.home.PAGE_PHOTOS
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
}
