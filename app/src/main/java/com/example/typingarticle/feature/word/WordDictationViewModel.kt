package com.example.typingarticle.feature.word

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.typingarticle.core.audio.AudioService
import com.example.typingarticle.core.data.local.entity.WordEntity
import com.example.typingarticle.core.data.repository.StatisticRepository
import com.example.typingarticle.core.data.repository.WordMarkRepository
import com.example.typingarticle.core.data.repository.WordRepository
import com.example.typingarticle.core.data.repository.WrongWordRepository
import com.example.typingarticle.core.data.repository.WrongWordStat
import com.example.typingarticle.core.engine.KeyCommand
import com.example.typingarticle.core.engine.KeyMapper
import com.example.typingarticle.core.engine.TypingEngine
import com.example.typingarticle.core.model.EngineEvent
import com.example.typingarticle.core.model.EngineSnapshot
import com.example.typingarticle.core.model.Sentence
import com.example.typingarticle.core.model.Settings
import com.example.typingarticle.core.model.Token
import com.example.typingarticle.core.model.TokenType
import com.example.typingarticle.core.model.WordMode
import com.example.typingarticle.core.model.WordStats
import com.example.typingarticle.core.settings.SettingsRepository
import com.example.typingarticle.feature.practice.TokenRender
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 单词默写 ViewModel
 *
 * 遵循 Section 12.5 与 13.3 规范：
 * - 纯逻辑，无 Compose 界面
 * - 引擎 100% 复用：每个单词作为单 Token（nextSpace=false）的独立句节
 * - 自动下一题（无需敲击空格）
 * - 错词自动入 word_mark 库，支持一键错词重练
 */
