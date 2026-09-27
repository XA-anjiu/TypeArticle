package com.example.desktop

import com.example.typingarticle.core.data.WordJson
import com.example.typingarticle.core.data.local.entity.WordBookEntity
import com.example.typingarticle.core.data.repository.ContentRepository
import com.example.typingarticle.core.data.repository.WordRepository
import com.example.typingarticle.core.model.Book
import java.io.File

/**
 * 桌面端内置内容：从打包资源读取 32 篇范文与三档词表，首次启动写入本地库。
 */
object DesktopBuiltinContent {

    const val BUILTIN_BOOK_ID = "book_english_writing_2010_2025"
    const val DEFAULT_WORDBOOK_ID = "wb_en2_s"
    private const val BUILTIN_ARTICLES_ASSET = "/articles_2010_2025.json"
    private const val LEGACY_WORDBOOK_ID = "wb_core_vocabulary"

    /** 内置内容版本：内容资源有改动时 +1，启动时会重导内置书库/词库 */
    private const val CONTENT_VERSION = 3

    private data class BuiltinWordBook(val id: String, val name: String, val description: String, val asset: String)

    private val BUILTIN_WORD_BOOKS = listOf(
        BuiltinWordBook("wb_en2_s", "S · 必背", "考研英语二写作必背词（图表描述 / 书信框架 / 议论转折）", "/words/s_must.json"),
        BuiltinWordBook("wb_en2_a", "A · 话题备用", "按话题归堆：教育 / 环保 / 科技 / 就业 / 老龄化 / 健康 / 文化", "/words/a_topic.json"),
        BuiltinWordBook("wb_en2_b", "B · 仅备查", "低频行业词 / 同根次要形式 / 品格修养词等，真考到现查", "/words/b_reference.json"),
    )

    suspend fun populateIfEmpty(appDir: File, contentRepo: ContentRepository, wordRepo: WordRepository) {
        // 内容版本变化时，先清掉旧的内置书库/词库以便按新版重导（不影响其它自定义内容）
        val verFile = File(appDir, "content.version")
        val currentVersion = runCatching { verFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 0 }.getOrDefault(0)
        if (currentVersion < CONTENT_VERSION) {
            if (contentRepo.getBook(BUILTIN_BOOK_ID) != null) contentRepo.deleteBook(BUILTIN_BOOK_ID)
            BUILTIN_WORD_BOOKS.forEach { wb -> if (wordRepo.getWordBook(wb.id) != null) wordRepo.deleteWordBook(wb.id) }
            runCatching {
                appDir.mkdirs()
                verFile.writeText(CONTENT_VERSION.toString(), Charsets.UTF_8)
            }
        }

        if (contentRepo.getBook(BUILTIN_BOOK_ID) == null) {
            val json = readResource(BUILTIN_ARTICLES_ASSET)
            if (json != null) {
                val articles = contentRepo.parseArticleArray(json, BUILTIN_BOOK_ID)
                if (articles.isNotEmpty()) {
                    contentRepo.saveBookWithArticles(
                        Book(
                            id = BUILTIN_BOOK_ID,
                            name = "英语二写作真题范文 2010–2025",
                            description = "共 ${articles.size} 篇历年考研英语二应用文与图表作文范文，段句严密对照",
                            length = articles.size,
                            source = "builtin",
                            articles = articles
                        )
                    )
                }
            }
        }

        if (wordRepo.getWordBook(LEGACY_WORDBOOK_ID) != null) {
            wordRepo.deleteWordBook(LEGACY_WORDBOOK_ID)
        }

        BUILTIN_WORD_BOOKS.forEach { wb ->
            if (wordRepo.getWordBook(wb.id) == null) {
                wordRepo.saveBook(
                    WordBookEntity(id = wb.id, name = wb.name, description = wb.description, source = "builtin")
                )
                // 打乱一次并固化顺序（避免按字母序），与移动端一致
                val words = WordJson.parseArray(readResource(wb.asset), wb.id)
                    .shuffled()
                    .mapIndexed { i, e -> e.copy(idx = i) }
                if (words.isNotEmpty()) wordRepo.saveWords(wb.id, words)
            }
        }
    }

    private fun readResource(path: String): String? {
        return try {
            object {}.javaClass.getResourceAsStream(path)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
}
