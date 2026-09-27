package com.example.desktop

import com.example.typingarticle.core.audio.AudioService
import com.example.typingarticle.core.engine.TypingEngine
import com.example.typingarticle.core.model.Cursor
import com.example.typingarticle.core.model.Sentence
import com.example.typingarticle.core.model.Token
import com.example.typingarticle.core.model.TokenType
import com.example.typingarticle.feature.word.WordDictationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class NoopAudio : AudioService {
    override fun playSentence(sentenceText: String, audioSrc: String?, lrcStart: Double?, lrcEnd: Double?) {}
    override fun playWord(word: String) {}
    override fun keyClick() {}
    override fun errorBeep() {}
    override fun stop() {}
    override fun setVolume(volume: Float) {}
    override fun setSpeechRate(rate: Float) {}
    override fun setKeyboardSound(name: String) {}
    override fun previewKeyboardSound(name: String) {}
    override fun release() {}
}

class WordProgressTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun waitUntil(timeoutMs: Long = 5000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (cond()) return
            Thread.sleep(30)
        }
        throw AssertionError("waitUntil 超时")
    }

    @Test
    fun engineJumpLandsOnTargetSection() {
        val sections = (0 until 20).map { i ->
            listOf(Sentence(idx = i, text = "word$i", tokens = listOf(Token(0, "word$i", TokenType.Word, true))))
        }
        val engine = TypingEngine()
        engine.loadSections(sections, Cursor(sectionIdx = 7, sentenceIdx = 0, wordIdx = 0, charIdx = 0))
        assertEquals(7, engine.state.value.cursor.sectionIdx)
    }

    @Test
    fun resumesProgressAcrossViewModelReopen() {
        Dispatchers.setMain(Dispatchers.Default)

        val dir = File(System.getProperty("java.io.tmpdir"), "wp_" + System.nanoTime())
        val container = DesktopContainer(dir)
        val words = (1..20).joinToString("\n") { "word$it" }
        val book = runBlocking { container.wordRepository.importWords(words, "T") }

        fun newVm() = WordDictationViewModel(
            wordRepository = container.wordRepository,
            wordMarkRepository = container.wordMarkRepository,
            wrongWordRepository = container.wrongWordRepository,
            statisticRepository = container.statisticRepository,
            settingsRepository = container.settingsRepository,
            audioService = NoopAudio(),
            keyMapper = container.keyMapper,
        )

        // 第一次：打开并前进 3 个词
        val vm1 = newVm()
        vm1.openBook(book.id)
        waitUntil { vm1.uiState.value.totalWords == 20 }
        assertEquals(0, vm1.uiState.value.currentIdx)

        repeat(3) { vm1.nextWord() }
        waitUntil { runBlocking { container.wordRepository.getProgress(book.id) } == 3 }
        val saved = runBlocking { container.wordRepository.getProgress(book.id) }
        println("saved progress = $saved")
        assertEquals(3, saved)

        // 第二次：新建 VM（模拟退出重进）
        val vm2 = newVm()
        vm2.openBook(book.id)
        waitUntil { vm2.uiState.value.totalWords == 20 }
        println("reopened currentIdx = ${vm2.uiState.value.currentIdx}")
        assertEquals(3, vm2.uiState.value.currentIdx, "重进应接着上次的词")

        assertTrue(true)
        container.database.close()
        dir.deleteRecursively()
    }
}
