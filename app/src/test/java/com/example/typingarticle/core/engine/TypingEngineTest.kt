package com.example.typingarticle.core.engine

import com.example.typingarticle.core.model.Cursor
import com.example.typingarticle.core.model.EngineEvent
import com.example.typingarticle.core.model.Sentence
import com.example.typingarticle.core.model.Token
import com.example.typingarticle.core.model.TokenType
import com.example.typingarticle.core.parser.ArticleParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TypingEngineTest {

    @Test
    fun testTypingAndSpaceWaiting() {
        // 用例 5: 词尾空格：nextSpace=true 时进等待态，仅空格可推进
        val engine = TypingEngine(ignoreCase = true)
        val sections = ArticleParser.toSections("Hi you.")
        engine.loadSections(sections)

        // 初始状态应在 "Hi" (nextSpace = true)
        assertEquals("Hi", engine.currentToken()?.text)
        assertFalse(engine.state.value.isSpace)

        // 输入 'H'
        engine.onKey('H')
        assertEquals("H", engine.state.value.input)
        assertFalse(engine.state.value.isSpace)

        // 输入 'i' -> 词打完，因为 nextSpace=true，进入 isSpace 等待态
        engine.onKey('i')
        assertTrue("打完 Hi 后因为有空格，必须进入 isSpace 状态", engine.state.value.isSpace)

        // 按空格推进到下一词 "you"
        engine.onSpace()
        assertFalse("按下空格后必须退出等待态并进入下一词", engine.state.value.isSpace)
        assertEquals("you", engine.currentToken()?.text)
    }

    @Test
    fun testReplayKeyWhenInSpaceWaitingState() {
        // 关键坑点 2: isSpace 等待态按非空格键，要推进 + 重放该键，不吞字符
        val engine = TypingEngine(ignoreCase = true)
        val sections = ArticleParser.toSections("Go now.")
        engine.loadSections(sections)

        // 打完 "Go"
        engine.onKey('g')
        engine.onKey('o')
        assertTrue(engine.state.value.isSpace)

        // 用户未按空格直接按了下一词 "now" 的首字母 'n'
        engine.onKey('n')
        assertFalse(engine.state.value.isSpace)
        assertEquals("now", engine.currentToken()?.text)
        assertEquals("首字母 n 必须被新词接收，不得吞音", "n", engine.state.value.input)
        assertEquals(1, engine.state.value.cursor.charIdx)
    }

    @Test
    fun testBackspaceAcrossBoundaries() {
        // 用例 6: 段末回落：跨词 / 跨句 / 跨段回删落点正确
        val text = """
            One two.

            Three.
        """.trimIndent()
        val sections = ArticleParser.toSections(text)
        val engine = TypingEngine(ignoreCase = true)
        engine.loadSections(sections)

        // 打完第一段第一句："One two."
        // One
        engine.onKey('O'); engine.onKey('n'); engine.onKey('e'); engine.onSpace()
        // two
        engine.onKey('t'); engine.onKey('w'); engine.onKey('o')
        // . (Symbol, nextSpace=false)
        engine.onKey('.')

        // 推进到第二段 "Three."
        assertEquals("Three", engine.currentToken()?.text)
        assertEquals(1, engine.state.value.cursor.sectionIdx)
        assertEquals(0, engine.state.value.cursor.sentenceIdx)
        assertEquals(0, engine.state.value.cursor.charIdx)

        // 在第二段开头回删（跨段回退到上一段末尾）
        engine.onBackspace()
        assertEquals("应当回退到第 0 段", 0, engine.state.value.cursor.sectionIdx)
        assertEquals("应当回退到上一句末词符号", ".", engine.currentToken()?.text)

        // 再次回删，退到 "two"
        engine.onBackspace()
        assertEquals("two", engine.currentToken()?.text)
    }

    @Test
    fun testNameListAutoSkip() {
        // 用例 7: nameList 命中词自动跳过
        val text = "Welcome Dr. Smith to campus."
        val sections = ArticleParser.toSections(text)
        val engine = TypingEngine(
            ignoreCase = true,
            nameList = setOf("Dr.", "Smith")
        )
        engine.loadSections(sections)

        // "Welcome"
        assertEquals("Welcome", engine.currentToken()?.text)
        engine.onKey('W'); engine.onKey('e'); engine.onKey('l'); engine.onKey('c'); engine.onKey('o'); engine.onKey('m'); engine.onKey('e')
        engine.onSpace()

        // 此时应当自动跳过 "Dr." 和 "Smith"，直接到达 "to"
        assertEquals("to", engine.currentToken()?.text)
    }

    @Test
    fun testArticleCompleteEvent() = runTest(UnconfinedTestDispatcher()) {
        // 用例 8: 书末：触发完成事件、统计落库
        val text = "Go."
        val sections = ArticleParser.toSections(text)
        val engine = TypingEngine(ignoreCase = true)
        engine.loadSections(sections)

        val events = mutableListOf<EngineEvent>()
        val job = launch { engine.events.toList(events) }

        engine.onKey('G')
        engine.onKey('o')
        engine.onKey('.')

        assertTrue("打完最后词句后必须标记 isEnd", engine.state.value.isEnd)
        val hasCompleteEvent = events.any { it is EngineEvent.Complete }
        assertTrue("必须抛出 Complete 事件", hasCompleteEvent)

        job.cancel()
    }

    @Test
    fun testConsecutiveErrorsMergedToOne() {
        val text = "Apple"
        val sections = ArticleParser.toSections(text)
        val engine = TypingEngine(ignoreCase = true)
        engine.loadSections(sections)

        // 连续输错 3 次
        engine.onKey('x')
        engine.onKey('y')
        engine.onKey('z')

        // 应当只计 1 次错误（同词连续错合并）
        assertEquals(1, engine.state.value.stats.wrongCount)

        // 正确输入
        engine.onKey('A')
        assertEquals(1, engine.state.value.cursor.charIdx)
    }

    @Test
    fun testJumpCursor() {
        val text = """
            First.
            Second.

            Third.
        """.trimIndent()
        val sections = ArticleParser.toSections(text)
        val engine = TypingEngine(ignoreCase = true)
        engine.loadSections(sections)

        // 跳转到第 1 段（即第三句）
        engine.jump(Cursor(sectionIdx = 1, sentenceIdx = 0, wordIdx = 0, charIdx = 2))
        assertEquals(1, engine.state.value.cursor.sectionIdx)
        assertEquals("Third", engine.currentToken()?.text)
        assertEquals("Th", engine.state.value.input)
    }

    @Test
    fun testRawInputKeepsWrongChars() {
        // 打错的字符在当前字符位必须保留在 rawInput 中，供前端渲染“打错标红”；
        // 退格可清除，输入正确字符后清除并推进。
        val sections = ArticleParser.toSections("Apple")
        val engine = TypingEngine(ignoreCase = true)
        engine.loadSections(sections)

        engine.onKey('A')   // 正确 -> k=1
        engine.onKey('x')   // 错误 -> k 不动，错字进入 rawInput
        assertEquals("Ax", engine.state.value.rawInput)
        assertEquals("A", engine.state.value.input)
        assertEquals(1, engine.state.value.cursor.charIdx)

        // 退格优先清除错字，光标不后退
        engine.onBackspace()
        assertEquals("A", engine.state.value.rawInput)
        assertEquals(1, engine.state.value.cursor.charIdx)

        engine.onKey('x')   // 再次打错
        engine.onKey('p')   // 正确 -> 清除错字并推进
        assertEquals("Ap", engine.state.value.rawInput)
        assertEquals("Ap", engine.state.value.input)
        assertEquals(2, engine.state.value.cursor.charIdx)
    }

    @Test
    fun testBackspaceDeletesWrongCharAtWordStart() {
        // 词首打错（k==0）时，退格应清除错字而不是跳到上一个词
        val sections = ArticleParser.toSections("Apple Ball")
        val engine = TypingEngine(ignoreCase = true)
        engine.loadSections(sections)

        engine.onKey('x') // 词首打错，k 仍为 0
        assertEquals(0, engine.state.value.cursor.charIdx)
        assertEquals("x", engine.state.value.rawInput)

        engine.onBackspace()
        assertEquals("", engine.state.value.rawInput)
        assertEquals("Apple", engine.currentToken()?.text)
        assertEquals(0, engine.state.value.cursor.charIdx)
    }

    @Test
    fun testIgnoreSimpleWordSkipped() {
        // 过滤简单词开关生效：the 属简单词，自动跳过
        val sections = ArticleParser.toSections("The cat sat")
        val engine = TypingEngine(ignoreCase = true, ignoreSimpleWord = true)
        engine.loadSections(sections)
        assertEquals("cat", engine.currentToken()?.text)
    }

    @Test
    fun testKnownWordSkipped() {
        // 已掌握词自动跳过
        val sections = ArticleParser.toSections("Hello world")
        val engine = TypingEngine(ignoreCase = true, knownWords = setOf("hello"))
        engine.loadSections(sections)
        assertEquals("world", engine.currentToken()?.text)
    }
}
