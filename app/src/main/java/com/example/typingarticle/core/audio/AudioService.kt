package com.example.typingarticle.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.BuildConfig
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * 音频服务接口定义
 */
interface AudioService {
    fun playSentence(sentenceText: String, audioSrc: String? = null, lrcStart: Double? = null, lrcEnd: Double? = null)
    fun playWord(word: String)
    fun keyClick()
    fun errorBeep()
    fun stop()
    fun setVolume(volume: Float)
    fun setSpeechRate(rate: Float)

    /** 设置键盘音效（名称见 [KeyboardSounds]） */
    fun setKeyboardSound(name: String)

    /** 切换并试听键盘音效（设置页预览用） */
    fun previewKeyboardSound(name: String)
    fun release()
}

/** 键盘音效可选项（与 TypeWords 一致） */
object KeyboardSounds {
    const val OFF = "关闭"
    const val MECH = "机械键盘"
    const val MECH1 = "机械键盘1"
    const val MECH2 = "机械键盘2"
    const val OLD = "老式机械键盘"
    const val LAPTOP = "笔记本键盘"
    val ALL = listOf(MECH, MECH1, MECH2, OLD, LAPTOP, OFF)
    const val DEFAULT = MECH1
}

/**
 * 音频服务实现
 * - 句子：有音频+t时间轴走 ExoPlayer 区间，否则系统 TTS
 * - 单词：有道词典发音，失败降级系统 TTS
 * - 键音/错误音：SoundPool 预载，来自 res/raw（移植自 TypeWords），失败降级 ToneGenerator
 * - 全程静默容错，离线可用
 */
