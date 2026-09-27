package com.example.desktop

import com.example.typingarticle.core.audio.AudioService
import com.example.typingarticle.core.audio.KeyboardSounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 桌面端音频服务：
 * - 句子 / 单词朗读：优先小米 MiMo 在线 TTS（合成 wav 并按文本缓存），失败或未配置时回退 Windows 系统语音
 * - 键音 / 错误音：javax.sound 实时合成短音
 */
class DesktopAudioService(appDir: File) : AudioService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var volume: Float = 1.0f
    private var speechRate: Float = 1.0f
    private var keyboardSound: String = KeyboardSounds.DEFAULT
    private var speakJob: Job? = null
    private var line: SourceDataLine? = null

    private val mimo: MiMoTtsClient = MiMoTtsClient.fromConfig(appDir)

    override fun playSentence(sentenceText: String, audioSrc: String?, lrcStart: Double?, lrcEnd: Double?) {
        speak(sentenceText)
    }

    override fun playWord(word: String) {
        speak(word.trim())
    }

    override fun keyClick() {
        if (keyboardSound == KeyboardSounds.OFF) return
        val freq = when (keyboardSound) {
            KeyboardSounds.OLD -> 1600.0
            KeyboardSounds.LAPTOP -> 3200.0
            else -> 2600.0
        }
        tone(freq, 10, 0.18f)
    }

    override fun errorBeep() = tone(220.0, 170, 0.45f)

    override fun stop() {
        speakJob?.cancel()
        runCatching { line?.stop() }
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }

    override fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2f)
    }

    override fun setKeyboardSound(name: String) {
        keyboardSound = name
    }

    override fun previewKeyboardSound(name: String) {
        keyboardSound = name
        keyClick()
    }

    override fun release() {
        runCatching { line?.close() }
        scope.cancel()
    }

    /** 合成一个带指数衰减包络的正弦短音 */
    private fun tone(freq: Double, ms: Int, gain: Float) {
        if (volume <= 0f) return
        scope.launch {
            try {
                val sampleRate = 44100f
                val total = (sampleRate * ms / 1000.0).toInt().coerceAtLeast(1)
                val data = ByteArray(total * 2)
                for (i in 0 until total) {
                    val t = i / sampleRate
                    val env = exp(-6.0 * i / total)
                    val v = (sin(2 * PI * freq * t) * env * gain * volume * Short.MAX_VALUE).toInt()
                        .coerceIn(-32768, 32767).toShort()
                    data[i * 2] = (v.toInt() and 0xFF).toByte()
                    data[i * 2 + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
                }
                val fmt = AudioFormat(sampleRate, 16, 1, true, false)
                val info = DataLine.Info(SourceDataLine::class.java, fmt)
                val dl = AudioSystem.getLine(info) as SourceDataLine
                dl.open(fmt)
                dl.start()
                dl.write(data, 0, data.size)
                dl.drain()
                dl.close()
            } catch (_: Exception) {
            }
        }
    }

    /** 朗读：MiMo 优先，失败回退系统语音 */
    private fun speak(text: String) {
        if (text.isBlank()) return
        speakJob?.cancel()
        speakJob = scope.launch {
            if (mimo.isConfigured()) {
                val file = mimo.synthesize(text, mimo.voice)
                if (file != null) {
                    playWav(file)
                    return@launch
                }
            }
            speakSystem(text)
        }
    }

    /** 播放缓存的 wav 文件 */
    private fun playWav(file: File) {
        try {
            val ais = AudioSystem.getAudioInputStream(file)
            val fmt = ais.format
            val info = DataLine.Info(SourceDataLine::class.java, fmt)
            val dl = AudioSystem.getLine(info) as SourceDataLine
            dl.open(fmt)
            runCatching {
                val gain = dl.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                gain.value = if (volume <= 0.0001f) gain.minimum else (20.0 * kotlin.math.log10(volume.toDouble())).toFloat()
                    .coerceIn(gain.minimum, gain.maximum)
            }
            line = dl
            dl.start()
            val buf = ByteArray(8192)
            while (true) {
                val n = ais.read(buf, 0, buf.size)
                if (n <= 0) break
                dl.write(buf, 0, n)
            }
            dl.drain()
            dl.close()
            ais.close()
        } catch (_: Exception) {
        }
    }

    /** Windows 系统语音朗读（离线兜底） */
    private fun speakSystem(text: String) {
        try {
            val safe = text.replace("'", "''")
            val rate = ((speechRate - 1f) * 3f).toInt().coerceIn(-5, 5)
            val script = buildString {
                append("Add-Type -AssemblyName System.Speech;")
                append("\$s = New-Object System.Speech.Synthesis.SpeechSynthesizer;")
                append("\$s.Rate = $rate;")
                append("\$s.Volume = ${(volume.coerceIn(0f, 1f) * 100).toInt()};")
                append("\$s.Speak('$safe');")
            }
            val tmp = File.createTempFile("typearticle_tts_", ".ps1")
            tmp.writeText(script, Charsets.UTF_8)
            ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", tmp.absolutePath)
                .redirectErrorStream(true)
                .start()
                .waitFor()
            tmp.delete()
        } catch (_: Exception) {
        }
    }
}
