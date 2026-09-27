package com.example.typingarticle.core.data.repository

import com.example.typingarticle.core.data.local.dao.WrongWordDao
import com.example.typingarticle.core.data.local.entity.WrongWordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 错词统计条目 */
data class WrongWordStat(
    val word: String,
    val count: Int
)

/**
 * 错词统计仓库（隐藏词库）：
 * - 同一单词只存一条，重复出错只累加次数
 * - 仅用于「错词统计」卡片与「一键生成错词词库」
 */
class WrongWordRepository(
    private val wrongWordDao: WrongWordDao
) {
    fun observe(): Flow<List<WrongWordStat>> =
        wrongWordDao.observe().map { list -> list.map { WrongWordStat(it.word, it.count) } }

    suspend fun all(): List<WrongWordStat> =
        wrongWordDao.all().map { WrongWordStat(it.word, it.count) }

    /** 记录一次错误：不存在则新增，存在则次数 +1 */
    suspend fun record(word: String) {
        val w = word.trim()
        if (w.isEmpty()) return
        val now = System.currentTimeMillis()
        val rowId = wrongWordDao.insertIgnore(WrongWordEntity(word = w, count = 1, updatedAt = now))
        if (rowId == -1L) {
            wrongWordDao.bump(w, now)
        }
    }

    suspend fun remove(word: String) {
        wrongWordDao.delete(word.trim())
    }

    suspend fun clear() {
        wrongWordDao.clear()
    }
}
