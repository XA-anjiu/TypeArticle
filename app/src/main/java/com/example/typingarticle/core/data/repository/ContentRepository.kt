package com.example.typingarticle.core.data.repository

import com.example.typingarticle.core.data.local.dao.ArticleDao
import com.example.typingarticle.core.data.local.dao.BookDao
import com.example.typingarticle.core.data.local.entity.ArticleEntity
import com.example.typingarticle.core.data.local.entity.BookEntity
import com.example.typingarticle.core.model.Article
import com.example.typingarticle.core.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 文章与书籍内容仓库
 *
 * 遵循 Section 6 数据契约：
 * - 顶层导入支持 Article[] JSON 数组
 * - 原文只存 text 与 textTranslate，结构由 ArticleParser 运行时生成
 */
class ContentRepository(
    private val bookDao: BookDao,
    private val articleDao: ArticleDao
) {
    fun observeBooks(): Flow<List<Book>> {
        return bookDao.observeBooks().map { entities ->
            entities.map { entity ->
                Book(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    lang = entity.lang,
                    version = entity.version,
                    length = entity.length,
                    source = entity.source
                )
            }
        }
    }

    suspend fun getBook(id: String): Book? {
        val entity = bookDao.getBookById(id) ?: return null
        val articleEntities = articleDao.getArticlesByBook(id)
        val articles = articleEntities.map { toArticleModel(it) }
        return Book(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            lang = entity.lang,
            version = entity.version,
            length = articles.size,
            source = entity.source,
            articles = articles
        )
    }

    suspend fun getArticle(bookId: String, idx: Int): Article? {
        val entity = articleDao.getArticleByBookAndIndex(bookId, idx) ?: return null
        return toArticleModel(entity)
    }

    suspend fun getArticleCount(bookId: String): Int {
        return articleDao.getArticleCountByBook(bookId)
    }

    /** 删除书库：连同其下所有文章一并删除 */
    suspend fun deleteBook(bookId: String) {
        articleDao.deleteArticlesByBook(bookId)
        bookDao.deleteBookById(bookId)
    }

    /**
     * 导入用户提供的 JSON 数组或书籍包
     *
     * 遵循 Section 6.4：顶层为 Article[]，按数组顺序全部导入，不跳过任何项
     */
    suspend fun importPackage(json: String, bookName: String = "导入文章包"): Book {
        val bookId = "import_" + UUID.randomUUID().toString().take(8)
        val articleEntities = mutableListOf<ArticleEntity>()

        try {
            val trimmed = json.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    articleEntities.add(parseArticleObject(obj, bookId, i))
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                val bookTitle = obj.optString("name", bookName)
                val articlesArray = obj.getJSONArray("articles")
                for (i in 0 until articlesArray.length()) {
                    val artObj = articlesArray.getJSONObject(i)
                    articleEntities.add(parseArticleObject(artObj, bookId, i))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val bookEntity = BookEntity(
            id = bookId,
            name = bookName,
            description = "导入于 " + System.currentTimeMillis(),
            length = articleEntities.size,
            source = "import"
        )

        bookDao.insertBook(bookEntity)
        articleDao.insertArticles(articleEntities)

        return Book(
            id = bookId,
            name = bookEntity.name,
            description = bookEntity.description,
            length = articleEntities.size,
            source = "import",
            articles = articleEntities.map { toArticleModel(it) }
        )
    }

    /**
     * 解析 Article[] JSON 数组为领域模型列表（用于内置内容包加载）。
     * 跳过空对象占位；任何有内容的文章都不遗漏。
     */
    fun parseArticleArray(json: String, bookId: String): List<Article> {
        val result = mutableListOf<Article>()
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return result
        return try {
            val array = JSONArray(trimmed)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                if (obj.length() == 0) continue
                val title = obj.optString("title")
                val text = obj.optString("text")
                if (title.isEmpty() && text.isEmpty()) continue
                result.add(toArticleModel(parseArticleObject(obj, bookId, result.size)))
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            result
        }
    }

    suspend fun saveBookWithArticles(book: Book) {        val bookEntity = BookEntity(
            id = book.id,
            name = book.name,
            description = book.description,
            lang = book.lang,
            version = book.version,
            length = book.articles.size,
            source = book.source
        )
        val articleEntities = book.articles.mapIndexed { index, art ->
            ArticleEntity(
                id = art.id.ifEmpty { "${book.id}_$index" },
                bookId = book.id,
                idx = index,
                title = art.title,
                titleTranslate = art.titleTranslate,
                text = art.text,
                textTranslate = art.textTranslate,
                audioUri = art.audioSrc,
                lrcJson = serializeLrc(art.lrcPosition),
                nameListJson = serializeNameList(art.nameList)
            )
        }
        bookDao.insertBook(bookEntity)
        articleDao.insertArticles(articleEntities)
    }

    private fun parseArticleObject(obj: JSONObject, bookId: String, idx: Int): ArticleEntity {
        val id = obj.optString("id", "${bookId}_$idx")
        val title = obj.optString("title", "Article ${idx + 1}")
        val titleTranslate = obj.optString("titleTranslate").takeIf { it.isNotEmpty() }
        val text = obj.optString("text", "")
        val textTranslate = obj.optString("textTranslate").takeIf { it.isNotEmpty() }
        val audioSrc = obj.optString("audioSrc").takeIf { it.isNotEmpty() }
        val lrcJson = obj.optJSONArray("lrcPosition")?.toString()
        val nameListJson = obj.optJSONArray("nameList")?.toString()

        return ArticleEntity(
            id = id,
            bookId = bookId,
            idx = idx,
            title = title,
            titleTranslate = titleTranslate,
            text = text,
            textTranslate = textTranslate,
            audioUri = audioSrc,
            lrcJson = lrcJson,
            nameListJson = nameListJson
        )
    }

    private fun toArticleModel(entity: ArticleEntity): Article {
        return Article(
            id = entity.id,
            bookId = entity.bookId,
            idx = entity.idx,
            title = entity.title,
            titleTranslate = entity.titleTranslate,
            text = entity.text,
            textTranslate = entity.textTranslate,
            audioSrc = entity.audioUri,
            lrcPosition = deserializeLrc(entity.lrcJson),
            nameList = deserializeNameList(entity.nameListJson)
        )
    }

    private fun serializeLrc(lrc: List<Pair<Double, Double>>): String? {
        if (lrc.isEmpty()) return null
        val array = JSONArray()
        lrc.forEach { (start, end) ->
            val pair = JSONArray()
            pair.put(start)
            pair.put(end)
            array.put(pair)
        }
        return array.toString()
    }

    private fun deserializeLrc(json: String?): List<Pair<Double, Double>> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val list = mutableListOf<Pair<Double, Double>>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val pair = array.getJSONArray(i)
                list.add(pair.getDouble(0) to pair.getDouble(1))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeNameList(names: List<String>): String? {
        if (names.isEmpty()) return null
        return JSONArray(names).toString()
    }

    private fun deserializeNameList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val list = mutableListOf<String>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
