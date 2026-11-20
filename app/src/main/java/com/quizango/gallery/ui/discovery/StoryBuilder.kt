package com.quizango.gallery.ui.discovery

import androidx.annotation.StringRes
import com.quizango.gallery.R
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import java.util.Calendar

data class Story(val id: String, @StringRes val titleRes: Int, val items: List<MediaItem>)

/**
 * Home stories, as the reference HomeViewModel builds them: photos only (no video, GIF or
 * screenshot folders), at most 15 per story, empty stories dropped. Row order comes from the
 * reference sort values: Recent 0, Memories 2/4, Flashback 3/5, Random 5, Favorite 6.
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

        // (sort value, story) in the reference's insertion order; the sort is stable
        val stories = mutableListOf(0 to Story("recent", R.string.recent, recent))
        for ((n, id, title) in listOf(Triple(1, "memories", R.string.album_tab_memories), Triple(3, "flashback", R.string.flash_back))) {
            val years = window(photos, now, n, Calendar.YEAR)
            stories += if (years.isNotEmpty()) {
                (if (n == 1) 4 else 5) to Story(id, title, years.take(MAX))
            } else {
                (if (n == 1) 2 else 3) to Story(id, title, window(photos, now, n, Calendar.MONTH).take(MAX))
            }
        }
        stories += 5 to Story("random", R.string.random, random)
        stories += 6 to Story("favorite", R.string.favorite, favorite)
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
