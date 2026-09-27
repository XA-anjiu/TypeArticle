package com.example.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Base64
import java.util.Properties

/**
 * 小米 MiMo-V2.5-TTS 在线语音合成客户端（OpenAI 兼容 Chat Completions 接口）。
 *
 * - 非流式请求，返回 wav（base64）；结果按「音色 + 文本」缓存到本地，避免重复计费。
 * - 网络失败返回 null，由调用方回退系统 TTS。
 */
class MiMoTtsClient(
    private val apiKey: String,
    private val cacheDir: File,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val model: String = DEFAULT_MODEL,
) {
    fun isConfigured(): Boolean = apiKey.isNotBlank()

    suspend fun synthesize(text: String, voice: String): File? = withContext(Dispatchers.IO) {
        val content = text.trim()
        if (content.isEmpty()) return@withContext null

        val dir = File(cacheDir, "mimo_tts").apply { if (!exists()) mkdirs() }
        val outFile = File(dir, sha1("$voice|$content") + ".wav")
        if (outFile.exists() && outFile.length() > 44) return@withContext outFile

        var conn: HttpURLConnection? = null
        try {
            conn = (URL("$baseUrl/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 40_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("api-key", apiKey)
            }
            val payload = buildJsonObject {
                put("model", model)
                putJsonArray("messages") {
                    addJsonObject { put("role", "user"); put("content", STYLE_INSTRUCTION) }
                    addJsonObject { put("role", "assistant"); put("content", content) }
                }
                putJsonObject("audio") { put("format", "wav"); put("voice", voice) }
                put("stream", false)
            }.toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode !in 200..299) return@withContext null

            val respText = conn.inputStream.bufferedReader().use { it.readText() }
            val b64 = runCatching {
                Json.parseToJsonElement(respText).jsonObject["choices"]?.jsonArray
                    ?.getOrNull(0)?.jsonObject
                    ?.get("message")?.jsonObject
                    ?.get("audio")?.jsonObject
                    ?.get("data")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            if (b64.isNullOrEmpty()) return@withContext null

            val bytes = Base64.getDecoder().decode(b64)
            if (bytes.size <= 44) return@withContext null

            outFile.writeBytes(bytes)
            outFile
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun sha1(s: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(s.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.xiaomimimo.com/v1"
        const val DEFAULT_MODEL = "mimo-v2.5-tts"
        const val STYLE_INSTRUCTION =
            "Read clearly and naturally with standard American pronunciation, moderate pace."

        /**
         * 从多个来源装配客户端配置，优先级：系统属性 > 环境变量 > ~/.typearticle/mimo.properties
         * 未配置 Key 时返回的对象 isConfigured() 为 false（保持纯离线）。
         */
        fun fromConfig(appDir: File): MiMoTtsClient {
            val sysKey = System.getProperty("mimo.apiKey").orEmpty()
            val sysVoice = System.getProperty("mimo.voice").orEmpty()
            val envKey = System.getenv("MIMO_API_KEY").orEmpty()
            val envVoice = System.getenv("MIMO_TTS_VOICE").orEmpty()

            var fileKey = ""
            var fileVoice = ""
            val f = File(appDir, "mimo.properties")
            if (f.exists()) {
                runCatching {
                    val p = Properties()
                    f.inputStream().use { p.load(it) }
                    fileKey = p.getProperty("MIMO_API_KEY").orEmpty()
                    fileVoice = p.getProperty("MIMO_TTS_VOICE").orEmpty()
                }
            }

            val key = listOf(sysKey, envKey, fileKey).firstOrNull { it.isNotBlank() }.orEmpty()
            val voice = listOf(sysVoice, envVoice, fileVoice).firstOrNull { it.isNotBlank() } ?: "Mia"
            return MiMoTtsClient(key, appDir).also { it.voice = voice }
        }
    }

    /** 当前音色（可在运行时切换） */
    var voice: String = "Mia"
}
