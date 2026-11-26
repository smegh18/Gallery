package com.quizango.gallery.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.quizango.gallery.data.repository.movedPath
import com.quizango.gallery.ui.vault.VaultGate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gallery_preferences")

/** One albumCover entry: folder and cover path joined by NUL, which cannot occur in a path, so the split is exact. */
internal fun albumCoverEntry(folder: String, coverPath: String) = folder + '\u0000' + coverPath

internal fun parseAlbumCovers(entries: Set<String>): Map<String, String> =
    entries.mapNotNull { e -> e.indexOf('\u0000').takeIf { it >= 0 }?.let { e.substring(0, it) to e.substring(it + 1) } }.toMap()

/** Missing means no irreversible retention policy was approved; either saved Boolean is preserved exactly. */
internal fun autoDeleteTrashValue(savedValue: Boolean?): Boolean = savedValue ?: false

/** Show Pattern is on until the user turns it off (S3); untouched existing installs flip to showing the path, accepted as no release exists. */
internal fun showPatternValue(raw: Boolean?): Boolean = raw ?: true

class AppPreferences(private val context: Context) {

    private object Keys {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val HAS_COMPLETED_WELCOME = booleanPreferencesKey("has_completed_welcome")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val INSTALL_SOURCE = stringPreferencesKey("install_source")
        val THEME_MODE = intPreferencesKey("theme_mode") // 0: Light, 1: Dark, 2: System
        val MEDIA_COL_COUNT = intPreferencesKey("media_col_count")
        val IS_NO_REPEAT = intPreferencesKey("isNoRepeat") // 2.7.2 key: Video player repeat 0 / 1 / 2
        val PINCH_TIP_SEEN = booleanPreferencesKey("isPinchInOutToolTip") // 2.7.2 key name
        val SESSION_COUNT = intPreferencesKey("sessionCount") // 2.7.2 key names from here
        val VAULT_HINT_SEEN = booleanPreferencesKey("isVaultHintToolTip")
        val VAULT_HINT_CLOSED_SESSION = intPreferencesKey("vaultHintClosedSession")
        val AUTO_DELETE_TRASH = booleanPreferencesKey("auto_delete_trash_30_days")
        val ORGANIZER_SKIP_TOOLTIP = booleanPreferencesKey("isOrgSkipToolTip")
        val ORGANIZER_STEP_FLOW = intPreferencesKey("organizerStepFlow")
        val ORGANIZER_BIN_PATHS = stringSetPreferencesKey("organizerBinPathsV1")
        val ORGANIZER_STRIPE_SHOWN = booleanPreferencesKey("isOrganizerStripeShown")
        val VAULT_PIN = stringPreferencesKey("vault_pin")
        val VAULT_PATTERN = stringPreferencesKey("vault_pattern")
        val IS_PATTERN_MODE = booleanPreferencesKey("is_vault_pattern_mode")
        val IS_PRIVATE_LOCK = booleanPreferencesKey("isPrivateLock") // 2.7.2 key names: lock-out flag, first-time secured toast
        val IS_PRIVATE_TOAST_SHOW = booleanPreferencesKey("isPrivateToastShow")
        val IS_FINGERPRINT_ENABLED = booleanPreferencesKey("is_fingerprint_enabled")
        val STORIES_EDIT_TOOLTIP = booleanPreferencesKey("isStoriesEditToolTip") // 2.7.2 key names (Phase 4)
        val COLLAGE_NEW_EXPLORE = booleanPreferencesKey("collageMakerNewButtonExplore")
        val JUNK_CLEANED_TODAY = booleanPreferencesKey("isJunkCleanedForToday")
        val PIN_ALBUM = stringSetPreferencesKey("pinAlbum") // 2.7.2 key name: folder paths pinned to the top
        val ALBUM_COVER = stringSetPreferencesKey("albumCover") // 2.7.2 key name: folder + cover path entries
        val VAULT_QUESTION = stringPreferencesKey("vault_recovery_question")
        val VAULT_ANSWER = stringPreferencesKey("vault_recovery_answer")
        val VAULT_QUESTION_2 = stringPreferencesKey("vault_recovery_question_2")
        val VAULT_ANSWER_2 = stringPreferencesKey("vault_recovery_answer_2")
        val VAULT_QUESTION_3 = stringPreferencesKey("vault_recovery_question_3")
        val VAULT_ANSWER_3 = stringPreferencesKey("vault_recovery_answer_3")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_FIRST_LAUNCH] ?: true }
    val hasCompletedWelcome: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAS_COMPLETED_WELCOME] ?: false }
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.HAS_COMPLETED_ONBOARDING] ?: it[Keys.HAS_COMPLETED_WELCOME] ?: false
    }
    val appLanguage: Flow<String> = context.dataStore.data.map { it[Keys.APP_LANGUAGE] ?: "en" }
    val themeMode: Flow<Int> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: 2 }
    val mediaColCount: Flow<Int> = context.dataStore.data.map { it[Keys.MEDIA_COL_COUNT] ?: 3 }
    val isNoRepeat: Flow<Int> = context.dataStore.data.map { it[Keys.IS_NO_REPEAT] ?: 0 }
    val isPinchInOutToolTip: Flow<Boolean> = context.dataStore.data.map { it[Keys.PINCH_TIP_SEEN] ?: false }
    val sessionCount: Flow<Int> = context.dataStore.data.map { it[Keys.SESSION_COUNT] ?: 0 }
    val isVaultHintToolTip: Flow<Boolean> = context.dataStore.data.map { it[Keys.VAULT_HINT_SEEN] ?: false }
    val isStoriesEditToolTip: Flow<Boolean> = context.dataStore.data.map { it[Keys.STORIES_EDIT_TOOLTIP] ?: false }
    val collageMakerNewButtonExplore: Flow<Boolean> = context.dataStore.data.map { it[Keys.COLLAGE_NEW_EXPLORE] ?: false }
    // Written by Junk cleared (RemoveJunkScreen) once a clean really removed something.
    val isJunkCleanedForToday: Flow<Boolean> = context.dataStore.data.map { it[Keys.JUNK_CLEANED_TODAY] ?: false }
    val pinnedAlbums: Flow<Set<String>> = context.dataStore.data.map { it[Keys.PIN_ALBUM] ?: emptySet() }
    val albumCovers: Flow<Map<String, String>> = context.dataStore.data.map { parseAlbumCovers(it[Keys.ALBUM_COVER] ?: emptySet()) }
    val vaultHintClosedSession: Flow<Int> = context.dataStore.data.map { it[Keys.VAULT_HINT_CLOSED_SESSION] ?: 0 }
    /** A new install must never authorize automatic permanent deletion. Existing values are read unchanged. */
    val autoDeleteTrash: Flow<Boolean> = context.dataStore.data.map { autoDeleteTrashValue(it[Keys.AUTO_DELETE_TRASH]) }
    /** Read-only reference preference: no decoded code writes this flag. */
    val isOrgSkipToolTip: Flow<Boolean> = context.dataStore.data.map { it[Keys.ORGANIZER_SKIP_TOOLTIP] ?: false }
    /** Intro is shown on first use; 7 is the reference's completed state. */
    val organizerStepFlow: Flow<Int> = context.dataStore.data.map { it[Keys.ORGANIZER_STEP_FLOW] ?: 1 }
    val organizerBinPaths: Flow<Set<String>> = context.dataStore.data.map { it[Keys.ORGANIZER_BIN_PATHS] ?: emptySet() }
    val isOrganizerStripeShown: Flow<Boolean> = context.dataStore.data.map { it[Keys.ORGANIZER_STRIPE_SHOWN] ?: false }
    val vaultPin: Flow<String?> = context.dataStore.data.map { it[Keys.VAULT_PIN] }
    val vaultPattern: Flow<String?> = context.dataStore.data.map { it[Keys.VAULT_PATTERN] }
    val isPatternMode: Flow<Boolean> = context.dataStore.data.map { showPatternValue(it[Keys.IS_PATTERN_MODE]) }
    val isPrivateLock: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_PRIVATE_LOCK] ?: false }
    val isPrivateToastShow: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_PRIVATE_TOAST_SHOW] ?: false }
    val isVaultLockSet: Flow<Boolean> = context.dataStore.data.map { VaultGate.lockSet(it[Keys.VAULT_PIN], it[Keys.VAULT_PATTERN]) }
    val isFingerprintEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_FINGERPRINT_ENABLED] ?: false }
    val vaultQuestion: Flow<String?> = context.dataStore.data.map { it[Keys.VAULT_QUESTION] }
    val vaultAnswer: Flow<String?> = context.dataStore.data.map { it[Keys.VAULT_ANSWER] }

    /** All saved recovery (question, answer hash) pairs, for Forgot Password. */
    val vaultRecoveryPairs: Flow<List<Pair<String, String>>> = context.dataStore.data.map { p ->
        listOf(
            Keys.VAULT_QUESTION to Keys.VAULT_ANSWER,
            Keys.VAULT_QUESTION_2 to Keys.VAULT_ANSWER_2,
            Keys.VAULT_QUESTION_3 to Keys.VAULT_ANSWER_3
        ).mapNotNull { (q, a) ->
            val question = p[q]
            val answer = p[a]
            if (question.isNullOrEmpty() || answer.isNullOrEmpty()) null else question to answer
        }
    }

    val hasRecoveryQuestion: Flow<Boolean> = vaultRecoveryPairs.map { it.isNotEmpty() }

    suspend fun setFirstLaunch(completed: Boolean) {
        context.dataStore.edit { it[Keys.IS_FIRST_LAUNCH] = completed }
    }

    suspend fun setCompletedWelcome(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_WELCOME] = completed }
    }

    val installSource: Flow<String?> = context.dataStore.data.map { it[Keys.INSTALL_SOURCE] }

    suspend fun setInstallSource(source: String) {
        context.dataStore.edit { it[Keys.INSTALL_SOURCE] = source }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit {
            it[Keys.HAS_COMPLETED_ONBOARDING] = completed
            it[Keys.HAS_COMPLETED_WELCOME] = completed
        }
    }

    suspend fun setLanguage(langCode: String) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = langCode }
        // Applies the language to the app now (AppCompat stores it across restarts)
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(langCode)
        )
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setMediaColCount(count: Int) {
        context.dataStore.edit { it[Keys.MEDIA_COL_COUNT] = count }
    }

    suspend fun setNoRepeat(value: Int) {
        context.dataStore.edit { it[Keys.IS_NO_REPEAT] = value }
    }

    suspend fun setAlbumsPinned(paths: Set<String>, pinned: Boolean) {
        context.dataStore.edit { prefs ->
            val now = prefs[Keys.PIN_ALBUM] ?: emptySet()
            prefs[Keys.PIN_ALBUM] = if (pinned) now + paths else now - paths
        }
    }

    /** Replaces [folder]'s cover entry. */
    suspend fun setAlbumCover(folder: String, coverPath: String) {
        context.dataStore.edit { prefs ->
            val now = prefs[Keys.ALBUM_COVER] ?: emptySet()
            prefs[Keys.ALBUM_COVER] = now.filterNot { parseAlbumCovers(setOf(it)).containsKey(folder) }.toSet() + albumCoverEntry(folder, coverPath)
        }
    }

    /** A renamed folder keeps its pin and cover: every entry at or under [oldFolder] follows it to [newFolder]. */
    suspend fun moveAlbumKeys(oldFolder: String, newFolder: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_ALBUM]?.let { pins -> prefs[Keys.PIN_ALBUM] = pins.map { movedPath(it, oldFolder, newFolder) }.toSet() }
            prefs[Keys.ALBUM_COVER]?.let { covers ->
                prefs[Keys.ALBUM_COVER] = parseAlbumCovers(covers)
                    .map { (folder, cover) -> albumCoverEntry(movedPath(folder, oldFolder, newFolder), movedPath(cover, oldFolder, newFolder)) }
                    .toSet()
            }
        }
    }

    suspend fun setPinchInOutToolTip(seen: Boolean) {
        context.dataStore.edit { it[Keys.PINCH_TIP_SEEN] = seen }
    }

    /** One cold launch (one Splash run); the first launch becomes session 1. */
    suspend fun incrementSessionCount() {
        context.dataStore.edit { it[Keys.SESSION_COUNT] = (it[Keys.SESSION_COUNT] ?: 0) + 1 }
    }

    suspend fun setVaultHintToolTip(seen: Boolean) {
        context.dataStore.edit { it[Keys.VAULT_HINT_SEEN] = seen }
    }

    suspend fun setVaultHintClosedSession(session: Int) {
        context.dataStore.edit { it[Keys.VAULT_HINT_CLOSED_SESSION] = session }
    }

    suspend fun setStoriesEditToolTip(seen: Boolean) {
        context.dataStore.edit { it[Keys.STORIES_EDIT_TOOLTIP] = seen }
    }

    suspend fun setJunkCleanedForToday(cleaned: Boolean) {
        context.dataStore.edit { it[Keys.JUNK_CLEANED_TODAY] = cleaned }
    }

    suspend fun setCollageMakerNewButtonExplore(seen: Boolean) {
        context.dataStore.edit { it[Keys.COLLAGE_NEW_EXPLORE] = seen }
    }

    suspend fun setAutoDeleteTrash(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DELETE_TRASH] = enabled }
    }

    suspend fun setOrganizerStepFlow(step: Int) {
        context.dataStore.edit { it[Keys.ORGANIZER_STEP_FLOW] = step.coerceIn(1, 7) }
    }

    suspend fun addOrganizerBinPaths(paths: Set<String>, limit: Int = 2000) {
        context.dataStore.edit { prefs ->
            val existing = prefs[Keys.ORGANIZER_BIN_PATHS].orEmpty()
            prefs[Keys.ORGANIZER_BIN_PATHS] = (existing + paths).toList().takeLast(limit.coerceIn(1, 2000)).toSet()
        }
    }

    suspend fun removeOrganizerBinPaths(paths: Set<String>) {
        context.dataStore.edit { prefs -> prefs[Keys.ORGANIZER_BIN_PATHS] = prefs[Keys.ORGANIZER_BIN_PATHS].orEmpty() - paths }
    }

    suspend fun reconcileOrganizerBinPaths(recyclePaths: Set<String>, limit: Int = 2000) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ORGANIZER_BIN_PATHS] = (prefs[Keys.ORGANIZER_BIN_PATHS].orEmpty() intersect recyclePaths).take(limit.coerceIn(1, 2000)).toSet()
        }
    }

    suspend fun setOrganizerStripeShown(shown: Boolean) {
        context.dataStore.edit { it[Keys.ORGANIZER_STRIPE_SHOWN] = shown }
    }

    suspend fun setVaultPin(pin: String) {
        // Stored as a salted hash; "" clears the PIN
        context.dataStore.edit { it[Keys.VAULT_PIN] = if (pin.isEmpty()) "" else com.quizango.gallery.data.local.VaultSecret.hash(pin) }
    }

    suspend fun setVaultPattern(pattern: String) {
        // Stored as a salted hash; "" clears the pattern
        context.dataStore.edit { it[Keys.VAULT_PATTERN] = if (pattern.isEmpty()) "" else com.quizango.gallery.data.local.VaultSecret.hash(pattern) }
    }

    suspend fun setPrivateLock(locked: Boolean) {
        context.dataStore.edit { it[Keys.IS_PRIVATE_LOCK] = locked }
    }

    suspend fun setPrivateToastShow(shown: Boolean) {
        context.dataStore.edit { it[Keys.IS_PRIVATE_TOAST_SHOW] = shown }
    }

    /** One edit: hashes [value] into the chosen lock, clears the other lock and the lock-out flag (L2). */
    suspend fun saveVaultLock(value: String, pattern: Boolean) {
        val hashed = com.quizango.gallery.data.local.VaultSecret.hash(value)
        context.dataStore.edit {
            it[Keys.VAULT_PIN] = if (pattern) "" else hashed
            it[Keys.VAULT_PATTERN] = if (pattern) hashed else ""
            it[Keys.IS_PRIVATE_LOCK] = false
        }
    }

    /** L10: wipes both locks in one edit. */
    suspend fun clearVaultLock() {
        context.dataStore.edit {
            it[Keys.VAULT_PIN] = ""
            it[Keys.VAULT_PATTERN] = ""
        }
    }

    suspend fun setPatternMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_PATTERN_MODE] = enabled }
    }

    suspend fun setFingerprintEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_FINGERPRINT_ENABLED] = enabled }
    }

    suspend fun setSecurityQuestion(question: String, answer: String) {
        context.dataStore.edit {
            it[Keys.VAULT_QUESTION] = question
            it[Keys.VAULT_ANSWER] = com.quizango.gallery.data.local.VaultSecret.hash(com.quizango.gallery.data.local.VaultSecret.normalizeAnswer(answer))
        }
    }

    suspend fun setThreeSecurityQuestions(
        q1: String, a1: String,
        q2: String, a2: String,
        q3: String, a3: String
    ) {
        context.dataStore.edit {
            it[Keys.VAULT_QUESTION] = q1
            // Answers are stored as salted hashes of the trimmed, lower-cased text
            it[Keys.VAULT_ANSWER] = com.quizango.gallery.data.local.VaultSecret.hash(com.quizango.gallery.data.local.VaultSecret.normalizeAnswer(a1))
            it[Keys.VAULT_QUESTION_2] = q2
            it[Keys.VAULT_ANSWER_2] = com.quizango.gallery.data.local.VaultSecret.hash(com.quizango.gallery.data.local.VaultSecret.normalizeAnswer(a2))
            it[Keys.VAULT_QUESTION_3] = q3
            it[Keys.VAULT_ANSWER_3] = com.quizango.gallery.data.local.VaultSecret.hash(com.quizango.gallery.data.local.VaultSecret.normalizeAnswer(a3))
        }
    }

    // Phase 09 + 12: Settings "Share app usage stats" and the usage-access prompts (2.7.2 AppPreference key names; -1 = never shown)
    private val appInsightsKey = booleanPreferencesKey("app_insights_enabled") // 2.7.2 key (x/a), default on
    private val homeUsageShownKey = intPreferencesKey("usage_access_home_popup_last_shown_session")
    private val usageShownKey = intPreferencesKey("usage_access_popup_last_shown_session")

    /** Only the choice is kept, no usage data is collected or sent; false means opted out, so no usage-access prompt shows. */
    val appInsightsEnabled: Flow<Boolean> = context.dataStore.data.map { it[appInsightsKey] ?: true }

    suspend fun setAppInsightsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[appInsightsKey] = enabled }
    }

    /** Session the Home prompt ([home]) or the one shared by the video, story, Recycle Bin and Settings prompts last showed in. */
    fun usageAccessShownSession(home: Boolean): Flow<Int> =
        context.dataStore.data.map { it[if (home) homeUsageShownKey else usageShownKey] ?: -1 }

    suspend fun setUsageAccessShownSession(home: Boolean, session: Int) {
        context.dataStore.edit { it[if (home) homeUsageShownKey else usageShownKey] = session }
    }
}
