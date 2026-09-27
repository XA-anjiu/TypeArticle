package com.example.typingarticle.core.data.repository

import com.example.typingarticle.core.data.local.dao.ProgressDao
import com.example.typingarticle.core.data.local.entity.ProgressEntity
import com.example.typingarticle.core.model.Cursor
import kotlinx.coroutines.flow.Flow

/**
 * 练习进度断点续练仓库
 */
class ProgressRepository(
    private val progressDao: ProgressDao
) {
    fun observeProgress(articleId: String): Flow<ProgressEntity?> {
        return progressDao.observeProgress(articleId)
    }

    suspend fun load(articleId: String): Cursor? {
        val entity = progressDao.getProgress(articleId) ?: return null
        return Cursor(
            sectionIdx = entity.sectionI,
            sentenceIdx = entity.sentenceI,
            wordIdx = entity.wordI,
            charIdx = 0
        )
    }

    suspend fun save(articleId: String, cursor: Cursor) {
        progressDao.saveProgress(
            ProgressEntity(
                articleId = articleId,
                sectionI = cursor.sectionIdx,
                sentenceI = cursor.sentenceIdx,
                wordI = cursor.wordIdx,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clear(articleId: String) {
        progressDao.clearProgress(articleId)
    }
}
