package com.example.typingarticle.core.data.repository

import com.example.typingarticle.core.data.local.dao.WordMarkDao
import com.example.typingarticle.core.data.local.entity.WordMarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 单词标记（收藏/错词/已掌握）仓库
 */
class WordMarkRepository(
    private val wordMarkDao: WordMarkDao
) {
    suspend fun mark(word: String, kind: String) {
        wordMarkDao.insertMark(
            WordMarkEntity(
                word = word.trim(),
                kind = kind,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    fun observe(kind: String): Flow<List<String>> {
        return wordMarkDao.observeMarksByKind(kind).map { list -> list.map { it.word } }
    }

    suspend fun getMarks(kind: String): List<String> {
        return wordMarkDao.getMarksByKind(kind).map { it.word }
    }

    suspend fun remove(word: String) {
        wordMarkDao.deleteMarkByWord(word.trim())
    }

    suspend fun clear(kind: String) {
        wordMarkDao.clearMarksByKind(kind)
    }
}
