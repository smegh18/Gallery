package com.quizango.gallery.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.quizango.gallery.R
import com.quizango.gallery.ui.splash.checkStoragePermissions
import com.quizango.gallery.ui.theme.GalleryText
import com.quizango.gallery.ui.theme.sdp

/** One Home tab: the title shown in the top bar and the label/icons shown in the tab bar (HA.o000o0oO / o000Oo00). */
internal data class HomeTab(
    @StringRes val title: Int,
    @StringRes val label: Int,
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val unselectedIcon: Int
)

/** In pager-page order: PAGE_PHOTOS, PAGE_ALBUMS, PAGE_EXPLORE. */
internal val HOME_TABS = listOf(
    HomeTab(R.string.home_shell_photos, R.string.home_shell_photos, R.drawable.ic_tab_photos_fill, R.drawable.ic_tab_photos_outline),
    HomeTab(R.string.home_shell_album, R.string.albums, R.drawable.ic_tab_albums_fill, R.drawable.ic_tab_albums_outline),
    HomeTab(R.string.explore, R.string.explore, R.drawable.ic_tab_explore_fill, R.drawable.ic_tab_explore_outline)
)

internal enum class TopBarSlot { AD_FREE, ADD_ALBUM, SEARCH, CAMERA, MORE }

internal enum class MoreMenuRow { SORT, RECYCLE_BIN, SETTINGS }

/** Top-bar buttons left to right (HA.o000o0oO); + and the more button need media permission. */
internal fun topBarSlots(page: Int, hasMediaPermission: Boolean): List<TopBarSlot> = buildList {
    add(TopBarSlot.AD_FREE)
    if (page == PAGE_ALBUMS && hasMediaPermission) add(TopBarSlot.ADD_ALBUM)
    add(TopBarSlot.SEARCH)
    if (page == PAGE_PHOTOS) add(TopBarSlot.CAMERA)
    if (page != PAGE_EXPLORE && hasMediaPermission) add(TopBarSlot.MORE)
}

/** Sort is on Photos only (HA.OoooooO: activeTabNumber == 1). */
internal fun moreMenuRows(page: Int): List<MoreMenuRow> = buildList {
    if (page == PAGE_PHOTOS) add(MoreMenuRow.SORT)
    add(MoreMenuRow.RECYCLE_BIN)
    add(MoreMenuRow.SETTINGS)
}

@Composable
internal fun HomeTopBar(
    page: Int,
    onSearch: () -> Unit,
    onCamera: () -> Unit,
    onAddAlbum: () -> Unit,
    onMoreRow: (MoreMenuRow) -> Unit,
    onTitleLongPress: () -> Unit,
    onPro: () -> Unit,
    onTitlePositioned: (Rect) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val slots = topBarSlots(page, checkStoragePermissions(LocalContext.current))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.backgroundColor))
            .statusBarsPadding()
            .height(42.sdp)
            .padding(end = 7.sdp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(HOME_TABS[page].title),
            style = GalleryText.BodyXXLargeSemiBold,
            color = colorResource(R.color.home_shell_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 13.sdp)
                .onGloballyPositioned { onTitlePositioned(it.boundsInWindow()) } // the vault-hint pill sits on it
                .pointerInput(onTitleLongPress) {
                    detectTapGestures(onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTitleLongPress()
                    })
                }
        )
        Spacer(Modifier.weight(1f))
        slots.forEach { slot ->
            when (slot) {
                // HA.Ooooo00 "pro" action: opens the Pro screen
                TopBarSlot.AD_FREE -> SlotButton(R.drawable.home_shell_ic_ad_free, stringResource(R.string.go_ads_free), onPro)
                TopBarSlot.ADD_ALBUM -> SlotButton(R.drawable.home_shell_ic_add, stringResource(R.string.create_album), onAddAlbum)
                TopBarSlot.SEARCH -> SlotButton(R.drawable.home_shell_ic_search, stringResource(R.string.search), onSearch)
                TopBarSlot.CAMERA -> SlotButton(R.drawable.home_shell_ic_camera, stringResource(R.string.camera), onCamera)
                // The menu state is keyed on the page, so it closes when the page changes.
                TopBarSlot.MORE -> {
                    var open by remember(page) { mutableStateOf(false) }
                    Box {
                        SlotButton(R.drawable.home_shell_ic_more, stringResource(R.string.home_shell_more_options)) { open = true }
                        if (open) {
                            MoreMenu(page, onDismiss = { open = false }, onRow = { open = false; onMoreRow(it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotButton(@DrawableRes icon: Int, description: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.sdp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick
            )
            .padding(7.sdp)
    ) {
        Image(painterResource(icon), description, Modifier.fillMaxSize())
    }
}

/**
 * home_shell_popup_menu: under the more button, 8.sdp down, end-aligned flush with the screen edge
 * (HA.OoooooO; measured on 2.7.2, where PopupWindow clamps the card on screen).
 */
@Composable
private fun MoreMenu(page: Int, onDismiss: () -> Unit, onRow: (MoreMenuRow) -> Unit) {
    val density = LocalDensity.current
    val inset = 8.sdp // room for the 8.sdp shadow, which a popup window would otherwise clip
    val offset = with(density) { 8.sdp.roundToPx() }
    val insetPx = with(density) { inset.roundToPx() }
    val position = remember(offset, insetPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize
            ): IntOffset {
                val contentWidth = popupContentSize.width - 2 * insetPx
                val x = if (layoutDirection == LayoutDirection.Ltr) windowSize.width - contentWidth else 0
                return IntOffset(x - insetPx, anchorBounds.bottom + offset)
            }
        }
    }
    // clippingEnabled = false: the transparent shadow inset may run past the screen edge while the card stays on it
    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true, clippingEnabled = false)) {
        Column(
            modifier = Modifier
                .padding(start = inset, end = inset, bottom = inset)
                .shadow(8.sdp, RoundedCornerShape(10.sdp))
                .background(colorResource(R.color.home_shell_popup_bg), RoundedCornerShape(10.sdp))
                .widthIn(min = 120.sdp)
                .width(IntrinsicSize.Max)
                .padding(vertical = 3.sdp)
        ) {
            moreMenuRows(page).forEach { row ->
                val (icon, label) = when (row) {
                    MoreMenuRow.SORT -> R.drawable.home_shell_ic_sort to R.string.home_shell_sort
                    MoreMenuRow.RECYCLE_BIN -> R.drawable.home_shell_ic_recycle to R.string.recycle_bin
                    MoreMenuRow.SETTINGS -> R.drawable.home_shell_ic_settings to R.string.settings
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(37.sdp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colorResource(R.color.home_shell_popup_ripple)),
                            onClick = { onRow(row) }
                        )
                        .padding(start = 12.sdp, end = 17.sdp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(icon), null, Modifier.size(16.sdp))
                    Text(
                        text = stringResource(label),
                        style = GalleryText.BodyMediumMedium,
                        color = colorResource(R.color.home_shell_popup_text),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.sdp)
                    )
                }
            }
        }
    }
}

