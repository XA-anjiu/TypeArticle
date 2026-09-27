package com.example.desktop

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.typingarticle.core.audio.AudioService
import com.example.typingarticle.core.data.local.AppDatabase
import com.example.typingarticle.core.data.repository.ContentRepository
import com.example.typingarticle.core.data.repository.ProgressRepository
import com.example.typingarticle.core.data.repository.StatisticRepository
import com.example.typingarticle.core.data.repository.WordMarkRepository
import com.example.typingarticle.core.data.repository.WordRepository
import com.example.typingarticle.core.data.repository.WrongWordRepository
import com.example.typingarticle.core.engine.KeyMapper
import com.example.typingarticle.core.settings.SettingsRepository
import java.io.File

/** 桌面端依赖容器（对应 Android 的 TypingDiContainer） */
class DesktopContainer(private val appDir: File) {

    val database: AppDatabase by lazy {
        appDir.mkdirs()
        Room.databaseBuilder<AppDatabase>(name = File(appDir, "typearticle.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build()
    }

    val contentRepository: ContentRepository by lazy {
        ContentRepository(database.bookDao(), database.articleDao())
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(database.progressDao())
    }

    val statisticRepository: StatisticRepository by lazy {
        StatisticRepository(database.statisticDao())
    }

    val wordMarkRepository: WordMarkRepository by lazy {
        WordMarkRepository(database.wordMarkDao())
    }

    val wordRepository: WordRepository by lazy {
        WordRepository(database.wordBookDao(), database.wordDao(), database.wordMarkDao(), database.wordProgressDao())
    }

    val wrongWordRepository: WrongWordRepository by lazy {
        WrongWordRepository(database.wrongWordDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        DesktopSettingsRepository(File(appDir, "settings.json"))
    }

    val audioService: AudioService by lazy { DesktopAudioService(appDir) }

    val keyMapper: KeyMapper by lazy { KeyMapper() }
}
