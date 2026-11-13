package com.quizango.gallery.ui.discovery

import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import java.util.Calendar

data class Story(val id: String, val title: String, val items: List<MediaItem>)

/**
 * Home stories, as the reference HomeViewModel builds them: photos only (no video, GIF or
 * screenshot folders), at most 15 per story, empty stories dropped, ordered
 * Recent, Random, Favorite, then Timeline / Flashback.
 */
object StoryBuilder {
    private const val MAX = 15
    private val SCREENSHOT_DIRS = listOf("Pictures/Screenshot", "Pictures/Screenshots", "DCIM/Screenshot", "DCIM/Screenshots")

    fun build(media: List<MediaItem>, now: Long = System.currentTimeMillis()): List<Story> {
        val photos = media.filter { m ->
            m.mediaType != MediaType.VIDEO && !m.isAnimated && SCREENSHOT_DIRS.none { m.folderPath.contains(it) }
        }
        val newestFirst = compareByDescending<MediaItem> { it.dateModified }

        val tenDaysAgo = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DATE, -10) }.timeInMillis
        val recent = photos.filter { it.dateModified in tenDaysAgo..now }.shuffled().take(MAX).sortedWith(newestFirst)
        val random = photos.shuffled().take(MAX)
        val favorite = photos.filter { it.isFavorite }.shuffled().take(MAX).sortedWith(newestFirst)

        // (sort key, story): reference sorts by these priorities
        val stories = mutableListOf(
            0 to Story("recent", "Recent", recent),
            1 to Story("random", "Random", random),
            2 to Story("favorite", "Favorite", favorite)
        )
        for ((n, id, title) in listOf(Triple(1, "timeline", "Timeline"), Triple(3, "flashback", "Flashback"))) {
            val years = window(photos, now, n, Calendar.YEAR)
            if (years.isNotEmpty()) {
                stories += (if (n == 1) 5 else 6) to Story(id, title, years.take(MAX))
            } else {
                stories += (if (n == 1) 3 else 4) to Story(id, title, window(photos, now, n, Calendar.MONTH).take(MAX))
            }
        }
        return stories.filter { it.second.items.isNotEmpty() }.sortedBy { it.first }.map { it.second }
    }

    /** The week starting [n] years/months ago (today, time cleared); if empty, the week after it. */
    private fun window(photos: List<MediaItem>, now: Long, n: Int, unit: Int): List<MediaItem> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            add(unit, -n)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DATE, 7)
        val end = cal.timeInMillis
        val first = photos.filter { it.dateModified in start..end }
        if (first.isNotEmpty()) return first.sortedBy { it.dateModified }
        cal.add(Calendar.DATE, 7)
        return photos.filter { it.dateModified in end..cal.timeInMillis }.sortedBy { it.dateModified }
    }
}