/** Remote-config default vault_hint_tooltip "session": [4, 8, 12]. */
internal val VAULT_HINT_SESSIONS = setOf(4, 8, 12)

/** True when the app was opened from a notification, deep link or shortcut (HA.o000O00O); MainActivity provides it. */
internal val LocalOpenedFromOutside = compositionLocalOf { false }

/**
 * HA.o000o0O0 show rules (UI-SPEC 1.4): never after an outside entry, and held while a start-up dialog
 * ([startDialogShowing]: the notification permission request, the usage-access prompt or the floating-button offer) is up.
 */
internal fun shouldShowVaultHint(
    session: Int,
    hintSeen: Boolean,
    closedSession: Int,
    hasMediaPermission: Boolean,
    openedFromOutside: Boolean = false,
    startDialogShowing: Boolean = false
): Boolean = session in VAULT_HINT_SESSIONS && !hintSeen && closedSession != session && hasMediaPermission &&
    !openedFromOutside && !startDialogShowing

/** HomeBottomBarBehavior SETTLE_MS (0xdc). */
internal const val BAR_SETTLE_MS = 220

/** On scroll stop the bar hides only when more than half of it is already hidden (strictly greater). */
internal fun settledBarOffset(offset: Float, height: Float): Float = if (offset > height / 2f) height else 0f

/** Settle progress of S272/z/p.smali: ease-out cubic. */
internal fun barSettleEasing(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)

/** No banner-ad slot: the reference slot is 0 high with no ad SDK (decision D5). */
@Composable
internal fun HomeTabBar(selectedPage: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    // The nav-bar strip under it is HomeScreen's navBarBg, which stays put while this bar slides.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(R.color.home_shell_tab_bg))
    ) {
        HorizontalDivider(thickness = 1.dp, color = colorResource(R.color.home_shell_tab_divider))
        Row(modifier = Modifier.fillMaxWidth().height(44.sdp)) {
            HOME_TABS.forEachIndexed { page, tab ->
                val selected = page == selectedPage
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = { onSelect(page) }
                        )
                        .padding(top = 7.sdp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(if (selected) tab.selectedIcon else tab.unselectedIcon),
                        contentDescription = null,
                        modifier = Modifier.size(19.sdp)
                    )
                    // SemiBold on every tab: the reference re-applies the selected typeface to all labels (HA.o000Oo0O)
                    Text(
                        text = stringResource(tab.label),
                        style = GalleryText.BodySmallSemiBold,
                        color = colorResource(if (selected) R.color.home_shell_tab_selected else R.color.home_shell_tab_unselected),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.sdp, start = 3.sdp, end = 3.sdp)
                    )
                }
            }
        }
    }
}