class WordDictationViewModel(
    private val wordRepository: WordRepository,
    private val wordMarkRepository: WordMarkRepository,
    private val wrongWordRepository: WrongWordRepository,
    private val statisticRepository: StatisticRepository,
    private val settingsRepository: SettingsRepository,
    private val audioService: AudioService,
    private val keyMapper: KeyMapper = KeyMapper()
) : ViewModel() {

    private val engine = TypingEngine()

    private val _uiState = MutableStateFlow(WordUiState())
    val uiState: StateFlow<WordUiState> = _uiState.asStateFlow()

    /** 后台错词统计（隐藏词库，不显示在侧栏 / 词库列表） */
    val wrongStats: StateFlow<List<WrongWordStat>> =
        wrongWordRepository.observe()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var currentWordList: List<WordEntity> = emptyList()
    private var settings: Settings = Settings()
    private val wrongWordsSet = mutableSetOf<String>()
    private var startedAt: Long = System.currentTimeMillis()
    private var totalSpendMs: Long = 0L

    private var timerJob: Job? = null
    // 仅当页面处于活动状态时才累计学习时间
    private var isSessionActive: Boolean = true

    init {
        viewModelScope.launch {
            settingsRepository.observe().collectLatest { s ->
                settings = s
                engine.ignoreCase = s.wordIgnoreCase
                audioService.setVolume(s.volume)
                audioService.setSpeechRate(s.speechRate)
                audioService.setKeyboardSound(s.keyboardSound)

                _uiState.value = _uiState.value.copy(
                    showMeaning = s.showMeaning,
                    showPhonetic = s.showPhonetic,
                    isListeningMode = s.wordMode == WordMode.LISTENING
                )
            }
        }

        viewModelScope.launch {
            engine.state.collectLatest { snapshot ->
                renderWordState(snapshot)
            }
        }

        viewModelScope.launch {
            engine.events.collect { event ->
                handleEngineEvent(event)
            }
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (isSessionActive) {
                    totalSpendMs += 1000
                    updateWordStats()
                }
            }
        }
    }

    fun openBook(bookId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val book = wordRepository.getWordBook(bookId)
            val words = wordRepository.getWords(bookId)

            if (words.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "词库为空或未找到 (bookId=$bookId)"
                )
                return@launch
            }

            currentWordList = words.shuffled()
            wrongWordsSet.clear()
            startedAt = System.currentTimeMillis()
            totalSpendMs = 0L

            loadWordsIntoEngine(currentWordList)

            _uiState.value = _uiState.value.copy(
                bookId = bookId,
                bookName = book?.name ?: "单词默写",
                totalWords = words.size,
                isWrongOnlyMode = false,
                isLoading = false
            )

            // 首次发音当前词
            replayWord()
        }
    }

    /**
     * 错词重练：仅练习错误本中的词汇
     */
    fun replayWrongOnly() {
        val currentBookId = _uiState.value.bookId
        if (currentBookId.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val wrongWords = wordRepository.retryWrongOnly(currentBookId)

            if (wrongWords.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "太棒了，目前暂无错词记录！"
                )
                return@launch
            }

            currentWordList = wrongWords.shuffled()
            wrongWordsSet.clear()
            startedAt = System.currentTimeMillis()
            totalSpendMs = 0L

            loadWordsIntoEngine(currentWordList)

            _uiState.value = _uiState.value.copy(
                totalWords = wrongWords.size,
                isWrongOnlyMode = true,
                isLoading = false
            )

            replayWord()
        }
    }

    fun onKey(key: String, code: String, isCtrl: Boolean = false, isAlt: Boolean = false) {
        val command = keyMapper.map(key = key, code = code, isCtrl = isCtrl, isAlt = isAlt)
        when (command) {
            is KeyCommand.InputChar -> {
                engine.onKey(command.char)
            }
            KeyCommand.Space -> {
                if (engine.state.value.isSpace) engine.onSpace()
            }
            KeyCommand.Confirm -> {
                if (engine.state.value.isSpace) engine.onSpace()
            }
            KeyCommand.Backspace -> {
                engine.onBackspace()
            }
            KeyCommand.NextSentence -> {
                nextWord()
            }
            KeyCommand.PrevSentence -> {
                prevWord()
            }
            KeyCommand.ReplaySentence -> {
                replayWord()
            }
            else -> {}
        }
    }

    fun onBackspace() {
        engine.onBackspace()
    }

    /** 由界面按生命周期调用：进入/离开页面时切换计时。 */
    fun setSessionActive(active: Boolean) {
        isSessionActive = active
    }

    fun nextWord() {
        engine.advanceNextSentence()
        replayWord()
    }

    fun prevWord() {
        engine.prevSentence()
        replayWord()
    }

    fun replayWord() {
        val word = currentWord() ?: return
        audioService.playWord(word.word)
    }

    fun toggleMeaning() {
        viewModelScope.launch {
            val updated = !_uiState.value.showMeaning
            settingsRepository.updateSettings { it.copy(showMeaning = updated) }
        }
    }

    fun togglePhonetic() {
        viewModelScope.launch {
            val updated = !_uiState.value.showPhonetic
            settingsRepository.updateSettings { it.copy(showPhonetic = updated) }
        }
    }

    /** 依据后台错词统计生成一个新的可见词库 */
    suspend fun createBookFromWrongStats(bookName: String) {
        val words = wrongWordRepository.all().map { it.word }
        if (words.isEmpty()) return
        val book = wordRepository.createBookFromWords(bookName, words)
        openBook(book.id)
    }

    fun clearWrongStats() {
        viewModelScope.launch { wrongWordRepository.clear() }
    }

    private fun loadWordsIntoEngine(words: List<WordEntity>) {
        // 将单词序列抽象为段落列表：每个单词是一个 section，包含一句单一 Token。
        // nextSpace=true：拼完不自动跳转，需按空格 / 回车才进入下一个单词，便于检查拼写。
        val sections = words.mapIndexed { idx, wordEntity ->
            listOf(
                Sentence(
                    idx = idx,
                    text = wordEntity.word,
                    translate = wordEntity.trans,
                    tokens = listOf(
                        Token(
                            idx = 0,
                            text = wordEntity.word,
                            type = TokenType.Word,
                            nextSpace = true
                        )
                    )
                )
            )
        }
        engine.loadSections(sections)
    }

    private fun handleEngineEvent(event: EngineEvent) {
        when (event) {
            is EngineEvent.CorrectChar -> {
                if (settings.keySoundEnabled) {
                    audioService.keyClick()
                }
            }
            is EngineEvent.WrongChar -> {
                if (settings.errorSoundEnabled) {
                    audioService.errorBeep()
                }
                // 记入后台错词统计（去重、累计次数）
                val word = currentWord()
                if (word != null) {
                    wrongWordsSet.add(word.word)
                    viewModelScope.launch {
                        wrongWordRepository.record(word.word)
                    }
                    updateWordStats()
                }
            }
            is EngineEvent.NextSentence -> {
                // 自动下一词时发音
                replayWord()
            }
            is EngineEvent.Complete -> {
                val currentBookId = _uiState.value.bookId
                viewModelScope.launch {
                    statisticRepository.saveStatistic(
                        refType = "word",
                        refId = currentBookId,
                        startedAt = startedAt,
                        spendMs = totalSpendMs,
                        total = currentWordList.size,
                        wrong = wrongWordsSet.size
                    )
                }
            }
            else -> {}
        }
    }

    private fun currentWord(): WordEntity? {
        val curSection = engine.state.value.cursor.sectionIdx
        return currentWordList.getOrNull(curSection)
    }

    private fun renderWordState(snapshot: EngineSnapshot) {
        val wordEntity = currentWordList.getOrNull(snapshot.cursor.sectionIdx)
        val charIdx = snapshot.cursor.charIdx
        val isFinished = snapshot.isEnd

        val tokenRender = if (wordEntity != null) {
            val fullText = wordEntity.word
            val correctPrefix = fullText.take(charIdx)
            val remaining = fullText.drop(charIdx)
            TokenRender(
                idx = 0,
                text = fullText,
                type = TokenType.Word,
                nextSpace = false,
                isWaitingSpace = false,
                isCurrent = !isFinished,
                isCompleted = isFinished,
                correctPrefix = correctPrefix,
                typedRaw = snapshot.rawInput,
                errorChar = null,
                remainingText = remaining
            )
        } else null

        _uiState.value = _uiState.value.copy(
            currentIdx = snapshot.cursor.sectionIdx,
            word = wordEntity?.word ?: "",
            phonetic = wordEntity?.phonetic0 ?: wordEntity?.phonetic1,
            trans = wordEntity?.trans,
            tokenRender = tokenRender,
            isEnd = isFinished
        )
        updateWordStats()
    }

    private fun updateWordStats() {
        val curIdx = _uiState.value.currentIdx
        val total = currentWordList.size
        _uiState.value = _uiState.value.copy(
            stats = WordStats(
                totalTested = (curIdx + 1).coerceAtMost(total),
                wrongCount = wrongWordsSet.size,
                spendMs = totalSpendMs,
                wrongWords = wrongWordsSet.toList()
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioService.stop()
        timerJob?.cancel()
    }
}