class AndroidAudioService(
    private val context: Context
) : AudioService {

    private var exoPlayer: ExoPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var soundPool: SoundPool? = null
    private var toneGenerator: ToneGenerator? = null

    private var keySoundIds: IntArray = IntArray(0)
    private val allKeySoundIds = HashMap<String, IntArray>()
    private var keyRotation: Int = 0
    private var errorSoundId: Int = 0
    private var keyboardSoundName: String = KeyboardSounds.DEFAULT

    private var currentVolume: Float = 1.0f
    private var currentSpeechRate: Float = 1.0f
    private var isTtsReady: Boolean = false

    // 小米 MiMo 在线 TTS（有 key 才启用）
    private val mimoTts: MiMoTtsClient? = if (BuildConfig.MIMO_API_KEY.isNotBlank()) {
        MiMoTtsClient(BuildConfig.MIMO_API_KEY, context.cacheDir)
    } else null
    private val ttsVoice: String = BuildConfig.MIMO_TTS_VOICE.ifBlank { "Mia" }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ttsJob: Job? = null

    init {
        initTts()
        initSoundPool()
        initExoPlayer()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                    textToSpeech?.setSpeechRate(currentSpeechRate)
                    isTtsReady = true
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    private fun initSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(16)
                .setAudioAttributes(audioAttributes)
                .build()

            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

            errorSoundId = soundPool?.load(context, R.raw.beep, 1) ?: 0

            // 一次性预载全部键盘音效，切换时无需重新 load，点击即可发声（支持试听）
            KeyboardSounds.ALL.forEach { name ->
                val resIds = resourceIdsFor(name)
                if (resIds.isEmpty()) {
                    allKeySoundIds[name] = IntArray(0)
                } else {
                    val repeats = if (resIds.size == 1) 4 else 1
                    val ids = ArrayList<Int>(resIds.size * repeats)
                    repeat(repeats) { resIds.forEach { ids.add(soundPool?.load(context, it, 1) ?: 0) } }
                    allKeySoundIds[name] = ids.toIntArray()
                }
            }
            applyKeyboardSound()
        } catch (_: Exception) {
            // 静默降级
        }
    }

    private fun resourceIdsFor(name: String): List<Int> = when (name) {
        KeyboardSounds.MECH -> listOf(R.raw.key_m0, R.raw.key_m1, R.raw.key_m2, R.raw.key_m3)
        KeyboardSounds.MECH1 -> listOf(R.raw.key_jixie1)
        KeyboardSounds.MECH2 -> listOf(R.raw.key_jixie2)
        KeyboardSounds.OLD -> listOf(R.raw.key_old)
        KeyboardSounds.LAPTOP -> listOf(R.raw.key_laptop)
        KeyboardSounds.OFF -> emptyList()
        else -> listOf(R.raw.key_jixie1)
    }

    private fun applyKeyboardSound() {
        keySoundIds = allKeySoundIds[keyboardSoundName] ?: IntArray(0)
        keyRotation = 0
    }

    override fun setKeyboardSound(name: String) {
        keyboardSoundName = name
        applyKeyboardSound()
    }

    override fun previewKeyboardSound(name: String) {
        setKeyboardSound(name)
        keyClick()
    }

    private fun initExoPlayer() {
        try {
            exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
                volume = currentVolume
            }
        } catch (_: Exception) {
        }
    }

    override fun playSentence(sentenceText: String, audioSrc: String?, lrcStart: Double?, lrcEnd: Double?) {
        val mimo = mimoTts
        if (mimo != null && mimo.isConfigured() && sentenceText.isNotBlank()) {
            ttsJob?.cancel()
            ttsJob = scope.launch {
                val file = mimo.synthesize(sentenceText, ttsVoice)
                if (file != null) playFile(file) else playSentenceLocal(sentenceText, audioSrc, lrcStart, lrcEnd)
            }
            return
        }
        playSentenceLocal(sentenceText, audioSrc, lrcStart, lrcEnd)
    }

    private fun playSentenceLocal(sentenceText: String, audioSrc: String?, lrcStart: Double?, lrcEnd: Double?) {
        if (!audioSrc.isNullOrEmpty() && lrcStart != null && lrcEnd != null && lrcEnd > lrcStart) {
            try {
                val player = exoPlayer ?: return
                player.stop()
                val startMs = (lrcStart * 1000).toLong()
                val endMs = (lrcEnd * 1000).toLong()
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(audioSrc))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(startMs)
                            .setEndPositionMs(endMs)
                            .build()
                    )
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                return
            } catch (_: Exception) {
            }
        }
        speakTts(sentenceText)
    }

    override fun playWord(word: String) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        val mimo = mimoTts
        if (mimo != null && mimo.isConfigured()) {
            ttsJob?.cancel()
            ttsJob = scope.launch {
                val file = mimo.synthesize(trimmed, ttsVoice)
                if (file != null) playFile(file) else playWordOnline(trimmed)
            }
            return
        }
        playWordOnline(trimmed)
    }

    private fun playWordOnline(trimmed: String) {
        val youdaoUrl = "https://dict.youdao.com/dictvoice?audio=${Uri.encode(trimmed)}&type=2"
        try {
            val player = exoPlayer
            if (player != null) {
                player.stop()
                player.setMediaItem(MediaItem.fromUri(youdaoUrl))
                player.prepare()
                player.play()
                return
            }
        } catch (_: Exception) {
        }
        speakTts(trimmed)
    }

    private fun playFile(file: File) {
        try {
            val player = exoPlayer ?: return
            player.stop()
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.prepare()
            player.play()
        } catch (_: Exception) {
        }
    }

    private fun speakTts(text: String) {
        try {
            if (isTtsReady && textToSpeech != null) {
                textToSpeech?.setSpeechRate(currentSpeechRate)
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sentence_${System.currentTimeMillis()}")
            }
        } catch (_: Exception) {
        }
    }

    override fun keyClick() {
        if (keyboardSoundName == KeyboardSounds.OFF) return
        try {
            if (keySoundIds.isNotEmpty()) {
                keyRotation = (keyRotation + 1) % keySoundIds.size
                soundPool?.play(keySoundIds[keyRotation], currentVolume * 0.7f, currentVolume * 0.7f, 1, 0, 1.0f)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 18)
            }
        } catch (_: Exception) {
        }
    }

    override fun errorBeep() {
        try {
            if (errorSoundId > 0) {
                soundPool?.play(errorSoundId, currentVolume, currentVolume, 2, 0, 1.0f)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 70)
            }
        } catch (_: Exception) {
        }
    }

    override fun stop() {
        try {
            ttsJob?.cancel()
            exoPlayer?.stop()
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0.0f, 1.0f)
        try {
            exoPlayer?.volume = currentVolume
        } catch (_: Exception) {
        }
    }

    override fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate.coerceIn(0.5f, 2.0f)
        try {
            textToSpeech?.setSpeechRate(currentSpeechRate)
        } catch (_: Exception) {
        }
    }

    override fun release() {
        try {
            ttsJob?.cancel()
            scope.cancel()
            exoPlayer?.release()
            exoPlayer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            soundPool?.release()
            soundPool = null
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {
        }
    }
}
