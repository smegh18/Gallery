package com.quizango.gallery

import com.quizango.gallery.data.local.PathMutationOutcome
import com.quizango.gallery.data.local.successfulPaths
import com.quizango.gallery.ui.discovery.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizerTest {
    private fun outcome(path: String, success: Boolean) = PathMutationOutcome(
        path,
        if (success) PathMutationOutcome.Status.SUCCESS else PathMutationOutcome.Status.METADATA_FAILED
    )

    @Test
    fun leftSkipAdvancesWithoutRepositoryMutationAndUndoRestoresItsPath() {
        val state = OrganizerActionState(currentPath = "/a.jpg")
        val skipped = recordOrganizerAction(state, OrganizerAction.Skip("/a.jpg"), outcome("/a.jpg", true))
        assertEquals(listOf(OrganizerAction.Skip("/a.jpg")), skipped.history)
        val undone = undoOrganizerAction(skipped, skipped.history.last(), outcome("/a.jpg", true))
        assertTrue(undone.history.isEmpty())
        assertEquals("/a.jpg", undone.currentPath)
    }

    @Test
    fun trashHistoryIsRecordedOnlyAfterSuccessAndTargetsImmutablePath() {
        val state = OrganizerActionState(currentPath = "/a.jpg")
        val action = OrganizerAction.Trash("/a.jpg")
        assertEquals(state, recordOrganizerAction(state, action, outcome("/a.jpg", false)))
        val succeeded = recordOrganizerAction(state, action, outcome("/a.jpg", true))
        assertEquals(listOf(action), succeeded.history)
        assertEquals(listOf("/a.jpg", "/b.jpg"), organizerActionTargets(listOf("/a.jpg", "/b.jpg", "/a.jpg")))
    }

    @Test
    fun failedUndoKeepsHistoryAndDoesNotChangeCurrentPath() {
        val action = OrganizerAction.Trash("/a.jpg")
        val state = OrganizerActionState(history = listOf(action), currentPath = "/b.jpg")
        assertEquals(state, undoOrganizerAction(state, action, outcome("/a.jpg", false)))
    }

    @Test
    fun undoCannotPopAChangedHistoryHead() {
        val action = OrganizerAction.Trash("/a.jpg")
        val state = OrganizerActionState(history = listOf(action), currentPath = "/b.jpg")
        assertEquals(state, undoOrganizerAction(state, OrganizerAction.Skip("/other.jpg"), outcome("/other.jpg", true)))
        assertFalse(state.history.isEmpty())
    }

    @Test
    fun advanceTracksPathsAcrossSkippedEntriesAndMissingRows() {
        assertEquals("/c.jpg", nextOrganizerPath(listOf("/a.jpg", "/b.jpg", "/c.jpg"), "/b.jpg"))
        assertEquals("/a.jpg", nextOrganizerPath(listOf("/a.jpg", "/b.jpg"), "/b.jpg"))
        assertEquals("/new.jpg", nextOrganizerPath(listOf("/new.jpg"), "/removed.jpg"))
        assertEquals(null, nextOrganizerPath(emptyList(), "/removed.jpg"))
    }

    @Test
    fun tutorialSwipesAdvanceOnlyTheirOwnStep() {
        assertEquals(6, nextOrganizerTutorialStep(5, OrganizerSwipe.LEFT))
        assertEquals(5, nextOrganizerTutorialStep(5, OrganizerSwipe.UP))
        assertEquals(5, nextOrganizerTutorialStep(5, OrganizerSwipe.NONE))
        assertEquals(6, nextOrganizerTutorialStep(6, OrganizerSwipe.LEFT))
        assertEquals(7, nextOrganizerTutorialStep(6, OrganizerSwipe.UP))
        assertEquals(4, nextOrganizerTutorialStep(4, OrganizerSwipe.LEFT))
        assertEquals(7, nextOrganizerTutorialStep(7, OrganizerSwipe.UP))
    }

    @Test
    fun aMostlyLeftSwipeWithUpwardDriftSkipsAndNeverTrashes() {
        assertEquals(OrganizerSwipe.LEFT, organizerSwipeFromDelta(-300f, -85f))
        assertEquals(OrganizerSwipe.UP, organizerSwipeFromDelta(-30f, -85f))
    }

    @Test
    fun createAndPromptedAlbumTransitionsRequireTheirExactStepAndSuccess() {
        assertEquals(4, organizerTutorialStepAfterCreate(3, true))
        assertEquals(3, organizerTutorialStepAfterCreate(3, false))
        assertEquals(2, organizerTutorialStepAfterCreate(2, true))
        assertEquals(5, organizerTutorialStepAfterAlbumTap(4, 1))
        assertEquals(4, organizerTutorialStepAfterAlbumTap(4, 2))
        assertEquals(3, organizerTutorialStepAfterAlbumTap(3, 1))
    }

    @Test
    fun organizerBinOwnershipReconcilesOnlyExistingRecycleRowsAndStaysBounded() {
        assertEquals(setOf("/a.jpg"), organizerOwnedPathsAfterReconcile(setOf("/a.jpg", "/b.jpg"), setOf("/a.jpg")))
        assertEquals(2, organizerOwnedPathsAfterReconcile(setOf("/a.jpg", "/b.jpg", "/c.jpg"), setOf("/a.jpg", "/b.jpg", "/c.jpg"), 2).size)
    }

    @Test
    fun organizerRestoreAndDeleteClearSuccessfulPathsOnly() {
        val outcomes = listOf(outcome("/ok.jpg", true), outcome("/failed.jpg", false))
        assertEquals(listOf("/ok.jpg"), outcomes.successfulPaths())
        val owned = setOf("/ok.jpg", "/failed.jpg")
        assertEquals(setOf("/failed.jpg"), owned - outcomes.successfulPaths().toSet())
    }

    @Test
    fun organizerStripeIsRestrictedToAllPhotosAndOneTimeVisibility() {
        assertTrue(organizerStripeEligible("filter:all_photos", dismissed = false))
        assertFalse(organizerStripeEligible("filter:all_photos", dismissed = true))
        assertFalse(organizerStripeEligible("filter:camera", dismissed = false))
        assertFalse(organizerStripeEligible("/storage/Pictures/Trip", dismissed = false))
    }

    @Test
    fun swipeDirectionsMatchLeftSkipAndUpTrash() {
        assertEquals(OrganizerSwipe.LEFT, organizerSwipeFromDelta(-120f, 10f))
        assertEquals(OrganizerSwipe.UP, organizerSwipeFromDelta(10f, -120f))
        assertEquals(OrganizerSwipe.RIGHT, organizerSwipeFromDelta(120f, 10f))
        assertEquals(OrganizerSwipe.DOWN, organizerSwipeFromDelta(10f, 120f))
        assertEquals(OrganizerSwipe.NONE, organizerSwipeFromDelta(-20f, 20f))
    }

    @Test
    fun moveUndoTracksActualCollisionPathAndOriginalFolder() {
        val move = organizerMoveAction("/Pictures/Camera/IMG_1.jpg", "/Pictures/Family/IMG_1(1).jpg")
        assertEquals("/Pictures/Family/IMG_1(1).jpg", move.path)
        assertEquals("/Pictures/Camera/IMG_1.jpg", move.source)
        assertEquals(java.io.File("/Pictures/Camera").path, organizerMoveUndoFolder(move))
    }
}
