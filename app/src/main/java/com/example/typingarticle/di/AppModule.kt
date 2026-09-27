package com.example.typingarticle.di

import android.content.Context
import com.example.typingarticle.core.audio.AndroidAudioService
import com.example.typingarticle.core.audio.AudioService
import com.example.typingarticle.core.data.local.AppDatabase
import com.example.typingarticle.core.data.local.dao.ArticleDao
import com.example.typingarticle.core.data.local.dao.BookDao
import com.example.typingarticle.core.data.local.dao.ProgressDao
import com.example.typingarticle.core.data.local.dao.StatisticDao
import com.example.typingarticle.core.data.local.dao.WordBookDao
import com.example.typingarticle.core.data.local.dao.WordDao
import com.example.typingarticle.core.data.local.dao.WordMarkDao
import com.example.typingarticle.core.data.local.dao.WordProgressDao
import com.example.typingarticle.core.data.local.dao.WrongWordDao
import com.example.typingarticle.core.data.repository.ContentRepository
import com.example.typingarticle.core.data.repository.ProgressRepository
import com.example.typingarticle.core.data.repository.StatisticRepository
import com.example.typingarticle.core.data.repository.WordMarkRepository
import com.example.typingarticle.core.data.repository.WordRepository
import com.example.typingarticle.core.data.repository.WrongWordRepository
import com.example.typingarticle.core.engine.KeyMapper
import com.example.typingarticle.core.settings.SettingsRepository

/**
 * 应用级依赖注入容器与模块提供者
 */
class TypingDiContainer(val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val bookDao: BookDao by lazy { database.bookDao() }
    val articleDao: ArticleDao by lazy { database.articleDao() }
    val progressDao: ProgressDao by lazy { database.progressDao() }
    val statisticDao: StatisticDao by lazy { database.statisticDao() }
    val wordMarkDao: WordMarkDao by lazy { database.wordMarkDao() }
    val wordBookDao: WordBookDao by lazy { database.wordBookDao() }
    val wordDao: WordDao by lazy { database.wordDao() }
    val wordProgressDao: WordProgressDao by lazy { database.wordProgressDao() }
    val wrongWordDao: WrongWordDao by lazy { database.wrongWordDao() }

    val contentRepository: ContentRepository by lazy {
        ContentRepository(bookDao, articleDao)
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(progressDao)
    }

    val wordRepository: WordRepository by lazy {
        WordRepository(wordBookDao, wordDao, wordMarkDao, wordProgressDao)
    }

    val wordMarkRepository: WordMarkRepository by lazy {
        WordMarkRepository(wordMarkDao)
    }

    val wrongWordRepository: WrongWordRepository by lazy {
        WrongWordRepository(wrongWordDao)
    }

    val statisticRepository: StatisticRepository by lazy {
        StatisticRepository(statisticDao)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val audioService: AudioService by lazy {
        AndroidAudioService(context)
    }

    val keyMapper: KeyMapper by lazy {
        KeyMapper()
    }
}
