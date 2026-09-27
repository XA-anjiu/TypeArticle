package com.example.typingarticle.core.parser

import com.example.typingarticle.core.model.Sentence
import com.example.typingarticle.core.model.Token
import com.example.typingarticle.core.model.TokenType

/**
 * 文章文本解析器（纯 Kotlin 实现，禁止依赖 android.*）
 *
 * 职责：
 * 1. 引号归一化（弯引号、各种 Unicode 单双引号 -> 标准直引号）
 * 2. 切段切句（双换行切段，单换行切句，段落与译文严格结构对齐）
 * 3. 词元化 Tokenizer（按优先级识别货币数字、数字、带点缩写、英文单词、符号，并精准判定 nextSpace）
 */
object ArticleParser {

    // 优先级 1: 货币符号 + 数字（含千分位、小数、百分号）
    private val CURRENCY_NUMBER_REGEX = Regex(
        pattern = "^([$¥€£]\\d+(?:,\\d{3})*(?:\\.\\d+)?%?)"
    )

    // 优先级 2: 纯数字（含千分位、小数、百分号）
    private val NUMBER_REGEX = Regex(
        pattern = "^(\\d+(?:,\\d{3})*(?:\\.\\d+)?%?)"
    )

    // 优先级 3: 带点缩写（U.S. / U.S.A. / e.g. / i.e. / Ph.D. / Mr. / Dr. 等）
    private val DOTTED_ABBREVIATION_REGEX = Regex(
        pattern = "^(?:(?:[A-Za-z]\\.){2,}|(?:[A-Za-z]+\\.[A-Za-z]+(?:\\.[A-Za-z]+)*\\.?)|(?:(?:Mr|Mrs|Ms|Dr|Prof|vs|etc|al|No|St|Ltd|Inc|Co)\\.))",
        option = RegexOption.IGNORE_CASE
    )

    // 优先级 4: 英文单词（含撇号与连字符，如 it's, o'clock, mother-in-law）
    private val WORD_REGEX = Regex(
        pattern = "^([A-Za-z]+(?:['\\-][A-Za-z]+)*)"
    )

    /**
     * 7.1 引号归一（切分前必须先做）
     * ‘ ’ ‚ ‛ → '
     * “ ” „ ‟ → "
     */
    fun normalizeQuotes(raw: String): String {
        return raw
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace('‚', '\'')
            .replace('‛', '\'')
            .replace('“', '"')
            .replace('”', '"')
            .replace('„', '"')
            .replace('‟', '"')
    }

    /**
     * 7.2 切段切句
     *
     * 1. 引号归一；
     * 2. 以 \n\n 切段落，丢弃空段；
     * 3. 段内以 \n 切句，丢弃空行；
     * 4. 产出 sections[sectionIdx][sentenceIdx].text，句子译文按同下标从 textTranslate 取。
     */
    fun toSections(
        text: String,
        textTranslate: String? = null,
        lrcPositions: List<Pair<Double, Double>> = emptyList()
    ): List<List<Sentence>> {
        val normalizedText = normalizeQuotes(text).replace("\r\n", "\n").replace("\r", "\n")
        val rawSections = normalizedText.split(Regex("\n{2,}"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val normalizedTranslate = textTranslate?.let {
            normalizeQuotes(it).replace("\r\n", "\n").replace("\r", "\n")
        }
        val rawTranslateSections = normalizedTranslate?.split(Regex("\n{2,}"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }

        var globalSentenceIndex = 0

        return rawSections.mapIndexed { sectionIdx, sectionStr ->
            val sentencesStr = sectionStr.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val translateSentences = rawTranslateSections?.getOrNull(sectionIdx)?.split('\n')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }

            sentencesStr.mapIndexed { sentenceIdx, sentenceText ->
                val trans = translateSentences?.getOrNull(sentenceIdx)
                val lrc = lrcPositions.getOrNull(globalSentenceIndex)
                globalSentenceIndex++

                val tokens = tokenize(sentenceText)
                Sentence(
                    idx = sentenceIdx,
                    text = sentenceText,
                    translate = trans,
                    lrcStart = lrc?.first,
                    lrcEnd = lrc?.second,
                    tokens = tokens
                )
            }
        }
    }

    /**
     * 7.3 词元化（优先级从高到低）
     *
     * 1. [$¥€£]＋数字（含千分位/小数/%） -> Number
     * 2. 纯数字（含千分位/小数/%） -> Number
     * 3. 带点缩写（U.S. / U.S.A. / e.g. / i.e. / Ph.D. 等） -> Word
     * 4. 单词（含 ' 与 -：it's / o'clock / mother-in-law） -> Word
     * 5. 其它非空白单字符（标点） -> Symbol
     * 6. 兜底单字符 -> Symbol
     *
     * nextSpace：本 token 结束位置到下一 token 开始位置之间是否含空白；含 → 该词打完后需按空格。
     */
    fun tokenize(sentence: String): List<Token> {
        val normalized = normalizeQuotes(sentence)
        val tokens = mutableListOf<Token>()
        var index = 0
        var tokenIdx = 0

        while (index < normalized.length) {
            // 跳过可能存在的空白
            if (normalized[index].isWhitespace()) {
                index++
                continue
            }

            val remaining = normalized.substring(index)

            // 按优先级进行正则匹配
            val (tokenText, tokenType) = when {
                // 1. 货币 + 数字
                CURRENCY_NUMBER_REGEX.find(remaining)?.let { it.range.first == 0 } == true -> {
                    val match = CURRENCY_NUMBER_REGEX.find(remaining)!!
                    match.value to TokenType.Number
                }
                // 2. 纯数字
                NUMBER_REGEX.find(remaining)?.let { it.range.first == 0 } == true -> {
                    val match = NUMBER_REGEX.find(remaining)!!
                    match.value to TokenType.Number
                }
                // 3. 带点缩写
                DOTTED_ABBREVIATION_REGEX.find(remaining)?.let { it.range.first == 0 } == true -> {
                    val match = DOTTED_ABBREVIATION_REGEX.find(remaining)!!
                    match.value to TokenType.Word
                }
                // 4. 英文单词
                WORD_REGEX.find(remaining)?.let { it.range.first == 0 } == true -> {
                    val match = WORD_REGEX.find(remaining)!!
                    match.value to TokenType.Word
                }
                // 5 & 6. 符号或单个字符
                else -> {
                    remaining.substring(0, 1) to TokenType.Symbol
                }
            }

            val tokenEnd = index + tokenText.length

            // 检查 token 结束位置与下一个有效 token 之间是否存在空白
            var scanIndex = tokenEnd
            var hasWhitespace = false
            while (scanIndex < normalized.length && normalized[scanIndex].isWhitespace()) {
                hasWhitespace = true
                scanIndex++
            }

            // 仅当后面还有下一个有效词元且中间存在空白时，nextSpace 才为 true
            val nextSpace = hasWhitespace && (scanIndex < normalized.length)

            tokens.add(
                Token(
                    idx = tokenIdx++,
                    text = tokenText,
                    type = tokenType,
                    nextSpace = nextSpace
                )
            )

            index = scanIndex
        }

        return tokens
    }
}
