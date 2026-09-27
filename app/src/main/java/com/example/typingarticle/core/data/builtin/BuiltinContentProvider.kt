package com.example.typingarticle.core.data.builtin

import android.content.Context
import com.example.typingarticle.core.data.local.entity.WordBookEntity
import com.example.typingarticle.core.data.local.entity.WordEntity
import com.example.typingarticle.core.data.repository.ContentRepository
import com.example.typingarticle.core.data.repository.WordRepository
import com.example.typingarticle.core.model.Book
import org.json.JSONArray

/**
 * 内置精选内容提供源
 *
 * - 首次启动自动写入《英语二写作真题范文 2010–2025》（32 篇，来自 assets）
 * - 内置三档考研英语二写作词表（S 必背 / A 话题备用 / B 仅备查），来自 assets 的 words 目录
 */
object BuiltinContentProvider {

    const val BUILTIN_BOOK_ID = "book_english_writing_2010_2025"
    private const val BUILTIN_ARTICLES_ASSET = "articles_2010_2025.json"

    /** 默认打开的默认词库（S 必背） */
    const val DEFAULT_WORDBOOK_ID = "wb_en2_s"

    /** 早期版本的示例词库，启动时清理 */
    private const val LEGACY_WORDBOOK_ID = "wb_core_vocabulary"

    private data class BuiltinWordBook(
        val id: String,
        val name: String,
        val description: String,
        val asset: String,
    )

    private val BUILTIN_WORD_BOOKS = listOf(
        BuiltinWordBook(
            id = "wb_en2_s",
            name = "S · 必背",
            description = "考研英语二写作必背 246 词（图表描述 / 书信框架 / 议论转折），要求背到能默写",
            asset = "words/s_must.json",
        ),
        BuiltinWordBook(
            id = "wb_en2_a",
            name = "A · 话题备用",
            description = "按话题归堆：教育 / 环保 / 科技 / 就业 / 老龄化 / 健康 / 文化，考到哪个话题翻哪批",
            asset = "words/a_topic.json",
        ),
        BuiltinWordBook(
            id = "wb_en2_b",
            name = "B · 仅备查",
            description = "低频行业词 / 同根次要形式 / 品格修养词等，不进背词书，真考到现查",
            asset = "words/b_reference.json",
        ),
    )

    suspend fun populateIfEmpty(
        context: Context,
        contentRepo: ContentRepository,
        wordRepo: WordRepository
    ) {
        // 1. 内置文章包（真实内容，来自 assets）
        if (contentRepo.getBook(BUILTIN_BOOK_ID) == null) {
            val book = buildBuiltinArticleBook(context, contentRepo)
            if (book != null && book.articles.isNotEmpty()) {
                contentRepo.saveBookWithArticles(book)
            }
        }

        // 2. 清理旧版示例词库
        if (wordRepo.getWordBook(LEGACY_WORDBOOK_ID) != null) {
            wordRepo.deleteWordBook(LEGACY_WORDBOOK_ID)
        }

        // 3. 内置三档词表
        BUILTIN_WORD_BOOKS.forEach { wb ->
            if (wordRepo.getWordBook(wb.id) == null) {
                wordRepo.saveBook(
                    WordBookEntity(
                        id = wb.id,
                        name = wb.name,
                        description = wb.description,
                        source = "builtin"
                    )
                )
                val words = parseWordList(readAsset(context, wb.asset), wb.id)
                if (words.isNotEmpty()) wordRepo.saveWords(wb.id, words)
            }
        }
    }

    private fun readAsset(context: Context, name: String): String? {
        return try {
            context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildBuiltinArticleBook(context: Context, contentRepo: ContentRepository): Book? {
        val json = readAsset(context, BUILTIN_ARTICLES_ASSET) ?: return null
        val articles = contentRepo.parseArticleArray(json, BUILTIN_BOOK_ID)
        if (articles.isEmpty()) return null
        return Book(
            id = BUILTIN_BOOK_ID,
            name = "英语二写作真题范文 2010–2025",
            description = "共 ${articles.size} 篇历年考研英语二应用文与图表作文范文，段句严密对照",
            length = articles.size,
            source = "builtin",
            articles = articles
        )
    }

    /** 解析词表 JSON：[{word, phonetic0, trans}] */
    private fun parseWordList(raw: String?, bookId: String): List<WordEntity> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val w = o.optString("word").trim()
                if (w.isEmpty()) return@mapNotNull null
                WordEntity(
                    bookId = bookId,
                    idx = i,
                    word = w,
                    phonetic0 = o.optString("phonetic0").takeIf { it.isNotBlank() },
                    phonetic1 = o.optString("phonetic1").takeIf { it.isNotBlank() },
                    trans = o.optString("trans").takeIf { it.isNotBlank() },
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
