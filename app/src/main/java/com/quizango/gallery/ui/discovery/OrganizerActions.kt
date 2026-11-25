package com.quizango.gallery.ui.discovery

import com.quizango.gallery.data.local.PathMutationOutcome

/** History is deliberately keyed by immutable media paths, never by a pager position. */
internal sealed interface OrganizerAction {
    val path: String

    data class Skip(override val path: String) : OrganizerAction
    data class Trash(override val path: String) : OrganizerAction
    data class Move(override val path: String, val source: String) : OrganizerAction
}

internal data class OrganizerActionState(
    val history: List<OrganizerAction> = emptyList(),
    val currentPath: String? = null,
    val completedPaths: Set<String> = emptySet()
)

internal fun recordOrganizerAction(
    state: OrganizerActionState,
    action: OrganizerAction,
    outcome: PathMutationOutcome
): OrganizerActionState = if (outcome.succeeded) {
    state.copy(history = state.history + action, completedPaths = state.completedPaths + action.path)
} else state

internal fun undoOrganizerAction(
    state: OrganizerActionState,
    expected: OrganizerAction,
    outcome: PathMutationOutcome
): OrganizerActionState = if (outcome.succeeded && state.history.lastOrNull() == expected) {
    state.copy(history = state.history.dropLast(1), currentPath = expected.path, completedPaths = state.completedPaths - expected.path)
} else state

internal fun organizerActionTargets(paths: List<String>): List<String> = paths.distinct()

internal fun nextOrganizerPath(visiblePaths: List<String>, currentPath: String): String? {
    val index = visiblePaths.indexOf(currentPath)
    if (index < 0) return visiblePaths.firstOrNull()
    return visiblePaths.getOrNull(index + 1) ?: visiblePaths.getOrNull(index - 1)
}

/** 2.7.2 oo0o0Oo: a swipe on the tutorial layer only advances it (left 5 -> 6, up 6 -> 7); no photo is skipped or trashed. */
internal fun nextOrganizerTutorialStep(step: Int, swipe: OrganizerSwipe): Int = when {
    step == 5 && swipe == OrganizerSwipe.LEFT -> 6
    step == 6 && swipe == OrganizerSwipe.UP -> 7
    else -> step
}

internal fun organizerTutorialStepAfterCreate(step: Int, created: Boolean): Int =
    if (step == 3 && created) 4 else step

internal fun organizerTutorialStepAfterAlbumTap(step: Int, albumIndex: Int): Int =
    if (step == 4 && albumIndex == 1) 5 else step

internal enum class OrganizerSwipe { LEFT, UP, RIGHT, DOWN, NONE }

internal fun organizerSwipeFromDelta(dx: Float, dy: Float, threshold: Float = 80f): OrganizerSwipe {
    if (kotlin.math.abs(dx) < threshold && kotlin.math.abs(dy) < threshold) return OrganizerSwipe.NONE
    return if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
        if (dx < 0) OrganizerSwipe.LEFT else OrganizerSwipe.RIGHT
    } else if (dy < 0) OrganizerSwipe.UP else OrganizerSwipe.DOWN
}

internal fun organizerOwnedPathsAfterReconcile(owned: Set<String>, recycle: Set<String>, limit: Int = 2000): Set<String> =
    (owned intersect recycle).take(limit.coerceIn(1, 2000)).toSet()

internal fun organizerStripeEligible(bucketId: String, dismissed: Boolean): Boolean =
    bucketId == "filter:all_photos" && !dismissed

internal fun organizerMoveAction(sourcePath: String, actualDestinationPath: String): OrganizerAction.Move =
    OrganizerAction.Move(path = actualDestinationPath, source = sourcePath)

internal fun organizerMoveUndoFolder(action: OrganizerAction.Move): String =
    java.io.File(action.source).parent.orEmpty()
