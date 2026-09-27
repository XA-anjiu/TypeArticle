package com.example.typingarticle.feature.word

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.typingarticle.core.audio.AudioService
import com.example.typingarticle.core.data.local.AppDatabase
import com.example.typingarticle.core.data.repository.StatisticRepository
import com.example.typingarticle.core.data.repository.WordMarkRepository
import com.example.typingarticle.core.data.repository.WordRepository
import com.example.typingarticle.core.data.repository.WrongWordRepository
import com.example.typingarticle.core.model.WordMode
import com.example.typingarticle.core.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WordDictationTest {

    private lateinit var db: AppDatabase
    private lateinit var wordRepo: WordRepository
    private lateinit var wordMarkRepo: WordMarkRepository
    private lateinit var wrongWordRepo: WrongWordRepository
    private lateinit var statRepo: StatisticRepository
    private lateinit var settingsRepo: SettingsRepository

    private val fakeAudioService = object : AudioService {
        var playedWords = mutableListOf<String>()
        override fun playSentence(sentenceText: String, audioSrc: String?, lrcStart: Double?, lrcEnd: Double?) {}
        override fun playWord(word: String) { playedWords.add(word) }
        override fun keyClick() {}
        override fun errorBeep() {}
        override fun stop() {}
        override fun setVolume(volume: Float) {}
        override fun setSpeechRate(rate: Float) {}
        override fun setKeyboardSound(name: String) {}
        override fun previewKeyboardSound(name: String) {}
        override fun release() {}
    }

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        wordRepo = WordRepository(db.wordBookDao(), db.wordDao(), db.wordMarkDao(), db.wordProgressDao())
        wordMarkRepo = WordMarkRepository(db.wordMarkDao())
        wrongWordRepo = WrongWordRepository(db.wrongWordDao())
        statRepo = StatisticRepository(db.statisticDao())
        settingsRepo = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun awaitTrue(timeoutMs: Long = 15000, cond: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (cond()) return
            delay(20)
        }
        throw AssertionError("awaitTrue timeout after ${timeoutMs}ms")
    }

    private fun newViewModel() = WordDictationViewModel(
        wordRepository = wordRepo,
        wordMarkRepository = wordMarkRepo,
        wrongWordRepository = wrongWordRepo,
        statisticRepository = statRepo,
        settingsRepository = settingsRepo,
        audioService = fakeAudioService
    )

    @Test
    fun testWordImportTxtJsonCsv() = runTest(UnconfinedTestDispatcher()) {
        val txtContent = """
            apple
            banana
            apple
        """.trimIndent()
        val bookTxt = wordRepo.importWords(txtContent, "TXT库", "txt")
        val wordsTxt = wordRepo.getWords(bookTxt.id)
        assertEquals("TXT 导入且自动去重后应为 2 词", 2, wordsTxt.size)

        val jsonContent = """
            [
              {"word": "computer", "phonetic0": "/kəmˈpjuːtə/", "trans": "计算机"},
              {"word": "keyboard", "phonetic0": "/ˈkiːbɔːd/", "trans": "键盘"}
            ]
        """.trimIndent()
        val bookJson = wordRepo.importWords(jsonContent, "JSON库", "json")
        val wordsJson = wordRepo.getWords(bookJson.id)
        assertEquals(2, wordsJson.size)
        assertEquals("computer", wordsJson[0].word)
        assertEquals("计算机", wordsJson[0].trans)

        val csvContent = """
            单词,音标,音标2,翻译
            network,/ˈnetwɜːk/,,网络
            database,/ˈdeɪtəbeɪs/,,数据库
        """.trimIndent()
        val bookCsv = wordRepo.importWords(csvContent, "CSV库", "csv")
        val wordsCsv = wordRepo.getWords(bookCsv.id)
        assertEquals(2, wordsCsv.size)
        assertEquals("network", wordsCsv[0].word)
    }

    @Test
    fun testWordDictationFlowAndManualAdvance() = runBlocking {
        // 逐字符判定；拼完不自动跳转，按空格确认才进入下一题；错词记入错词统计；（词序随机）
        val book = wordRepo.importWords("cat\ndog", "动物库", "txt")
        val viewModel = newViewModel()
        viewModel.openBook(book.id)
        awaitTrue { viewModel.uiState.value.word.isNotEmpty() }
        val first = viewModel.uiState.value.word

        viewModel.onKey("x", "KeyX")
        awaitTrue { wrongWordRepo.all().any { it.word == first } }

        first.forEach { viewModel.onKey(it.toString(), "Key") }
        assertEquals("拼完后不应自动跳转", first, viewModel.uiState.value.word)

        viewModel.onKey("", "Space")
        assertNotEquals("按空格后应切到下一题", first, viewModel.uiState.value.word)
    }

    @Test
    fun testRetryWrongOnly() = runBlocking {
        val book = wordRepo.importWords("apple\nbanana\norange", "水果库", "txt")
        wordMarkRepo.mark("banana", "wrong")

        val viewModel = newViewModel()
        viewModel.openBook(book.id)
        awaitTrue { viewModel.uiState.value.totalWords == 3 }

        viewModel.replayWrongOnly()
        awaitTrue { viewModel.uiState.value.totalWords == 1 }

        assertEquals("错词重练应仅包含 1 个错词", 1, viewModel.uiState.value.totalWords)
        assertEquals("banana", viewModel.uiState.value.word)
        assertTrue(viewModel.uiState.value.isWrongOnlyMode)
    }

    @Test
    fun testListeningMode() = runBlocking {
        settingsRepo.updateSettings { it.copy(wordMode = WordMode.LISTENING) }
        val book = wordRepo.importWords("sun", "太阳", "txt")

        val viewModel = newViewModel()
        viewModel.openBook(book.id)
        awaitTrue { viewModel.uiState.value.word == "sun" }
        awaitTrue { viewModel.uiState.value.isListeningMode }

        assertTrue(viewModel.uiState.value.isListeningMode)

        viewModel.onKey("s", "KeyS")
        viewModel.onKey("u", "KeyU")
        viewModel.onKey("n", "KeyN")

        assertFalse("拼完最后一词后需确认才结束", viewModel.uiState.value.isEnd)

        viewModel.onKey("", "Enter")

        assertTrue("确认最后一词后标记为结束", viewModel.uiState.value.isEnd)
    }
}
