package com.example.desktop

import com.example.typingarticle.core.model.Settings
import com.example.typingarticle.core.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.io.File

/** 桌面端设置存储：本地 JSON 文件（~/.typearticle/settings.json） */
class DesktopSettingsRepository(private val file: File) : SettingsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val state = MutableStateFlow(load())

    private fun load(): Settings = try {
        if (file.exists()) json.decodeFromString(Settings.serializer(), file.readText(Charsets.UTF_8))
        else Settings()
    } catch (_: Exception) {
        Settings()
    }

    override fun observe(): Flow<Settings> = state.asStateFlow()

    override suspend fun updateSettings(transform: (Settings) -> Settings) {
        val updated = transform(state.value)
        state.update { updated }
        persist(updated)
    }

    private fun persist(s: Settings) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(Settings.serializer(), s), Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }
}
