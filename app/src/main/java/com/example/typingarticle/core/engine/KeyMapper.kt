package com.example.typingarticle.core.engine

/**
 * 实体键盘输入映射器（纯逻辑，禁止依赖 android.*）
 *
 * 遵循 Section 9 规范：
 * - 仅做实体键盘：接收 (key: String, code: String)
 * - 字母/数字/标点 -> InputChar
 * - 空格 -> Space
 * - Backspace -> Backspace
 * - -> 或 N (无修饰键或配合快捷模式) -> NextSentence
 * - <- -> PrevSentence
 * - R -> ReplaySentence
 * - D -> ToggleDictation
 * - T -> ToggleTranslate
 * - Ctrl + -> -> NextArticle
 */
class KeyMapper(
    private val keyBindings: Map<String, String> = emptyMap()
) {
    fun map(key: String, code: String, isCtrl: Boolean = false, isAlt: Boolean = false): KeyCommand {
        // 0. Alt + 空格：上一个（上一词 / 上一句）
        if (isAlt && (code.equals("Space", ignoreCase = true) || key == " ")) {
            return KeyCommand.PrevSentence
        }

        // 1. Ctrl 组合快捷键
        if (isCtrl) {
            when {
                code.equals("ArrowRight", ignoreCase = true) || key.equals("ArrowRight", ignoreCase = true) ->
                    return KeyCommand.NextArticle
                key.equals("n", ignoreCase = true) ->
                    return KeyCommand.NextSentence
                key.equals("p", ignoreCase = true) ->
                    return KeyCommand.PrevSentence
                key.equals("r", ignoreCase = true) ->
                    return KeyCommand.ReplaySentence
                key.equals("d", ignoreCase = true) ->
                    return KeyCommand.ToggleDictation
                key.equals("t", ignoreCase = true) ->
                    return KeyCommand.ToggleTranslate
            }
        }

        // 2. 自定义键位配置映射
        val customAction = keyBindings[code] ?: keyBindings[key]
        if (customAction != null) {
            return when (customAction) {
                "NextSentence" -> KeyCommand.NextSentence
                "PrevSentence" -> KeyCommand.PrevSentence
                "ReplaySentence" -> KeyCommand.ReplaySentence
                "ToggleDictation" -> KeyCommand.ToggleDictation
                "ToggleTranslate" -> KeyCommand.ToggleTranslate
                "NextArticle" -> KeyCommand.NextArticle
                "Space" -> KeyCommand.Space
                "Backspace" -> KeyCommand.Backspace
                else -> KeyCommand.Ignored
            }
        }

        // 3. 基础功能键识别
        if (code.equals("Space", ignoreCase = true) || key == " ") {
            return KeyCommand.Space
        }
        if (code.equals("Backspace", ignoreCase = true) || key.equals("Backspace", ignoreCase = true)) {
            return KeyCommand.Backspace
        }
        if (code.equals("ArrowRight", ignoreCase = true) || key.equals("ArrowRight", ignoreCase = true)) {
            return KeyCommand.NextSentence
        }
        if (code.equals("ArrowLeft", ignoreCase = true) || key.equals("ArrowLeft", ignoreCase = true)) {
            return KeyCommand.PrevSentence
        }
        if (code.equals("Enter", ignoreCase = true) || code.equals("NumpadEnter", ignoreCase = true) ||
            key.equals("Enter", ignoreCase = true) || key == "\n" || key == "\r"
        ) {
            return KeyCommand.Confirm
        }

        // 4. 普通打字字符输入（包含字母、数字、英文标点）
        if (key.length == 1) {
            return KeyCommand.InputChar(key[0])
        }

        return KeyCommand.Ignored
    }
}
