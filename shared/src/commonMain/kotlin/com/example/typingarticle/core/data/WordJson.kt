package com.example.typingarticle.core.data

import com.example.typingarticle.core.data.local.entity.WordEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 词表 JSON 的跨平台解析：[{word, phonetic0?, phonetic1?, trans?}]
 * trans / phonetic* 可能是字符串，也可能是字符串数组（多义项）。
 */
object WordJson {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun parseArray(raw: String?, bookId: String): List<WordEntity> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val root = json.parseToJsonElement(raw.trim().removePrefix("\uFEFF").trim()) as? JsonArray
                ?: return emptyList()
            root.mapIndexedNotNull { idx, el ->
                val o = el as? JsonObject ?: return@mapIndexedNotNull null
                val w = text(o["word"])?.trim().orEmpty()
                if (w.isEmpty()) null
                else WordEntity(
                    bookId = bookId,
                    idx = idx,
                    word = w,
                    phonetic0 = text(o["phonetic0"]),
                    phonetic1 = text(o["phonetic1"]),
                    trans = text(o["trans"]),
                    sentencesJson = o["sentences"]?.toString(),
                    phrasesJson = o["phrases"]?.toString()
                )
            }
                .distinctBy { it.word.lowercase() }
                .mapIndexed { i, e -> e.copy(idx = i) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun text(el: JsonElement?): String? = when (el) {
        null -> null
        is JsonPrimitive -> el.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        is JsonArray -> el.mapNotNull { item -> (item as? JsonPrimitive)?.contentOrNull?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("；")
            .takeIf { it.isNotEmpty() }
        else -> null
    }
}
