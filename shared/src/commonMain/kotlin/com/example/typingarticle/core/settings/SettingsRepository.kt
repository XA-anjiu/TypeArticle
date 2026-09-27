package com.example.typingarticle.core.settings

import com.example.typingarticle.core.model.Settings
import kotlinx.coroutines.flow.Flow

/**
 * 设置存储抽象（跨平台）。
 */
interface SettingsRepository {
    fun observe(): Flow<Settings>
    suspend fun updateSettings(transform: (Settings) -> Settings)
}
