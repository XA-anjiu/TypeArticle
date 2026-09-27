package com.example.typingarticle.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.typingarticle.core.audio.AudioService
import com.example.typingarticle.core.data.repository.ContentRepository
import com.example.typingarticle.core.data.repository.WordRepository
import com.example.typingarticle.core.model.Settings
import com.example.typingarticle.core.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 设置页：书库条目 */
data class BookItemUi(
    val id: String,
    val name: String,
    val length: Int,
    val source: String
)

/** 设置页：词库条目 */
data class WordBookItemUi(
    val id: String,
    val name: String,
    val source: String
)

/**
 * 设置页 ViewModel：书库 / 词库 列表、删除、导入。
 * 纯本地，无 Compose 依赖。
 */
class SettingsViewModel(
    private val contentRepository: ContentRepository,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val audioService: AudioService
) : ViewModel() {

    val settings: StateFlow<Settings> =
        settingsRepository.observe()
            .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    fun setKeySoundEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(keySoundEnabled = value) } }
        if (value) audioService.keyClick()
    }

    fun setErrorSoundEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(errorSoundEnabled = value) } }
        if (value) audioService.errorBeep()
    }

    fun setKeyboardSound(name: String) {
        audioService.previewKeyboardSound(name)
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(keyboardSound = name) } }
    }

    val books: StateFlow<List<BookItemUi>> =
        contentRepository.observeBooks()
            .map { list -> list.map { BookItemUi(it.id, it.name, it.length, it.source) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val wordBooks: StateFlow<List<WordBookItemUi>> =
        wordRepository.observeWordBooks()
            .map { list -> list.map { WordBookItemUi(it.id, it.name, it.source) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun deleteBook(id: String) {
        viewModelScope.launch { contentRepository.deleteBook(id) }
    }

    fun deleteWordBook(id: String) {
        viewModelScope.launch { wordRepository.deleteWordBook(id) }
    }

    fun importArticles(json: String, name: String = "导入文章包") {
        viewModelScope.launch { contentRepository.importPackage(json, name) }
    }

    fun importWords(content: String, name: String = "导入词库", format: String = "auto") {
        viewModelScope.launch { wordRepository.importWords(content, name, format) }
    }
}
