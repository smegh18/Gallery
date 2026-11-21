package com.quizango.gallery.ui.discovery

import com.quizango.gallery.data.repository.hasCoordinates
import com.quizango.gallery.data.repository.placeKey
import com.quizango.gallery.domain.model.MediaItem
import com.quizango.gallery.ui.navigation.Screen

/** One Place card and marker (CommonModel of MapViewModel): media sharing [key], in query order. The key is the title. */
internal class PlaceGroup(val key: String, val items: List<MediaItem>) {
    /** Where the marker goes: the first item with usable coordinates; null for a name-only group (card, no marker). */
    val anchor: MediaItem? get() = items.firstOrNull { hasCoordinates(it.latitude, it.longitude) }
}

/** MapViewModel.OooO0Oo: a LinkedHashMap by place in query order (newest first); items without a key are skipped. */
internal fun groupPlaces(items: List<MediaItem>): List<PlaceGroup> =
    items.groupBy { it.placeKey() }.filterKeys { it.isNotEmpty() }.map { (key, list) -> PlaceGroup(key, list) }

/** Card or marker tap: the media list of exactly this place, titled with it (MapActivity → MediaListActivity). */
internal fun placeMediaRoute(group: PlaceGroup): String = Screen.AlbumDetail.createRoute("filter:place:${group.key}", group.key)
