package com.example.typingarticle.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.typingarticle.core.data.local.AppDatabase
import com.example.typingarticle.core.model.Cursor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressAndStatsTest {

    private lateinit var db: AppDatabase
    private lateinit var progressRepo: ProgressRepository
    private lateinit var statRepo: StatisticRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        progressRepo = ProgressRepository(db.progressDao())
        statRepo = StatisticRepository(db.statisticDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testProgressSaveAndReload() = runTest(UnconfinedTestDispatcher()) {
        // 用例 9: 断点：保存后重载，游标精确恢复
        val articleId = "art_test_123"
        val originalCursor = Cursor(sectionIdx = 2, sentenceIdx = 1, wordIdx = 4, charIdx = 0)

        // 保存进度
        progressRepo.save(articleId, originalCursor)

        // 重载进度
        val loadedCursor = progressRepo.load(articleId)
        assertNotNull(loadedCursor)
        assertEquals(2, loadedCursor?.sectionIdx)
        assertEquals(1, loadedCursor?.sentenceIdx)
        assertEquals(4, loadedCursor?.wordIdx)

        // 清理进度
        progressRepo.clear(articleId)
        val afterClear = progressRepo.load(articleId)
        assertNull(afterClear)
    }

    @Test
    fun testStatisticSaveAndQuery() = runTest(UnconfinedTestDispatcher()) {
        statRepo.saveStatistic(
            refType = "article",
            refId = "art_001",
            startedAt = 1000L,
            spendMs = 45000L,
            total = 120,
            wrong = 3
        )

        val stats = statRepo.observeStats("article", "art_001").first()
        assertEquals(1, stats.size)
        assertEquals(120, stats[0].total)
        assertEquals(3, stats[0].wrong)
        assertEquals(45000L, stats[0].spendMs)
    }
}
