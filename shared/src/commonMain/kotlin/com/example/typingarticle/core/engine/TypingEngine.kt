package com.example.typingarticle.core.engine

import com.example.typingarticle.core.model.Cursor
import com.example.typingarticle.core.model.EngineEvent
import com.example.typingarticle.core.model.EngineSnapshot
import com.example.typingarticle.core.model.PracticeStats
import com.example.typingarticle.core.model.Sentence
import com.example.typingarticle.core.model.Token
import com.example.typingarticle.core.model.TokenType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 核心打字状态机引擎（纯 Kotlin 实现，禁止依赖 android.*）
 *
 * 遵循 Section 8 规范：
 * - 游标结构：s(section) / t(sentence) / w(word) / k(charIndex)
 * - 标志位：isSpace / isEnd / input
 * - 自动跳过（8.4）：ignoreSymbol / nameList
 * - 回删跨边界（8.5）：跨词、跨句、跨段四层游标回退
 * - 状态推进与字符重放（Pitfall 2）：isSpace 态收到非空格字符，先推进并重放字符
 */
class TypingEngine(
    var ignoreCase: Boolean = true,
    var ignoreSymbol: Boolean = false,
    var ignoreSimpleWord: Boolean = false,
    var nameList: Set<String> = setOf("Mr", "Mrs", "Ms", "Dr", "Miss", "Mr.", "Mrs.", "Ms.", "Dr."),
    var simpleWords: Set<String> = DEFAULT_SIMPLE_WORDS,
    var knownWords: Set<String> = emptySet()
) {
    private var sections: List<List<Sentence>> = emptyList()

    private var s: Int = 0
    private var t: Int = 0
    private var w: Int = 0
    private var k: Int = 0

    private var isSpace: Boolean = false
    private var isEnd: Boolean = false
    private var input: String = ""
    private var pendingWrong: String = ""

    // 错误记录与去重：同词连续输错合并为 1 次
    private var currentWordHasError: Boolean = false
    private var totalWrongCount: Int = 0
    private var completedWordsCount: Int = 0
    private var spendMsTotal: Long = 0L
    private var startedTimestamp: Long = System.currentTimeMillis()

    private val _snapshot = MutableStateFlow(EngineSnapshot())
    val state: StateFlow<EngineSnapshot> = _snapshot.asStateFlow()

    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    /**
     * 加载文章段落或单词列表
     */
    fun loadSections(newSections: List<List<Sentence>>, initialCursor: Cursor? = null) {
        sections = newSections
        s = 0
        t = 0
        w = 0
        k = 0
        isSpace = false
        isEnd = false
        input = ""
        pendingWrong = ""
        currentWordHasError = false
        totalWrongCount = 0
        completedWordsCount = 0
        spendMsTotal = 0L
        startedTimestamp = System.currentTimeMillis()

        if (sections.isEmpty() || sections.all { sec -> sec.isEmpty() || sec.all { sen -> sen.tokens.isEmpty() } }) {
            isEnd = true
            emitSnapshot()
            return
        }

        // 跳过起始处的被忽略词
        skipForwardIgnored()

        if (initialCursor != null) {
            jump(initialCursor)
        } else {
            emitSnapshot()
        }
    }

    /**
     * 重置当前引擎状态
     */
    fun reset() {
        s = 0
        t = 0
        w = 0
        k = 0
        isSpace = false
        isEnd = false
        input = ""
        pendingWrong = ""
        currentWordHasError = false
        totalWrongCount = 0
        completedWordsCount = 0
        spendMsTotal = 0L
        startedTimestamp = System.currentTimeMillis()
        skipForwardIgnored()
        emitSnapshot()
    }

    /**
     * 8.3 转移表：处理字符输入
     */
    fun onKey(char: Char) {
        if (isEnd || sections.isEmpty()) return

        // 情形：处于等待空格态
        if (isSpace) {
            if (char == ' ') {
                advanceNextWord()
            } else {
                // Pitfall 2：isSpace 且收到其它字符，先 NextWord，再把该字符在新词上重放一次
                advanceNextWord()
                if (!isEnd) {
                    onKey(char)
                }
            }
            return
        }

        // 情形：非等待态（正在打当前词的某个字符）
        val token = currentToken() ?: return
        if (k >= token.text.length) {
            // 兜底防护：若已越界则按需进入空格态或下一词
            if (token.nextSpace) {
                isSpace = true
                input = ""
                _events.tryEmit(EngineEvent.SpaceWait)
                emitSnapshot()
            } else {
                advanceNextWord()
            }
            return
        }

        val targetChar = token.text[k]
        val isCorrect = if (ignoreCase) {
            char.equals(targetChar, ignoreCase = true)
        } else {
            char == targetChar
        }

        if (isCorrect) {
            input += char
            pendingWrong = ""
            k++
            _events.tryEmit(EngineEvent.CorrectChar(char))

            if (k >= token.text.length) {
                // 该词打完
                if (token.type == TokenType.Word && !isNameSkipped(token.text)) {
                    completedWordsCount++
                }

                if (token.nextSpace) {
                    isSpace = true
                    input = ""
                    _events.tryEmit(EngineEvent.SpaceWait)
                } else {
                    advanceNextWord()
                }
            }
            emitSnapshot()
        } else {
            // 字符错：记错（同词连续错合并为 1 次）+ 触发错误音，k 不动
            if (!currentWordHasError) {
                totalWrongCount++
                currentWordHasError = true
            }
            pendingWrong += char
            _events.tryEmit(EngineEvent.WrongChar(inputChar = char, expectedChar = targetChar))
            emitSnapshot()
        }
    }

    /**
     * 处理显式空格键
     */
    fun onSpace() {
        if (isEnd || sections.isEmpty()) return

        if (isSpace) {
            advanceNextWord()
            emitSnapshot()
        } else {
            // 若当前不是等待空格态，但用户按了空格，则视作输入字符 ' '
            onKey(' ')
        }
    }

    /**
     * 8.5 回删（跨边界，必须正确）
     */
    fun onBackspace() {
        if (isEnd || sections.isEmpty()) return

        // 1. 若处于等待空格态，回退到当前词的词尾字符
        if (isSpace) {
            val token = currentToken()
            if (token != null) {
                isSpace = false
                k = (token.text.length - 1).coerceAtLeast(0)
                input = token.text.take(k)
                pendingWrong = ""
                _events.tryEmit(EngineEvent.Backspace)
                emitSnapshot()
                return
            }
        }

        // 2. 优先清除当前字符位上打错的字符
        if (pendingWrong.isNotEmpty()) {
            pendingWrong = pendingWrong.dropLast(1)
            _events.tryEmit(EngineEvent.Backspace)
            emitSnapshot()
            return
        }

        // 3. 若在词内部（k > 0），则本词内单字符回删
        if (k > 0) {
            k--
            input = input.dropLast(1)
            _events.tryEmit(EngineEvent.Backspace)
            emitSnapshot()
            return
        }

        // 4. 当 k == 0 时，跨词/跨句/跨段逆序回退
        val prevLocation = findPreviousValidToken(s, t, w)
        if (prevLocation != null) {
            val (prevS, prevT, prevW) = prevLocation
            s = prevS
            t = prevT
            w = prevW
            currentWordHasError = false

            val prevToken = sections[s][t].tokens[w]
            if (prevToken.nextSpace) {
                // 上一个词结尾有空格：回退到该词的等待空格态
                isSpace = true
                k = prevToken.text.length
                input = ""
                pendingWrong = ""
            } else {
                // 上一个词结尾无空格：回退到该词最后一个字符
                isSpace = false
                k = (prevToken.text.length - 1).coerceAtLeast(0)
                input = prevToken.text.take(k)
                pendingWrong = ""
            }
            _events.tryEmit(EngineEvent.Backspace)
            emitSnapshot()
        }
    }

    /**
     * 推进到下一个有效词
     */
    private fun advanceNextWord() {
        isSpace = false
        input = ""
        pendingWrong = ""
        currentWordHasError = false
        k = 0
        w++

        val curSentence = currentSentence()
        if (curSentence == null || w >= curSentence.tokens.size) {
            advanceNextSentence()
            return
        }

        // 检查并跳过后续无需练习的词
        while (w < curSentence.tokens.size && shouldSkip(curSentence.tokens[w])) {
            w++
        }

        if (w >= curSentence.tokens.size) {
            advanceNextSentence()
        } else {
            _events.tryEmit(EngineEvent.NextWord(curSentence.tokens[w]))
            emitSnapshot()
        }
    }

    /**
     * 推进到下一句
     */
    fun advanceNextSentence() {
        isSpace = false
        input = ""
        pendingWrong = ""
        currentWordHasError = false
        k = 0
        w = 0
        t++

        if (s < sections.size && t >= sections[s].size) {
            s++
            t = 0
        }

        if (s >= sections.size) {
            isEnd = true
            val finalStats = buildStats()
            _events.tryEmit(EngineEvent.Complete(finalStats))
            emitSnapshot()
            return
        }

        // 新句起始词跳过判定
        val curSentence = currentSentence()
        if (curSentence != null) {
            while (w < curSentence.tokens.size && shouldSkip(curSentence.tokens[w])) {
                w++
            }
            if (w >= curSentence.tokens.size) {
                // 整句都被跳过时递归进入下一句
                advanceNextSentence()
                return
            }
        }

        _events.tryEmit(EngineEvent.NextSentence(sentenceIdx = t, sectionIdx = s))
        currentToken()?.let { _events.tryEmit(EngineEvent.NextWord(it)) }
        emitSnapshot()
    }

    /**
     * 回退到上一句
     */
    fun prevSentence() {
        if (sections.isEmpty()) return
        if (t > 0) {
            t--
        } else if (s > 0) {
            s--
            t = (sections[s].size - 1).coerceAtLeast(0)
        } else {
            return
        }

        w = 0
        k = 0
        input = ""
        pendingWrong = ""
        isSpace = false
        isEnd = false
        currentWordHasError = false

        val curSentence = currentSentence()
        if (curSentence != null) {
            while (w < curSentence.tokens.size && shouldSkip(curSentence.tokens[w])) {
                w++
            }
        }
        emitSnapshot()
    }

    /**
     * 精确跳转到指定游标
     */
    fun jump(cursor: Cursor) {
        if (sections.isEmpty()) return

        s = cursor.sectionIdx.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
        val curSection = sections.getOrNull(s) ?: return
        t = cursor.sentenceIdx.coerceIn(0, (curSection.size - 1).coerceAtLeast(0))
        val curSentence = curSection.getOrNull(t) ?: return
        w = cursor.wordIdx.coerceIn(0, (curSentence.tokens.size - 1).coerceAtLeast(0))
        val curToken = curSentence.tokens.getOrNull(w)
        val tokenLen = curToken?.text?.length ?: 0
        k = cursor.charIdx.coerceIn(0, tokenLen)

        isSpace = false
        isEnd = false
        currentWordHasError = false
        pendingWrong = ""
        input = curToken?.text?.take(k) ?: ""

        // 如果跳转落点在跳过词上，顺向滑移到有效词
        skipForwardIgnored()
        emitSnapshot()
    }

    /**
     * 周期性计时更新（毫秒）
     */
    fun tick(deltaMs: Long) {
        if (!isEnd) {
            spendMsTotal += deltaMs
            emitSnapshot()
        }
    }

    private fun skipForwardIgnored() {
        while (s < sections.size) {
            val sec = sections[s]
            while (t < sec.size) {
                val sen = sec[t]
                while (w < sen.tokens.size) {
                    if (!shouldSkip(sen.tokens[w])) {
                        return
                    }
                    w++
                }
                t++
                w = 0
            }
            s++
            t = 0
            w = 0
        }
        isEnd = true
    }

    /**
     * 向上逆向搜索上一个未被跳过的有效词
     */
    private fun findPreviousValidToken(fromS: Int, fromT: Int, fromW: Int): Triple<Int, Int, Int>? {
        var curS = fromS
        var curT = fromT
        var curW = fromW - 1

        while (true) {
            while (curW < 0) {
                curT--
                if (curT < 0) {
                    curS--
                    if (curS < 0) {
                        return null // 已经到达全篇最起始，无更早词元
                    }
                    curT = sections[curS].size - 1
                }
                curW = (sections[curS][curT].tokens.size - 1)
            }

            if (curW >= 0) {
                val token = sections[curS][curT].tokens[curW]
                if (!shouldSkip(token)) {
                    return Triple(curS, curT, curW)
                }
                curW--
            }
        }
    }

    private fun shouldSkip(token: Token): Boolean {
        if (ignoreSymbol && (token.type == TokenType.Number || token.type == TokenType.Symbol)) {
            return true
        }
        if (isNameSkipped(token.text)) {
            return true
        }
        if (token.type == TokenType.Word && isIgnoredWord(token.text)) {
            return true
        }
        return false
    }

    /** 已掌握词 / 简单词过滤（仅对 Word 生效） */
    private fun isIgnoredWord(text: String): Boolean {
        val clean = text.trimEnd('.', ',', '!', '?', ';', ':', '"', '\'').lowercase()
        if (clean.isEmpty()) return false
        if (knownWords.contains(clean)) return true
        if (ignoreSimpleWord && simpleWords.contains(clean)) return true
        return false
    }

    private fun isNameSkipped(text: String): Boolean {
        val clean = text.trimEnd('.', ',', '!', '?', ';', ':', '"', '\'')
        return nameList.any {
            it.equals(clean, ignoreCase = true) || it.equals(text, ignoreCase = true)
        }
    }

    fun currentToken(): Token? {
        return sections.getOrNull(s)?.getOrNull(t)?.tokens?.getOrNull(w)
    }

    fun currentSentence(): Sentence? {
        return sections.getOrNull(s)?.getOrNull(t)
    }

    private fun buildStats(): PracticeStats {
        val totalWords = calculateTotalWords()
        return PracticeStats(
            startedAt = startedTimestamp,
            spendMs = spendMsTotal,
            totalWords = totalWords,
            wrongCount = totalWrongCount,
            completedWords = completedWordsCount
        )
    }

    private fun calculateTotalWords(): Int {
        var count = 0
        for (sec in sections) {
            for (sen in sec) {
                for (token in sen.tokens) {
                    if (token.type == TokenType.Word && !shouldSkip(token)) {
                        count++
                    }
                }
            }
        }
        return count
    }

    private fun emitSnapshot() {
        val curToken = currentToken()
        val curSentence = currentSentence()
        _snapshot.value = EngineSnapshot(
            cursor = Cursor(sectionIdx = s, sentenceIdx = t, wordIdx = w, charIdx = k),
            isSpace = isSpace,
            isEnd = isEnd,
            input = input,
            rawInput = input + pendingWrong,
            currentToken = curToken,
            currentSentence = curSentence,
            stats = buildStats()
        )
    }

    companion object {
        /** 常见简单词，用于“过滤简单词”开关 */
        val DEFAULT_SIMPLE_WORDS: Set<String> = setOf(
            "a", "an", "i", "my", "me", "you", "your", "he", "his", "she", "her", "it",
            "what", "who", "where", "how", "when", "which", "be", "am", "is", "was", "are",
            "were", "do", "did", "does", "can", "could", "will", "would", "the", "that",
            "this", "and", "not", "no", "yes", "to", "of", "for", "at", "in", "on", "as",
            "if", "or", "so", "we", "they", "them", "us", "too"
        )
    }
}
