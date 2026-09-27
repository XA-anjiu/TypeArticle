package com.example.typingarticle.core.engine

/**
 * 实体键盘解析后的核心命令
 */
sealed class KeyCommand {
    data class InputChar(val char: Char) : KeyCommand()
    data object Space : KeyCommand()
    data object Backspace : KeyCommand()
    data object Confirm : KeyCommand()
    data object NextSentence : KeyCommand()
    data object PrevSentence : KeyCommand()
    data object ReplaySentence : KeyCommand()
    data object ToggleDictation : KeyCommand()
    data object ToggleTranslate : KeyCommand()
    data object NextArticle : KeyCommand()
    data object Ignored : KeyCommand()
}
