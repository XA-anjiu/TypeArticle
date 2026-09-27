package com.example.typingarticle.core.data

import com.example.typingarticle.core.model.Article
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 文章包 JSON 的跨平台解析（替代 Android 专有的 org.json）
 */
@Serializable
data class ArticleDto(
    val id: String? = null,
    val title: String = "",
    val titleTranslate: String? = null,
    val text: String = "",
    val textTranslate: String? = null,
    val audioSrc: String? = null,
    val lrcPosition: List<List<Double>> = emptyList(),
    val nameList: List<String> = emptyList()
)

object ArticleJson {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun parseArray(raw: String, bookId: String): List<Article> {
        val trimmed = raw.trim().removePrefix("\uFEFF").trim()
        if (!trimmed.startsWith("[")) return emptyList()
        return try {
            val dtos = json.decodeFromString<List<ArticleDto>>(trimmed)
            dtos.mapIndexedNotNull { index, dto ->
                if (dto.title.isEmpty() && dto.text.isEmpty()) return@mapIndexedNotNull null
                toArticle(dto, bookId, index)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun toArticle(dto: ArticleDto, bookId: String, index: Int): Article = Article(
        id = dto.id ?: "${bookId}_$index",
        bookId = bookId,
        idx = index,
        title = dto.title.ifEmpty { "Article ${index + 1}" },
        titleTranslate = dto.titleTranslate,
        text = dto.text,
        textTranslate = dto.textTranslate,
        audioSrc = dto.audioSrc,
        lrcPosition = dto.lrcPosition.mapNotNull { pair ->
            if (pair.size >= 2) pair[0] to pair[1] else null
        },
        nameList = dto.nameList
    )
}
