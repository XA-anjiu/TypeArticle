package com.example.typingarticle.core.parser

import com.example.typingarticle.core.model.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 对应 Section 15.1 必测用例：词元化与文本解析
 */
class ArticleParserTest {

    @Test
    fun testAbbreviationTokens() {
        // 用例 1: U.S.A. / e.g. / i.e. 作为单个 Word token
        val tokensUSA = ArticleParser.tokenize("U.S.A. is vast.")
        assertEquals("U.S.A.", tokensUSA[0].text)
        assertEquals(TokenType.Word, tokensUSA[0].type)

        val tokensEg = ArticleParser.tokenize("For example, e.g. this one.")
        val egToken = tokensEg.firstOrNull { it.text.equals("e.g.", ignoreCase = true) }
        assertTrue("e.g. 必须被整体解析为单个词元", egToken != null)
        assertEquals(TokenType.Word, egToken?.type)

        val tokensPhd = ArticleParser.tokenize("He got a Ph.D. in physics.")
        val phdToken = tokensPhd.firstOrNull { it.text.equals("Ph.D.", ignoreCase = true) }
        assertTrue("Ph.D. 必须被整体解析为单个词元", phdToken != null)
        assertEquals(TokenType.Word, phdToken?.type)
    }

    @Test
    fun testNumberTokens() {
        // 用例 2: 35% / 3.14 / 1,000 / $1,000.50 作为 Number token
        val tokens = ArticleParser.tokenize("Rates: 35% and 3.14 or 1,000 and $1,000.50.")

        val t35 = tokens.first { it.text == "35%" }
        assertEquals(TokenType.Number, t35.type)

        val tPi = tokens.first { it.text == "3.14" }
        assertEquals(TokenType.Number, tPi.type)

        val tThousand = tokens.first { it.text == "1,000" }
        assertEquals(TokenType.Number, tThousand.type)

        val tCurrency = tokens.first { it.text == "$1,000.50" }
        assertEquals(TokenType.Number, tCurrency.type)
    }

    @Test
    fun testCompoundWordTokens() {
        // 用例 3: it's / o'clock / mother-in-law 不被拆开
        val tokens = ArticleParser.tokenize("It's 5 o'clock with mother-in-law.")

        val itsToken = tokens[0]
        assertEquals("It's", itsToken.text)
        assertEquals(TokenType.Word, itsToken.type)

        val oclockToken = tokens.first { it.text == "o'clock" }
        assertEquals(TokenType.Word, oclockToken.type)

        val motherToken = tokens.first { it.text == "mother-in-law" }
        assertEquals(TokenType.Word, motherToken.type)
    }

    @Test
    fun testSmartQuotesNormalization() {
        // 用例 4: 智能引号 “ ” ’ 归一后与直引号一致匹配
        val raw = "“Hello,” he said, ‘It’s fine.’"
        val normalized = ArticleParser.normalizeQuotes(raw)
        assertEquals("\"Hello,\" he said, 'It's fine.'", normalized)

        val tokens = ArticleParser.tokenize(raw)
        val wordIts = tokens.first { it.text == "It's" }
        assertEquals(TokenType.Word, wordIts.type)
    }

    @Test
    fun testNextSpaceBehavior() {
        // 用例 5: 词尾空格判定
        val sentence = "Hello, world! 100% true."
        val tokens = ArticleParser.tokenize(sentence)

        // "Hello" 后紧接 ","，中间无空格 -> nextSpace 应为 false
        assertEquals("Hello", tokens[0].text)
        assertFalse(tokens[0].nextSpace)

        // "," 后有空格 -> nextSpace 应为 true
        assertEquals(",", tokens[1].text)
        assertTrue(tokens[1].nextSpace)

        // "world" 后紧接 "!" -> nextSpace 应为 false
        assertEquals("world", tokens[2].text)
        assertFalse(tokens[2].nextSpace)

        // "!" 后有空格 -> nextSpace 应为 true
        assertEquals("!", tokens[3].text)
        assertTrue(tokens[3].nextSpace)

        // "100%" 后有空格 -> nextSpace 应为 true
        assertEquals("100%", tokens[4].text)
        assertTrue(tokens[4].nextSpace)

        // "true" 后紧接 "." -> nextSpace 应为 false
        assertEquals("true", tokens[5].text)
        assertFalse(tokens[5].nextSpace)

        // "." 为全句最末 -> nextSpace 为 false
        assertEquals(".", tokens[6].text)
        assertFalse(tokens[6].nextSpace)
    }

    @Test
    fun testToSectionsStructure() {
        val text = """
            First sentence of section one.
            Second sentence of section one.

            First sentence of section two.
        """.trimIndent()

        val textTranslate = """
            第一段第一句。
            第一段第二句。

            第二段第一句。
        """.trimIndent()

        val sections = ArticleParser.toSections(text, textTranslate)
        assertEquals(2, sections.size)
        assertEquals(2, sections[0].size)
        assertEquals(1, sections[1].size)

        assertEquals("First sentence of section one.", sections[0][0].text)
        assertEquals("第一段第一句。", sections[0][0].translate)
        assertEquals("Second sentence of section one.", sections[0][1].text)
        assertEquals("第一段第二句。", sections[0][1].translate)
        assertEquals("First sentence of section two.", sections[1][0].text)
        assertEquals("第二段第一句。", sections[1][0].translate)
    }
}
