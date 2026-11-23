package com.quizango.gallery

import com.quizango.gallery.data.repository.hasCoordinates
import com.quizango.gallery.data.repository.placeKey
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.discovery.LocationOutcome
import com.quizango.gallery.ui.discovery.groupPlaces
import com.quizango.gallery.ui.discovery.locationGranted
import com.quizango.gallery.ui.discovery.locationOutcome
import com.quizango.gallery.ui.home.ExploreItem
import com.quizango.gallery.ui.home.exploreRoute
import com.quizango.gallery.ui.viewer.showOnMapRoute
import com.quizango.gallery.ui.discovery.placeMediaRoute
import com.quizango.gallery.ui.discovery.placeMosaicRows
import com.quizango.gallery.ui.discovery.placeRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceScreenTest {

    private fun item(id: Long, place: String = "", lat: Float = 0f, lng: Float = 0f) = MediaItem(
        id = id, filePath = "/x/$id.jpg", fileName = "$id.jpg", fileSize = 1, folderName = "x", folderPath = "/x",
        dateModified = id, dateTaken = id, latitude = lat, longitude = lng, placeName = place
    )

    @Test
    fun keyIsTheTrimmedPlaceNameElseRoundedCoordinatesElseEmpty() {
        assertEquals("Paris", placeKey("  Paris ", 48.85f, 2.35f))
        assertEquals("48.8566,2.3522", placeKey("", 48.85661f, 2.35222f))
        assertEquals("", placeKey("", 0f, 0f))
        assertEquals("", placeKey("   ", Float.NaN, 10f))
        assertEquals("", placeKey("", 95f, 10f))
    }

    @Test
    fun coordinatesNeedToBeInRangeAndNotTheZeroDefault() {
        assertFalse(hasCoordinates(0f, 0f))
        assertTrue(hasCoordinates(0f, 5f))
        assertFalse(hasCoordinates(-91f, 0f))
        assertFalse(hasCoordinates(10f, 181f))
        assertFalse(hasCoordinates(Float.NaN, 5f))
    }

    @Test
    fun groupsKeepQueryOrderAndSkipItemsWithoutAPlace() {
        val groups = groupPlaces(
            listOf(item(5, "Rome", 41.9f, 12.5f), item(4, "Paris", 48.8f, 2.3f), item(3), item(2, "Rome"), item(1, " Paris"))
        )
        assertEquals(listOf("Rome", "Paris"), groups.map { it.key })
        assertEquals(listOf(5L, 2L), groups[0].items.map { it.id })
        assertEquals(listOf(4L, 1L), groups[1].items.map { it.id })
    }

    @Test
    fun coordinateOnlyItemsShareAKeyWithinAbout11m() {
        val groups = groupPlaces(listOf(item(1, lat = 10.00001f, lng = 20.00001f), item(2, lat = 10.00002f, lng = 20.00002f)))
        assertEquals(1, groups.size)
        assertEquals("10.0000,20.0000", groups[0].key)
    }

    @Test
    fun markerAnchorsOnTheFirstUsableCoordinateAndNameOnlyGroupsHaveNone() {
        val named = groupPlaces(listOf(item(2, "Rome"), item(1, "Rome", 41.9f, 12.5f))).single()
        assertEquals(1L, named.anchor?.id)
        assertNull(groupPlaces(listOf(item(3, "Nowhere"))).single().anchor)
    }

    @Test
    fun cardRouteCarriesThePlaceFilterEncodedAndNeverAPath() {
        val group = groupPlaces(listOf(item(1, "A/B: c&d", 1f, 1f))).single()
        val route = placeMediaRoute(group)
        assertEquals("album/filter%3Aplace%3AA%2FB%3A%20c%26d/A%2FB%3A%20c%26d", route)
        assertTrue(route.split("/").size == 3) // nothing in the key can add a path segment
    }

    @Test
    fun placeRouteEncodesTheSelectedKey() {
        assertEquals("place?key=", placeRoute())
        assertEquals("place?key=Saint%20Louis%2FMO", placeRoute("Saint Louis/MO"))
        assertNotNull(placeRoute("48.8566,2.3522"))
    }

    @Test
    fun mosaicFacePerCountMatchesItemLocation() {
        assertEquals(emptyList<List<Int>>(), placeMosaicRows(1))
        assertEquals(listOf(listOf(0), listOf(1)), placeMosaicRows(2))
        assertEquals(listOf(listOf(0, 1), listOf(2)), placeMosaicRows(3))
        assertEquals(listOf(listOf(0, 1), listOf(2, 3)), placeMosaicRows(4))
        assertEquals(listOf(listOf(0, 1), listOf(2, 3)), placeMosaicRows(50)) // no overflow count
    }

    @Test
    fun locationStripeShowsUntilFineOrCoarseIsGranted() {
        assertFalse(locationGranted(fine = false, coarse = false))
        assertTrue(locationGranted(fine = true, coarse = false))
        assertTrue(locationGranted(fine = false, coarse = true)) // Android 12 "Approximate"
    }

    @Test
    fun permanentDenialRoutesToSettingsAndFirstDenialStaysRequestable() {
        assertEquals(LocationOutcome.GRANTED, locationOutcome(fine = false, coarse = true, canAskAgain = false))
        assertEquals(LocationOutcome.RETRY, locationOutcome(fine = false, coarse = false, canAskAgain = true))
        assertEquals(LocationOutcome.SETTINGS, locationOutcome(fine = false, coarse = false, canAskAgain = false))
    }

    @Test
    fun exploreAndViewerShareOneRouteContract() {
        assertEquals(placeRoute(), exploreRoute(ExploreItem.PLACE, junkCleanedToday = false))
        assertEquals(placeRoute("Paris"), showOnMapRoute(item(1, "Paris")))
        assertNull(showOnMapRoute(item(2)))
    }

    @Test
    fun syncKeepsReadLocationUntilTheFileChanges() {
        val old = com.quizango.gallery.data.local.database.entity.MediaEntity("/p/a.jpg", lastDate = 5, isLocation = true, lat = 48.85f, longs = 2.35f, place = "Paris")
        val same = com.quizango.gallery.data.local.database.entity.MediaEntity("/p/a.jpg", lastDate = 5)
        assertEquals("Paris", com.quizango.gallery.data.repository.keepLocation(same, old).place)
        assertTrue(com.quizango.gallery.data.repository.keepLocation(same, old).isLocation)
        assertFalse(com.quizango.gallery.data.repository.keepLocation(same.copy(lastDate = 6), old).isLocation)
        assertEquals("Minato City", com.quizango.gallery.data.repository.pickPlaceName(null, " ", "Minato City", "Tokyo"))
        assertEquals("", com.quizango.gallery.data.repository.pickPlaceName(null, ""))
    }
}
