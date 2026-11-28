package com.quizango.gallery

import com.quizango.gallery.data.local.mediastore.MediaStoreThumbnailFetcher.Companion.coverGrowth
import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbnailCoverTest {
    @Test
    fun tallScreenshotInASquareCropCellIsRequestedAgainLargeEnoughToCover() {
        // 9:20 screenshot fitted into 240x240 comes back 108x240; covering needs 240 wide
        assertEquals(240f / 108f, coverGrowth(108, 240, 240, 240, fill = true), 0.001f)
    }

    @Test
    fun nothingMoreWhenFittingAlreadyCoveredOrNearlyCovered() {
        assertEquals(1f, coverGrowth(108, 240, 240, 240, fill = false))
        assertEquals(1f, coverGrowth(320, 240, 240, 240, fill = true))
        assertEquals(1f, coverGrowth(232, 240, 240, 240, fill = true)) // 3% short
        assertEquals(1f, coverGrowth(0, 0, 240, 240, fill = true))
    }
}
