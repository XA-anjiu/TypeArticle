package com.example.typingarticle.core.audio

/**
 * 音频服务接口（跨平台）。
 */
interface AudioService {
    fun playSentence(sentenceText: String, audioSrc: String? = null, lrcStart: Double? = null, lrcEnd: Double? = null)
    fun playWord(word: String)
    fun keyClick()
    fun errorBeep()
    fun stop()
    fun setVolume(volume: Float)
    fun setSpeechRate(rate: Float)

    fun setKeyboardSound(name: String)

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
