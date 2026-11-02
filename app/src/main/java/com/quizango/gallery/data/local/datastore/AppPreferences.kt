package com.quizango.gallery.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gallery_preferences")

class AppPreferences(private val context: Context) {

    private object Keys {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val HAS_COMPLETED_WELCOME = booleanPreferencesKey("has_completed_welcome")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = intPreferencesKey("theme_mode") // 0: Light, 1: Dark, 2: System
        val MEDIA_COL_COUNT = intPreferencesKey("media_col_count")
        val AUTO_DELETE_TRASH = booleanPreferencesKey("auto_delete_trash_30_days")
        val VAULT_PIN = stringPreferencesKey("vault_pin")
        val VAULT_PATTERN = stringPreferencesKey("vault_pattern")
        val IS_PATTERN_MODE = booleanPreferencesKey("is_vault_pattern_mode")
        val IS_FINGERPRINT_ENABLED = booleanPreferencesKey("is_fingerprint_enabled")
        val VAULT_QUESTION = stringPreferencesKey("vault_recovery_question")
        val VAULT_ANSWER = stringPreferencesKey("vault_recovery_answer")
        val VAULT_QUESTION_2 = stringPreferencesKey("vault_recovery_question_2")
        val VAULT_ANSWER_2 = stringPreferencesKey("vault_recovery_answer_2")
        val VAULT_QUESTION_3 = stringPreferencesKey("vault_recovery_question_3")
        val VAULT_ANSWER_3 = stringPreferencesKey("vault_recovery_answer_3")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_FIRST_LAUNCH] ?: true }
    val hasCompletedWelcome: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAS_COMPLETED_WELCOME] ?: false }
    val appLanguage: Flow<String> = context.dataStore.data.map { it[Keys.APP_LANGUAGE] ?: "en" }
    val themeMode: Flow<Int> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: 2 }
    val mediaColCount: Flow<Int> = context.dataStore.data.map { it[Keys.MEDIA_COL_COUNT] ?: 4 }
    val autoDeleteTrash: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_DELETE_TRASH] ?: true }
    val vaultPin: Flow<String?> = context.dataStore.data.map { it[Keys.VAULT_PIN] }
    val vaultPattern: Flow<String?> = context.dataStore.data.map { it[Keys.VAULT_PATTERN] }
    val isPatternMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_PATTERN_MODE] ?: false }
    val isFingerprintEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_FINGERPRINT_ENABLED] ?: false }
    val vaultQuestion: Flow<String?> = context.dataStore.data.map { it[Keys.VAULT_QUESTION] }
    val vaultAnswer: Flow<String?> = context.dataStore.data.map { it[Keys.VAULT_ANSWER] }

    suspend fun setFirstLaunch(completed: Boolean) {
        context.dataStore.edit { it[Keys.IS_FIRST_LAUNCH] = completed }
    }

    suspend fun setCompletedWelcome(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_WELCOME] = completed }
    }

    suspend fun setLanguage(langCode: String) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = langCode }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setMediaColCount(count: Int) {
        context.dataStore.edit { it[Keys.MEDIA_COL_COUNT] = count }
    }

    suspend fun setAutoDeleteTrash(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DELETE_TRASH] = enabled }
    }

    suspend fun setVaultPin(pin: String) {
        context.dataStore.edit { it[Keys.VAULT_PIN] = pin }
    }

    suspend fun setVaultPattern(pattern: String) {
        context.dataStore.edit { it[Keys.VAULT_PATTERN] = pattern }
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
            it[Keys.VAULT_ANSWER] = answer
        }
    }

    suspend fun setThreeSecurityQuestions(
        q1: String, a1: String,
        q2: String, a2: String,
        q3: String, a3: String
    ) {
        context.dataStore.edit {
            it[Keys.VAULT_QUESTION] = q1
            it[Keys.VAULT_ANSWER] = a1
            it[Keys.VAULT_QUESTION_2] = q2
            it[Keys.VAULT_ANSWER_2] = a2
            it[Keys.VAULT_QUESTION_3] = q3
            it[Keys.VAULT_ANSWER_3] = a3
        }
    }
}
