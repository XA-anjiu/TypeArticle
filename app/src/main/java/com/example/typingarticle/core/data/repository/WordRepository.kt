package com.example.typingarticle.core.data.repository

import com.example.typingarticle.core.data.local.dao.WordBookDao
import com.example.typingarticle.core.data.local.dao.WordDao
import com.example.typingarticle.core.data.local.dao.WordMarkDao
import com.example.typingarticle.core.data.local.dao.WordProgressDao
import com.example.typingarticle.core.data.local.entity.WordBookEntity
import com.example.typingarticle.core.data.local.entity.WordEntity
import com.example.typingarticle.core.data.local.entity.WordProgressEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 单词默写词库仓库
 *
 * 遵循 Section 12.5.1 契约：
 * - 导入支持 .txt（一行一词）/ .json（string[] 或 { word }[]）/ .csv 或表格列结构
 * - 支持错词本查询与错词一键重练
 */
class WordRepository(
    private val wordBookDao: WordBookDao,
    private val wordDao: WordDao,
    private val wordMarkDao: WordMarkDao,
    private val wordProgressDao: WordProgressDao
) {
    fun observeWordBooks(): Flow<List<WordBookEntity>> {
        return wordBookDao.observeWordBooks()
    }

    suspend fun getWordBook(id: String): WordBookEntity? {
        return wordBookDao.getWordBookById(id)
    }

    suspend fun getWords(bookId: String): List<WordEntity> {
        return wordDao.getWordsByBook(bookId)
    }

    suspend fun observeWords(bookId: String): Flow<List<WordEntity>> {
        return wordDao.observeWordsByBook(bookId)
    }

    suspend fun saveWords(bookId: String, words: List<WordEntity>) {
        wordDao.insertWords(words)
    }

    suspend fun saveBook(book: WordBookEntity) {
        wordBookDao.insertWordBook(book)
    }

    /** 删除词库：连同其下所有词条一并删除 */
    suspend fun deleteWordBook(bookId: String) {
        wordDao.deleteWordsByBook(bookId)
        wordBookDao.deleteWordBookById(bookId)
    }

    /**
     * 导入词库（支持 txt / json / csv）
     */
    suspend fun importWords(content: String, bookName: String = "导入词库", format: String = "auto"): WordBookEntity {
        val bookId = "wb_" + UUID.randomUUID().toString().take(8)
        val wordList = mutableListOf<WordEntity>()
        val trimmed = content.trim()

        val isJson = format.equals("json", ignoreCase = true) ||
                (format == "auto" && (trimmed.startsWith("[") || trimmed.startsWith("{")))

        if (isJson) {
            try {
                if (trimmed.startsWith("[")) {
                    val array = JSONArray(trimmed)
                    for (i in 0 until array.length()) {
                        val item = array.get(i)
                        if (item is String) {
                            wordList.add(WordEntity(bookId = bookId, idx = i, word = item.trim()))
                        } else if (item is JSONObject) {
                            wordList.add(parseWordJson(item, bookId, i))
                        }
                    }
                } else if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    val wordsArr = obj.getJSONArray("words")
                    for (i in 0 until wordsArr.length()) {
                        val item = wordsArr.get(i)
                        if (item is String) {
                            wordList.add(WordEntity(bookId = bookId, idx = i, word = item.trim()))
                        } else if (item is JSONObject) {
                            wordList.add(parseWordJson(item, bookId, i))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // 按行解析（txt 或 csv / tsv）
            val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
            lines.forEachIndexed { idx, line ->
                val parts = if (line.contains("\t")) {
                    line.split("\t")
                } else if (line.contains(",") && !line.startsWith("#")) {
                    line.split(",")
                } else {
                    listOf(line)
                }

                val word = parts.getOrNull(0)?.trim() ?: ""
                val phonetic0 = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
                val phonetic1 = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
                val trans = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }

                if (word.isNotEmpty() && !word.startsWith("#") && !word.equals("单词", ignoreCase = true)) {
                    wordList.add(
                        WordEntity(
                            bookId = bookId,
                            idx = idx,
                            word = word,
                            phonetic0 = phonetic0,
                            phonetic1 = phonetic1,
                            trans = trans
                        )
                    )
                }
            }
        }

        // 去重并重新编号
        val uniqueWords = wordList.distinctBy { it.word.lowercase() }
            .mapIndexed { index, item -> item.copy(idx = index) }

        val bookEntity = WordBookEntity(
            id = bookId,
            name = bookName,
            description = "共 ${uniqueWords.size} 词",
            source = "import"
        )

        wordBookDao.insertWordBook(bookEntity)
        wordDao.insertWords(uniqueWords)

        return bookEntity
    }

    /** 由给定单词创建新的可见词库（复用库中已有词条的音标 / 释义） */
    suspend fun createBookFromWords(name: String, words: List<String>): WordBookEntity {
        val bookId = "wb_" + UUID.randomUUID().toString().take(8)
        val unique = words.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        val entities = unique.mapIndexed { index, w ->
            val existing = wordDao.getWordByText(w)
            WordEntity(
                bookId = bookId,
                idx = index,
                word = w,
                phonetic0 = existing?.phonetic0,
                phonetic1 = existing?.phonetic1,
                trans = existing?.trans
            )
        }
        val book = WordBookEntity(
            id = bookId,
            name = name,
            description = "共 ${entities.size} 词",
            source = "import"
        )
        wordBookDao.insertWordBook(book)
        if (entities.isNotEmpty()) wordDao.insertWords(entities)
        return book
    }

    private fun parseWordJson(obj: JSONObject, bookId: String, idx: Int): WordEntity {
        return WordEntity(
            bookId = bookId,
            idx = idx,
            word = obj.getString("word").trim(),
            phonetic0 = obj.optString("phonetic0").takeIf { it.isNotEmpty() },
            phonetic1 = obj.optString("phonetic1").takeIf { it.isNotEmpty() },
            trans = obj.optString("trans").takeIf { it.isNotEmpty() },
            sentencesJson = obj.optJSONArray("sentences")?.toString(),
            phrasesJson = obj.optJSONArray("phrases")?.toString()
        )
    }

    /**
     * 获取所有标记为错词的词条列表（支持错词重练）
     */
    suspend fun listWrongWords(): List<String> {
        return wordMarkDao.getMarksByKind("wrong").map { it.word }
    }

    /**
     * 仅重练错词：根据错词本筛选当前库中的词，如果库中未包含则现场生成
     */
    suspend fun retryWrongOnly(bookId: String): List<WordEntity> {
        val wrongWordTexts = listWrongWords().map { it.lowercase() }.toSet()
        if (wrongWordTexts.isEmpty()) return emptyList()

        val allWords = wordDao.getWordsByBook(bookId)
        val matchedWords = allWords.filter { it.word.lowercase() in wrongWordTexts }

        // 若当前词库完全不包含错词，则从数据库中全量错词中匹配
        val result = if (matchedWords.isNotEmpty()) {
            matchedWords
        } else {
            wrongWordTexts.mapIndexed { index, wordText ->
                val existing = wordDao.getWordByText(wordText)
                existing?.copy(idx = index) ?: WordEntity(
                    bookId = bookId,
                    idx = index,
                    word = wordText,
                    trans = "错词重练"
                )
            }
        }

        return result.mapIndexed { index, item -> item.copy(idx = index) }
    }

    suspend fun getProgress(bookId: String): Int {
        return wordProgressDao.getWordProgress(bookId)?.idx ?: 0
    }

    suspend fun saveProgress(bookId: String, idx: Int) {
        wordProgressDao.saveWordProgress(WordProgressEntity(bookId = bookId, idx = idx))
    }
}
