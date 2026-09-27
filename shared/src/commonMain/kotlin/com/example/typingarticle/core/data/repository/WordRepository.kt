package com.example.typingarticle.core.data.repository

import com.example.typingarticle.core.data.local.dao.WordBookDao
import com.example.typingarticle.core.data.local.dao.WordDao
import com.example.typingarticle.core.data.local.dao.WordMarkDao
import com.example.typingarticle.core.data.local.dao.WordProgressDao
import com.example.typingarticle.core.data.local.entity.WordBookEntity
import com.example.typingarticle.core.data.local.entity.WordEntity
import com.example.typingarticle.core.data.local.entity.WordProgressEntity
import com.example.typingarticle.core.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 单词默写词库仓库（跨平台）
 */
class WordRepository(
    private val wordBookDao: WordBookDao,
    private val wordDao: WordDao,
    private val wordMarkDao: WordMarkDao,
    private val wordProgressDao: WordProgressDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    fun observeWordBooks(): Flow<List<WordBookEntity>> = wordBookDao.observeWordBooks()

    suspend fun getWordBook(id: String): WordBookEntity? = wordBookDao.getWordBookById(id)

    suspend fun getWords(bookId: String): List<WordEntity> = wordDao.getWordsByBook(bookId)

    suspend fun observeWords(bookId: String): Flow<List<WordEntity>> = wordDao.observeWordsByBook(bookId)

    suspend fun saveWords(bookId: String, words: List<WordEntity>) {
        wordDao.insertWords(words)
    }

    suspend fun saveBook(book: WordBookEntity) {
        wordBookDao.insertWordBook(book)
    }

    suspend fun deleteWordBook(bookId: String) {
        wordDao.deleteWordsByBook(bookId)
        wordBookDao.deleteWordBookById(bookId)
    }

    suspend fun importWords(content: String, bookName: String = "导入词库", format: String = "auto"): WordBookEntity {
        val bookId = "wb_" + newId().take(8)
        val wordList = mutableListOf<WordEntity>()
        val trimmed = content.trim().removePrefix("\uFEFF").trim()

        val isJson = format.equals("json", ignoreCase = true) ||
                (format == "auto" && (trimmed.startsWith("[") || trimmed.startsWith("{")))

        if (isJson) {
            try {
                val root = json.parseToJsonElement(trimmed)
                val items: List<JsonElement> = when (root) {
                    is JsonArray -> root
                    is JsonObject -> (root["words"] as? JsonArray) ?: emptyList()
                    else -> emptyList()
                }
                items.forEachIndexed { i, el ->
                    parseWordElement(el, bookId, i)?.let { wordList.add(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
            lines.forEachIndexed { idx, line ->
                val parts = when {
                    line.contains("\t") -> line.split("\t")
                    line.contains(",") && !line.startsWith("#") -> line.split(",")
                    else -> listOf(line)
                }
                val word = parts.getOrNull(0)?.trim() ?: ""
                val phonetic0 = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
                val phonetic1 = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
                val trans = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }

                if (word.isNotEmpty() && !word.startsWith("#") && !word.equals("单词", ignoreCase = true)) {
                    wordList.add(
                        WordEntity(bookId = bookId, idx = idx, word = word, phonetic0 = phonetic0, phonetic1 = phonetic1, trans = trans)
                    )
                }
            }
        }

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

    private fun parseWordElement(el: JsonElement, bookId: String, idx: Int): WordEntity? {
        return when (el) {
            is JsonPrimitive -> {
                val w = el.content.trim()
                if (w.isEmpty()) null else WordEntity(bookId = bookId, idx = idx, word = w)
            }
            is JsonObject -> {
                val word = el["word"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (word.isEmpty()) null
                else WordEntity(
                    bookId = bookId,
                    idx = idx,
                    word = word,
                    phonetic0 = el["phonetic0"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() },
                    phonetic1 = el["phonetic1"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() },
                    trans = el["trans"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() },
                    sentencesJson = el["sentences"]?.toString(),
                    phrasesJson = el["phrases"]?.toString()
                )
            }
            else -> null
        }
    }

    /** 依据一组单词创建一个新的可见词库（音标/释义从已存在的同名词条复用） */
    suspend fun createBookFromWords(name: String, words: List<String>): WordBookEntity {
        val bookId = "wb_" + newId().take(8)
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

    /** 获取所有标记为错词的词条列表（支持错词重练） */
    suspend fun listWrongWords(): List<String> = wordMarkDao.getMarksByKind("wrong").map { it.word }

    /** 仅重练错词 */
    suspend fun retryWrongOnly(bookId: String): List<WordEntity> {
        val wrongWordTexts = listWrongWords().map { it.lowercase() }.toSet()
        if (wrongWordTexts.isEmpty()) return emptyList()

        val allWords = wordDao.getWordsByBook(bookId)
        val matchedWords = allWords.filter { it.word.lowercase() in wrongWordTexts }

        val result = if (matchedWords.isNotEmpty()) {
            matchedWords
        } else {
            wrongWordTexts.mapIndexed { index, wordText ->
                val existing = wordDao.getWordByText(wordText)
                existing?.copy(idx = index) ?: WordEntity(bookId = bookId, idx = index, word = wordText, trans = "错词重练")
            }
        }

        return result.mapIndexed { index, item -> item.copy(idx = index) }
    }

    suspend fun getProgress(bookId: String): Int = wordProgressDao.getWordProgress(bookId)?.idx ?: 0

    suspend fun saveProgress(bookId: String, idx: Int) {
        wordProgressDao.saveWordProgress(WordProgressEntity(bookId = bookId, idx = idx))
    }
}
