package com.example.typingarticle.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.typingarticle.core.model.Settings
import com.example.typingarticle.core.model.WordMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "typing_settings")

/**
 * 设置存储仓（基于 Jetpack DataStore Preferences）
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val DICTATION = booleanPreferencesKey("dictation")
        val TRANSLATE = booleanPreferencesKey("translate")
        val IGNORE_CASE = booleanPreferencesKey("ignore_case")
        val IGNORE_SYMBOL = booleanPreferencesKey("ignore_symbol")
        val IGNORE_SIMPLE_WORD = booleanPreferencesKey("ignore_simple_word")
        val NAME_LIST = stringSetPreferencesKey("name_list")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("key_sound_enabled")
        val ERROR_SOUND_ENABLED = booleanPreferencesKey("error_sound_enabled")
        val KEYBOARD_SOUND = stringPreferencesKey("keyboard_sound")
        val VOLUME = floatPreferencesKey("volume")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val FONT_SIZE = intPreferencesKey("font_size")
        val THEME = stringPreferencesKey("theme")
        val WORD_MODE = stringPreferencesKey("word_mode")
        val WORD_IGNORE_CASE = booleanPreferencesKey("word_ignore_case")
        val SHOW_MEANING = booleanPreferencesKey("show_meaning")
        val SHOW_PHONETIC = booleanPreferencesKey("show_phonetic")
    }

    private val defaultSettings = Settings()

    fun observe(): Flow<Settings> {
        return context.dataStore.data.map { pref ->
            Settings(
                dictation = pref[Keys.DICTATION] ?: defaultSettings.dictation,
                translate = pref[Keys.TRANSLATE] ?: defaultSettings.translate,
                ignoreCase = pref[Keys.IGNORE_CASE] ?: defaultSettings.ignoreCase,
                ignoreSymbol = pref[Keys.IGNORE_SYMBOL] ?: defaultSettings.ignoreSymbol,
                ignoreSimpleWord = pref[Keys.IGNORE_SIMPLE_WORD] ?: defaultSettings.ignoreSimpleWord,
                nameList = pref[Keys.NAME_LIST] ?: defaultSettings.nameList,
                keySoundEnabled = pref[Keys.KEY_SOUND_ENABLED] ?: defaultSettings.keySoundEnabled,
                errorSoundEnabled = pref[Keys.ERROR_SOUND_ENABLED] ?: defaultSettings.errorSoundEnabled,
                keyboardSound = pref[Keys.KEYBOARD_SOUND] ?: defaultSettings.keyboardSound,
                volume = pref[Keys.VOLUME] ?: defaultSettings.volume,
                speechRate = pref[Keys.SPEECH_RATE] ?: defaultSettings.speechRate,
                fontSize = pref[Keys.FONT_SIZE] ?: defaultSettings.fontSize,
                theme = pref[Keys.THEME] ?: defaultSettings.theme,
                wordMode = pref[Keys.WORD_MODE]?.let { runCatching { WordMode.valueOf(it) }.getOrNull() }
                    ?: defaultSettings.wordMode,
                wordIgnoreCase = pref[Keys.WORD_IGNORE_CASE] ?: defaultSettings.wordIgnoreCase,
                showMeaning = pref[Keys.SHOW_MEANING] ?: defaultSettings.showMeaning,
                showPhonetic = pref[Keys.SHOW_PHONETIC] ?: defaultSettings.showPhonetic
            )
        }
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        context.dataStore.edit { pref ->
            val current = Settings(
                dictation = pref[Keys.DICTATION] ?: defaultSettings.dictation,
                translate = pref[Keys.TRANSLATE] ?: defaultSettings.translate,
                ignoreCase = pref[Keys.IGNORE_CASE] ?: defaultSettings.ignoreCase,
                ignoreSymbol = pref[Keys.IGNORE_SYMBOL] ?: defaultSettings.ignoreSymbol,
                ignoreSimpleWord = pref[Keys.IGNORE_SIMPLE_WORD] ?: defaultSettings.ignoreSimpleWord,
                nameList = pref[Keys.NAME_LIST] ?: defaultSettings.nameList,
                keySoundEnabled = pref[Keys.KEY_SOUND_ENABLED] ?: defaultSettings.keySoundEnabled,
                errorSoundEnabled = pref[Keys.ERROR_SOUND_ENABLED] ?: defaultSettings.errorSoundEnabled,
                keyboardSound = pref[Keys.KEYBOARD_SOUND] ?: defaultSettings.keyboardSound,
                volume = pref[Keys.VOLUME] ?: defaultSettings.volume,
                speechRate = pref[Keys.SPEECH_RATE] ?: defaultSettings.speechRate,
                fontSize = pref[Keys.FONT_SIZE] ?: defaultSettings.fontSize,
                theme = pref[Keys.THEME] ?: defaultSettings.theme,
                wordMode = pref[Keys.WORD_MODE]?.let { runCatching { WordMode.valueOf(it) }.getOrNull() }
                    ?: defaultSettings.wordMode,
                wordIgnoreCase = pref[Keys.WORD_IGNORE_CASE] ?: defaultSettings.wordIgnoreCase,
                showMeaning = pref[Keys.SHOW_MEANING] ?: defaultSettings.showMeaning,
                showPhonetic = pref[Keys.SHOW_PHONETIC] ?: defaultSettings.showPhonetic
            )
            val updated = transform(current)
            pref[Keys.DICTATION] = updated.dictation
            pref[Keys.TRANSLATE] = updated.translate
            pref[Keys.IGNORE_CASE] = updated.ignoreCase
            pref[Keys.IGNORE_SYMBOL] = updated.ignoreSymbol
            pref[Keys.IGNORE_SIMPLE_WORD] = updated.ignoreSimpleWord
            pref[Keys.NAME_LIST] = updated.nameList
            pref[Keys.KEY_SOUND_ENABLED] = updated.keySoundEnabled
            pref[Keys.ERROR_SOUND_ENABLED] = updated.errorSoundEnabled
            pref[Keys.KEYBOARD_SOUND] = updated.keyboardSound
            pref[Keys.VOLUME] = updated.volume
            pref[Keys.SPEECH_RATE] = updated.speechRate
            pref[Keys.FONT_SIZE] = updated.fontSize
            pref[Keys.THEME] = updated.theme
            pref[Keys.WORD_MODE] = updated.wordMode.name
            pref[Keys.WORD_IGNORE_CASE] = updated.wordIgnoreCase
            pref[Keys.SHOW_MEANING] = updated.showMeaning
            pref[Keys.SHOW_PHONETIC] = updated.showPhonetic
        }
    }
}

