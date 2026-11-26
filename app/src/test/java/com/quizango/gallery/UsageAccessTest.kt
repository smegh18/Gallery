package com.quizango.gallery

import com.quizango.gallery.ui.components.UsageAccessKind
import com.quizango.gallery.ui.components.UsageAccessKind.HOME
import com.quizango.gallery.ui.components.UsageAccessKind.RECYCLE
import com.quizango.gallery.ui.components.UsageAccessKind.STORY
import com.quizango.gallery.ui.components.UsageAccessKind.VIDEO
import com.quizango.gallery.ui.components.UsageAccessQueue
import com.quizango.gallery.ui.components.UsagePromptState
import com.quizango.gallery.ui.components.pickHomeUsagePrompt
import com.quizango.gallery.ui.components.usageAccessAllowed
import com.quizango.gallery.ui.discovery.recordStoryUsageAccessTrigger
import com.quizango.gallery.ui.recycle.recordRecycleUsageAccessTrigger
import com.quizango.gallery.ui.viewer.recordVideoUsageAccessTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageAccessTest {

    private fun state(session: Int, granted: Boolean = false, insights: Boolean = true, home: Int = -1, shown: Int = -1) =
        UsagePromptState(session, granted, insights, homeShownSession = home, shownSession = shown)

    @Test
    fun `Home prompt shows once in the remote-config sessions 1, 3, 5, 9 and 13`() {
        listOf(1, 3, 5, 9, 13).forEach { assertTrue("session $it", usageAccessAllowed(HOME, state(it))) }
        listOf(0, 2, 4, 6, 7, 8, 10, 12, 14).forEach { assertFalse("session $it", usageAccessAllowed(HOME, state(it))) }
        assertFalse(usageAccessAllowed(HOME, state(3, home = 3))) // already shown this session
        assertTrue(usageAccessAllowed(HOME, state(5, home = 3)))
        assertTrue(usageAccessAllowed(HOME, state(5, home = 3, shown = 5))) // the feature prompts' key never blocks Home
    }

    @Test
    fun `feature prompts wait for the Home one and share one slot per session`() {
        for (kind in listOf(VIDEO, STORY, RECYCLE)) {
            assertFalse("$kind before any Home prompt", usageAccessAllowed(kind, state(2)))
            listOf(1, 2, 3, 5, 8, 10, 14).forEach { assertTrue("$kind session $it", usageAccessAllowed(kind, state(it, home = 1))) }
            listOf(4, 6, 7, 9, 11, 12, 13).forEach { assertFalse("$kind session $it", usageAccessAllowed(kind, state(it, home = 1))) }
            assertFalse(usageAccessAllowed(kind, state(3, home = 1, shown = 3))) // a feature prompt already showed this session
            assertTrue(usageAccessAllowed(kind, state(5, home = 1, shown = 3)))
        }
    }

    @Test
    fun `granted access or a Settings opt-out suppresses every prompt`() {
        UsageAccessKind.entries.forEach { kind ->
            assertFalse("$kind granted", usageAccessAllowed(kind, state(2, granted = true, home = 1)))
            assertFalse("$kind opted out", usageAccessAllowed(kind, state(2, insights = false, home = 1)))
        }
    }

    @Test
    fun `Home tries the queued prompt first, then its own`() {
        assertEquals(VIDEO, pickHomeUsagePrompt(VIDEO, state(2, home = 1)))
        assertEquals(HOME, pickHomeUsagePrompt(VIDEO, state(3, home = 1, shown = 3))) // the shared slot is used: Home's own
        assertEquals(HOME, pickHomeUsagePrompt(STORY, state(1))) // no Home prompt yet: the story one waits, Home shows
        assertEquals(HOME, pickHomeUsagePrompt(null, state(1)))
        assertNull(pickHomeUsagePrompt(RECYCLE, state(4, home = 3)))
        assertNull(pickHomeUsagePrompt(null, state(3, granted = true, home = 1)))
    }

    @Test
    fun `story, video and Recycle Bin exits queue their prompt for Home`() {
        UsageAccessQueue.take()
        recordStoryUsageAccessTrigger()
        assertEquals(STORY, UsageAccessQueue.take())
        assertNull(UsageAccessQueue.take()) // taken once
        recordRecycleUsageAccessTrigger()
        recordVideoUsageAccessTrigger()
        assertEquals(VIDEO, UsageAccessQueue.take()) // one slot: the latest exit wins
    }
}
