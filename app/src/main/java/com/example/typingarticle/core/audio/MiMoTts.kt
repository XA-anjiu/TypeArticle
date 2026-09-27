package com.example.typingarticle.core.audio

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 小米 MiMo-V2.5-TTS 在线语音合成客户端（OpenAI 兼容 Chat Completions 接口）。
 *
 * - 非流式请求，返回 wav（base64）；结果按「音色 + 文本」缓存到本地，避免重复计费。
 * - 仅用标准库（HttpURLConnection + org.json），不引入额外依赖。
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
            val payload = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "user").put("content", STYLE_INSTRUCTION))
                    put(JSONObject().put("role", "assistant").put("content", content))
                })
                put("audio", JSONObject().put("format", "wav").put("voice", voice))
                put("stream", false)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode !in 200..299) return@withContext null

            val respText = conn.inputStream.bufferedReader().use { it.readText() }
            val b64 = JSONObject(respText)
                .optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optJSONObject("audio")
                ?.optString("data")
            if (b64.isNullOrEmpty()) return@withContext null

            val bytes = Base64.decode(b64, Base64.DEFAULT)
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
    }
}
