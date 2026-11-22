package com.quizango.gallery

import com.quizango.gallery.domain.model.Album
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.domain.model.MediaType
import com.quizango.gallery.ui.discovery.momentRoute
import com.quizango.gallery.ui.navigation.Screen
import com.quizango.gallery.ui.search.PICKER_START_HEX
import com.quizango.gallery.ui.search.SEARCH_PREVIEW_LIMIT
import com.quizango.gallery.ui.search.SEARCH_SWATCH_HEX
import com.quizango.gallery.ui.search.SearchSection
import com.quizango.gallery.ui.search.buildSearchIndex
import com.quizango.gallery.ui.search.colorOfHex
import com.quizango.gallery.ui.search.hexOf
import com.quizango.gallery.ui.search.hsvOf
import com.quizango.gallery.ui.search.searchPreview
import com.quizango.gallery.ui.search.searchResults
import com.quizango.gallery.ui.search.sectionCards
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenTest {

    private fun photo(id: Long, place: String = "", type: MediaType = MediaType.IMAGE) = MediaItem(
        id = id, filePath = "/x/$id.jpg", fileName = "$id.jpg", fileSize = 1, folderName = "x", folderPath = "/x",
        dateModified = id, dateTaken = id, placeName = place, mediaType = type
    )

    private fun album(id: String, name: String, count: Int = 1) =
        Album(id = id, name = name, path = id, coverPath = "", itemCount = count)

    // Three albums (plus a virtual one), places Paris/Berlin, moment labels Beach/Dog
    private val albums = listOf(
        album("filter:all_photos", "", 9),
        album("/dcim/Beach trip", "Beach trip"),
        album("/dcim/Work", "Work"),
        album("/dcim/Dogs", "Dogs")
    )
    private val media = listOf(photo(1, "Paris"), photo(2, "Paris"), photo(3, "Berlin"), photo(4))
    private val labels = mapOf("/x/1.jpg" to "Beach", "/x/2.jpg" to "Beach", "/x/3.jpg" to "Dog")
    private val index = buildSearchIndex(albums, media, labels)

    @Test
    fun sectionsAreAlbumsPlacesMomentsInOrderWithReferenceTypes() {
        assertEquals(listOf(SearchSection.ALBUMS, SearchSection.PLACES, SearchSection.MOMENTS), SearchSection.entries)
        assertEquals(listOf(1, 2, 3), SearchSection.entries.map { it.type })
        assertEquals(listOf(SearchSection.ALBUMS, SearchSection.PLACES, SearchSection.MOMENTS), searchResults(index, "").map { it.first })
        // A section with no match is omitted, not shown empty
        assertEquals(listOf(SearchSection.PLACES), searchResults(index, "berlin").map { it.first })
        assertTrue(searchResults(index, "zzz").isEmpty())
    }

    @Test
    fun albumsAreRealDeviceFoldersOnly() {
        val titles = sectionCards(index, SearchSection.ALBUMS, "").map { it.title }
        assertEquals(listOf("Beach trip", "Work", "Dogs"), titles)
    }

    @Test
    fun previewIsCappedAtSixButTheSectionKeepsEveryMatch() {
        val many = buildSearchIndex((1..10).map { album("/a/$it", "Album $it") }, emptyList(), emptyMap())
        val all = sectionCards(many, SearchSection.ALBUMS, "album")
        assertEquals(6, SEARCH_PREVIEW_LIMIT)
        assertEquals(10, all.size)
        assertEquals(all.take(6), searchPreview(all))
        assertEquals(3, searchPreview(sectionCards(index, SearchSection.ALBUMS, "")).size)
    }

    @Test
    fun queryFiltersEachSectionByNameCaseInsensitively() {
        assertEquals(listOf("Beach trip"), sectionCards(index, SearchSection.ALBUMS, "  BEACH ").map { it.title })
        assertEquals(listOf("Paris"), sectionCards(index, SearchSection.PLACES, "par").map { it.title })
        assertEquals(listOf("Dog"), sectionCards(index, SearchSection.MOMENTS, "dog").map { it.title })
        // "dog" also matches the album "Dogs": each section filters on its own names
        assertEquals(listOf(SearchSection.ALBUMS, SearchSection.MOMENTS), searchResults(index, "dog").map { it.first })
    }

    @Test
    fun cardsRouteToTheSameFiltersAsTheirBrowseScreens() {
        assertEquals(Screen.AlbumDetail.createRoute("/dcim/Work", "Work"), sectionCards(index, SearchSection.ALBUMS, "work").single().route)
        val paris = sectionCards(index, SearchSection.PLACES, "paris").single()
        assertEquals(Screen.AlbumDetail.createRoute("filter:place:Paris", "Paris"), paris.route)
        assertEquals(2, paris.count)
        val beach = sectionCards(index, SearchSection.MOMENTS, "beach").single()
        assertEquals(momentRoute("Beach"), beach.route)
        assertEquals(2, beach.count)
    }

    @Test
    fun momentsSkipVideosAndPlacesSkipItemsWithoutAKey() {
        val withVideo = buildSearchIndex(emptyList(), media + photo(5, "Paris", MediaType.VIDEO), labels + ("/x/5.jpg" to "Beach"))
        assertEquals(2, sectionCards(withVideo, SearchSection.MOMENTS, "beach").single().count)
        assertEquals(3, sectionCards(withVideo, SearchSection.PLACES, "paris").single().count) // places keep videos, like Place
        assertEquals(2, sectionCards(withVideo, SearchSection.PLACES, "").size)
    }

    @Test
    fun swatchesKeepTheTenReferenceValuesAndThePickerStartsAtTheReferenceBlue() {
        assertEquals(
            listOf("#F2F2F2", "#000000", "#FF0000", "#FFE500", "#FF7A00", "#FF00E5", "#9E00FF", "#0066FF", "#00C2FF", "#008625"),
            SEARCH_SWATCH_HEX
        )
        assertEquals("#007AFF", PICKER_START_HEX)
    }

    @Test
    fun pickerHsvRoundTripsEverySwatchAndTheStartColor() {
        (SEARCH_SWATCH_HEX + PICKER_START_HEX).forEach { hex ->
            val (h, s, v) = hsvOf(hex)
            assertEquals(hex, hexOf(Color.hsv(h, s, v)))
        }
        val (h, s, v) = hsvOf(PICKER_START_HEX)
        assertEquals(211.3f, h, 0.1f)
        assertEquals(1f, s, 0f)
        assertEquals(1f, v, 0f)
        assertEquals("#FF0000", hexOf(colorOfHex("#ff0000")))
    }

    @Test
    fun sectionTitlesAreTheReferenceNames() {
        assertEquals(R.string.albums, SearchSection.ALBUMS.title)
        assertEquals(R.string.places, SearchSection.PLACES.title)
        assertEquals(R.string.moment, SearchSection.MOMENTS.title)
    }
}
