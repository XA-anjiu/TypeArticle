package com.example.typingarticle.core.data.repository

import com.example.typingarticle.core.data.ArticleDto
import com.example.typingarticle.core.data.ArticleJson
import com.example.typingarticle.core.data.local.dao.ArticleDao
import com.example.typingarticle.core.data.local.dao.BookDao
import com.example.typingarticle.core.data.local.entity.ArticleEntity
import com.example.typingarticle.core.data.local.entity.BookEntity
import com.example.typingarticle.core.model.Article
import com.example.typingarticle.core.model.Book
import com.example.typingarticle.core.util.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 文章与书籍内容仓库（跨平台）
 */
class ContentRepository(
    private val bookDao: BookDao,
    private val articleDao: ArticleDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

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

    suspend fun getArticleCount(bookId: String): Int = articleDao.getArticleCountByBook(bookId)

    suspend fun deleteBook(bookId: String) {
        articleDao.deleteArticlesByBook(bookId)
        bookDao.deleteBookById(bookId)
    }

    suspend fun importPackage(rawJson: String, bookName: String = "导入文章包"): Book {
        val bookId = "import_" + newId().take(8)
        val articleEntities = mutableListOf<ArticleEntity>()

        try {
            val trimmed = rawJson.trim().removePrefix("\uFEFF").trim()
            if (trimmed.startsWith("[")) {
                val dtos = json.decodeFromString<List<ArticleDto>>(trimmed)
                dtos.forEachIndexed { i, dto ->
                    if (dto.title.isNotEmpty() || dto.text.isNotEmpty()) {
                        articleEntities.add(toArticleEntity(ArticleJson.toArticle(dto, bookId, i)))
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val pkg = json.decodeFromString<BookPackageDto>(trimmed)
                pkg.articles.forEachIndexed { i, dto ->
                    if (dto.title.isNotEmpty() || dto.text.isNotEmpty()) {
                        articleEntities.add(toArticleEntity(ArticleJson.toArticle(dto, bookId, i)))
                    }
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

    fun parseArticleArray(jsonText: String, bookId: String): List<Article> =
        ArticleJson.parseArray(jsonText, bookId)

    suspend fun saveBookWithArticles(book: Book) {
        val bookEntity = BookEntity(
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

    private fun toArticleEntity(article: Article): ArticleEntity = ArticleEntity(
        id = article.id,
        bookId = article.bookId,
        idx = article.idx,
        title = article.title,
        titleTranslate = article.titleTranslate,
        text = article.text,
        textTranslate = article.textTranslate,
        audioUri = article.audioSrc,
        lrcJson = serializeLrc(article.lrcPosition),
        nameListJson = serializeNameList(article.nameList)
    )

    private fun toArticleModel(entity: ArticleEntity): Article = Article(
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

    private fun serializeLrc(lrc: List<Pair<Double, Double>>): String? {
        if (lrc.isEmpty()) return null
        return json.encodeToString(lrc.map { listOf(it.first, it.second) })
    }

    private fun deserializeLrc(jsonText: String?): List<Pair<Double, Double>> {
        if (jsonText.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<List<Double>>>(jsonText)
                .mapNotNull { if (it.size >= 2) it[0] to it[1] else null }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeNameList(names: List<String>): String? {
        if (names.isEmpty()) return null
        return json.encodeToString(names)
    }

    private fun deserializeNameList(jsonText: String?): List<String> {
        if (jsonText.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(jsonText)
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Serializable
private data class BookPackageDto(
    val name: String? = null,
    val articles: List<ArticleDto> = emptyList()
)
