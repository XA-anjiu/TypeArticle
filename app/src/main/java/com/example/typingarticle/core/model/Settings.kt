package com.example.typingarticle.core.model

/**
 * 单词默写模式
 */
enum class WordMode {
    DICTATION, // 默写（看释义/音标写英文）
    LISTENING  // 听写（只放发音，不显示释义）
}

/**
 * 全局设置项（影响内核）
 */
data class Settings(
    val dictation: Boolean = false,
    val translate: Boolean = true,
    val ignoreCase: Boolean = true,
    val ignoreSymbol: Boolean = false,
    val ignoreSimpleWord: Boolean = false,
    val nameList: Set<String> = setOf("Mr", "Mrs", "Ms", "Dr", "Miss", "Mr.", "Mrs.", "Ms.", "Dr."),
    val keySoundEnabled: Boolean = true,
    val errorSoundEnabled: Boolean = true,
    val keyboardSound: String = "机械键盘1",
    val volume: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val fontSize: Int = 18,
    val theme: String = "system",
    val wordMode: WordMode = WordMode.DICTATION,
    val wordIgnoreCase: Boolean = true,
    val showMeaning: Boolean = true,
    val showPhonetic: Boolean = true
)
