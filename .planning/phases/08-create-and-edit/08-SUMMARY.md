# Phase 08: Create and Edit — Summary

Requirements EDT-01..03, checklist rows #38-#45. Branch `worktree-agent-a40ab26b8adc598af` (pushed as `phase-08-wip`).

## Reference findings that drove decisions

- **The "changed" layouts of row #38 belong to the collage.** item_crop (je/c), item_tools (je/z, je/ah), item_res
  (je/ab, je/e, je/u), fragment_text_input (te/j), layout_adjust_text_section, layout_text_color_tab and item_font
  (je/o) are inflated only by the collage library. Their 2.7.2 change is the font alone: `@font/font` becomes
  `inter` / `if_inter_medium`. They are ported in the collage (row #40: CollagePanels.kt, CollageTextEditor.kt).
  PhotoEditActivity's own layouts (activity_photo_edit, item_edit, item_crop_layout, item_ratio, fragment_text) did not change.
- **CropImageActivity (row #39) is never launched by app code.** Only the canhub library's own lambdas refer to it.
  The editor embeds `CropImageView` in item_crop_layout, so row #39 is the editor's crop view (`editor/crop/ImageCropView.kt`).
- **dialog_exit_popup is the collage's dialog, not the editor's.** Its only caller is `me/d.HISPj7KHQ7(CollageScreenActivity, …)`.
  The collage ports it (CollagePanels.kt, `exit_without_saving`). PhotoEditActivity, RemoveBg, BlurBg and ColorSplash
  use dialog_discard_changes (cw/b). The row #10 note is corrected here.
- Editor entry: TYPE 3 single picker when there is no photo (Explore AI Editor). Strip order is EDIT_TAB_* with the three
  AI tools first, shown only on Android 10+ with at least 3 GB RAM (s9/k.OooOo0o). "New" badge prefs
  `isRemoveNewToolTip`, `isColorSplashNewToolTip`, `isBlurNewToolTip`. Tooltip prefs `isRemoveEditToolTip`,
  `isColorEditToolTip`, `isBlurEditToolTip` (one tooltip at a time, Remove BG first, default true = not seen).
- The working copy is `filesDir/image.<ext>`. Every applied tool rewrites it as PNG. Save writes `Image_<millis>.png`
  into the source's folder (Pictures when it has none), scans it, then finishes with a result intent (`TYPE`, `DATA` = the
  new GalleryModel) back to the caller. With no edits, Save just leaves.
- Sticker: StickerActivity (activity_sticker = layout_search_toolbar + 3-column grid + emptyLayout "No Stickers")
  loads from the sticker API. We make no network calls, so it always shows the empty state, as 2.7.2 does offline.

## Built per row

| Row | Status | Files |
|---|---|---|
| #38 PhotoEditActivity | built | `ui/editor/PhotoEditorScreen.kt` (screen, toolbar, AI tooltip), `EditorImage.kt` (tools, presets, 25 GPUImage filters, adjust, files), `EditorPanels.kt` (strip with AI/New badges, crop, filter, adjust ruler, brush, sticker, text panels), `EditorText.kt` (text_art stickers, fragment_text input, rendering), `EditHistory.kt` (brush undo/redo), `ImageSharpen.kt` (GPUImage sharpen kernel incl. negative soften) |
| #39 CropImageActivity | built (as embedded view) | `ui/editor/crop/ImageCropView.kt` (CropImageView overlay, handles, aspect presets, rotate/flip, bitmap crop) |
| #40 Collage | built (earlier commit) | `ui/creative/collage/*`, `ui/creative/CollageMakerScreen.kt`. No collage home; dialog_exit_popup on Back |
| #41-#43 Remove BG / Blur BG / Color Splash | built (earlier commit) | `ui/creative/RemoveBgScreen.kt`, `BlurBgScreen.kt`, `ColorSplashScreen.kt`, `AiToolParts.kt`, `ImageSegmentationHelper.kt`, first-run guides |
| #44 Sticker | built | `ui/creative/StickerScreen.kt` (reuses `ui/search/SearchToolbar`, now `internal` with a `hint` param, "Explore Stickers") |
| #45 Wallpaper | built (earlier commit) | `ui/creative/WallpaperScreen.kt` + dialog_set_wallpaper |
| shared | built (earlier commit) | `ui/components/MediaPicker.kt` (activity_select TYPE 3/4), `ui/editor/DiscardChangesDialog.kt`, `ui/creative/ColorPickerDialog.kt` |

SearchScreen change verified: activity_sticker includes the same `layout_search_toolbar`. The default hint stays
`R.string.search`, so Search is unchanged.

## Navigation wiring (GalleryNavHost, existing destinations only)

- `Screen.Editor`: the AI tools set `savedStateHandle["editResult"] = true` on the editor entry. The editor collects it
  as `toolEdited` and resets it through `onToolEditConsumed`. Save sets `"editedPath"` on the previous entry and pops.
- `Screen.Stickers`: `StickerScreen(onBack)`. Inside the editor the picker is shown as an overlay.

## Deviations from 2.7.2

- The sticker grid is always empty because there is no sticker API. Sticker placement code is not ported because nothing
  can be picked.
- The text tool uses eight platform typefaces in place of 2.7.2's third-party font files (Amatic, Bebas Neue, Cocogoose and others).
- Filters and adjust are CPU colour-matrix and kernel ports of the GPUImage shaders, not GPUImage itself.
- Photos over 4096 px on the long side are decoded downsampled (shortcut noted in code).
- The caller does not read `"editedPath"` yet (the viewer does not reopen on the saved file). MediaStore sync refreshes the lists.
- The banner ad slot (global_ad_framelayout) is left out: `BannerAdView` is a no-op.

## Tests

`PhotoEditorTest` was rewritten for the new editor: strip/AI gating, tooltip order, suggestion entry, crop preset aspects,
adjust scale, working/saved file names, refusals, sample size, text-sticker corner hits, zoom/rotate. The crop geometry
tests are kept. Existing tests: `EditHistoryTest`, `ImageSharpenTest`, `AiToolsTest`, `CollageModelTest`, `PhotoPickerTest`.

## Gates

`testDebugUnitTest` pass · `assembleDebug` pass · `check-strings` up to date (24 languages) · `check-colors` match ·
`git diff --check` clean. UI audit of the changed screens: no gradients except the user's own text gradient, no glows or
emoji, no tracked uppercase. Colours come from resources, except the canhub library's fixed #AAFFFFFF crop guideline.

## Left undone

- Device or Artemis check (skipped by user decision; Phase 13).
- Integrator: optionally read `"editedPath"` in the viewer to open the saved image.
- Integrator: correct the PARITY-CHECKLIST row #10 note (dialog_exit_popup = collage).
