package com.example.typingarticle.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.typingarticle.core.audio.AudioService
import com.example.typingarticle.core.data.repository.ContentRepository
import com.example.typingarticle.core.data.repository.ProgressRepository
import com.example.typingarticle.core.data.repository.StatisticRepository
import com.example.typingarticle.core.data.repository.WordMarkRepository
import com.example.typingarticle.core.engine.KeyCommand
import com.example.typingarticle.core.engine.KeyMapper
import com.example.typingarticle.core.engine.TypingEngine
import com.example.typingarticle.core.model.Article
import com.example.typingarticle.core.model.EngineEvent
import com.example.typingarticle.core.model.EngineSnapshot
import com.example.typingarticle.core.model.Sentence
import com.example.typingarticle.core.model.Settings
import com.example.typingarticle.core.parser.ArticleParser
import com.example.typingarticle.core.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 文章跟打 ViewModel
 *
 * 遵循 Section 13.2 呈现层契约（纯逻辑，无 Compose 界面代码）
 * 前端依据 uiState 与公开命令完成所有交互。
 */
class PracticeViewModel(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val statisticRepository: StatisticRepository,
    private val settingsRepository: SettingsRepository,
    private val audioService: AudioService,
    private val wordMarkRepository: WordMarkRepository,
    private val keyMapper: KeyMapper = KeyMapper()
) : ViewModel() {

    private val engine = TypingEngine()

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var currentArticle: Article? = null
    private var parsedSections: List<List<Sentence>> = emptyList()
    private var settings: Settings = Settings()

    // 渲染缓存：光标未变时直接返回，避免每秒 tick 都重建整篇文章模型
    private var articleLoadToken = 0
    private var lastRenderKey: String? = null

    private var saveProgressJob: Job? = null
    private var tickerJob: Job? = null
    // 仅当页面处于活动状态时才累计学习时间（避免加载/后台空闲计时）
    private var isSessionActive: Boolean = true

    init {
        // 观察全局设置变化
        viewModelScope.launch {
            settingsRepository.observe().collectLatest { newSettings ->
                settings = newSettings
                engine.ignoreCase = newSettings.ignoreCase
                engine.ignoreSymbol = newSettings.ignoreSymbol
                engine.ignoreSimpleWord = newSettings.ignoreSimpleWord
                engine.simpleWords = com.example.typingarticle.core.engine.TypingEngine.DEFAULT_SIMPLE_WORDS
                engine.nameList = newSettings.nameList
                audioService.setVolume(newSettings.volume)
                audioService.setSpeechRate(newSettings.speechRate)
                audioService.setKeyboardSound(newSettings.keyboardSound)

                _uiState.value = _uiState.value.copy(
                    dictation = newSettings.dictation,
                    translate = newSettings.translate
                )
            }
        }

        // 观察“已掌握词”，实时同步到引擎的过滤集合
        viewModelScope.launch {
            wordMarkRepository.observe("known").collectLatest { known ->
                engine.knownWords = known.map { it.lowercase() }.toSet()
                if (!engine.state.value.isEnd) {
                    engine.jump(engine.state.value.cursor)
                }
            }
        }

        // 观察引擎快照并生成前端呈现状态
        viewModelScope.launch {
            engine.state.collectLatest { snapshot ->
                renderState(snapshot)
                debounceSaveProgress(snapshot)
            }
        }

        // 监听引擎核心事件驱动音频与统计落库
        viewModelScope.launch {
            engine.events.collect { event ->
                handleEngineEvent(event)
            }
        }

        // 启动秒级计时器
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (isSessionActive) {
                    engine.tick(1000)
                }
            }
        }
    }

    /**
     * 打开指定书籍中的某篇范文
     */
    fun openArticle(bookId: String, idx: Int, restart: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val article = contentRepository.getArticle(bookId, idx)
            val totalCount = contentRepository.getArticleCount(bookId)
            val book = contentRepository.getBook(bookId)
            val articleList = book?.articles?.map {
                ArticleItemUi(idx = it.idx, title = it.title, titleTranslate = it.titleTranslate)
            } ?: emptyList()

            if (article == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "未能找到对应文章 (bookId=$bookId, idx=$idx)"
                )
                return@launch
            }

            currentArticle = article
            // 实时切分段句与词元，绝不存库
            parsedSections = ArticleParser.toSections(
                text = article.text,
                textTranslate = article.textTranslate,
                lrcPositions = article.lrcPosition
            )

            // 断点续练：从数据库载入之前练习的游标；重练时强制从头开始
            val savedCursor = if (restart) null else progressRepository.load(article.id)
            if (restart) progressRepository.clear(article.id)
            articleLoadToken++
            lastRenderKey = null
            engine.loadSections(parsedSections, savedCursor)

            _uiState.value = _uiState.value.copy(
                articleId = article.id,
                bookId = bookId,
                bookName = book?.name ?: "",
                title = article.title,
                titleTranslate = article.titleTranslate,
                articleIdx = idx,
                totalArticles = totalCount,
                articleList = articleList,
                isLoading = false
            )

            // 自动发音新句（若配置开启）
            val firstSentence = engine.currentSentence()
            if (firstSentence != null) {
                playSentenceAudio(firstSentence)
            }
        }
    }

    /**
     * 响应实体键盘按键
     */
    fun onKey(key: String, code: String, isCtrl: Boolean = false, isAlt: Boolean = false) {
        val command = keyMapper.map(key = key, code = code, isCtrl = isCtrl, isAlt = isAlt)
        when (command) {
            is KeyCommand.InputChar -> {
                engine.onKey(command.char)
            }
            KeyCommand.Space -> {
                engine.onSpace()
            }
            KeyCommand.Backspace -> {
                engine.onBackspace()
            }
            KeyCommand.Confirm -> {
                nextSentence()
            }
            KeyCommand.NextSentence -> {
                nextSentence()
            }
            KeyCommand.PrevSentence -> {
                prevSentence()
            }
            KeyCommand.ReplaySentence -> {
                replay()
            }
            KeyCommand.ToggleDictation -> {
                toggleDictation()
            }
            KeyCommand.ToggleTranslate -> {
                toggleTranslate()
            }
            KeyCommand.NextArticle -> {
                nextArticle()
            }
            KeyCommand.Ignored -> {}
        }
    }

    fun onBackspace() {
        engine.onBackspace()
    }

    fun nextSentence() {
        engine.advanceNextSentence()
        engine.currentSentence()?.let { playSentenceAudio(it) }
    }

    fun prevSentence() {
        engine.prevSentence()
        engine.currentSentence()?.let { playSentenceAudio(it) }
    }

    fun replay() {
        engine.currentSentence()?.let { playSentenceAudio(it) }
    }

    fun toggleDictation() {
        viewModelScope.launch {
            val updated = !_uiState.value.dictation
            settingsRepository.updateSettings { it.copy(dictation = updated) }
        }
    }

    fun toggleTranslate() {
        viewModelScope.launch {
            val updated = !_uiState.value.translate
            settingsRepository.updateSettings { it.copy(translate = updated) }
        }
    }

    fun nextArticle() {
        val state = _uiState.value
        if (state.articleIdx + 1 < state.totalArticles) {
            openArticle(state.bookId, state.articleIdx + 1)
        }
    }

    fun prevArticle() {
        val state = _uiState.value
        if (state.articleIdx > 0) {
            openArticle(state.bookId, state.articleIdx - 1)
        }
    }

    fun exportImport(json: String) {
        viewModelScope.launch {
            val book = contentRepository.importPackage(json)
            if (book.articles.isNotEmpty()) {
                openArticle(book.id, 0)
            }
        }
    }

    /** 由界面按生命周期调用：进入/离开页面时切换计时。 */
    fun setSessionActive(active: Boolean) {
        isSessionActive = active
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
            }
            is EngineEvent.NextSentence -> {
                engine.currentSentence()?.let { playSentenceAudio(it) }
            }
            is EngineEvent.Complete -> {
                val article = currentArticle ?: return
                viewModelScope.launch {
                    statisticRepository.saveStatistic(
                        refType = "article",
                        refId = article.id,
                        startedAt = event.stats.startedAt,
                        spendMs = event.stats.spendMs,
                        total = event.stats.totalWords,
                        wrong = event.stats.wrongCount
                    )
                    progressRepository.clear(article.id)
                }
            }
            else -> {}
        }
    }

    private fun playSentenceAudio(sentence: Sentence) {
        val article = currentArticle ?: return
        audioService.playSentence(
            sentenceText = sentence.text,
            audioSrc = article.audioSrc,
            lrcStart = sentence.lrcStart,
            lrcEnd = sentence.lrcEnd
        )
    }

    private fun debounceSaveProgress(snapshot: EngineSnapshot) {
        val article = currentArticle ?: return
        if (snapshot.isEnd) return

        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(1500) // 1.5s 防抖落盘
            progressRepository.save(article.id, snapshot.cursor)
        }
    }

    /**
     * 将底层快照渲染为前端所需的一切分段高亮数据
     */
    private fun renderState(snapshot: EngineSnapshot) {
        val c = snapshot.cursor

        // 光标/空格/结束态未变（例如每秒 tick），只更新统计，不重建整篇 sections；
        // rawInput 必须纳入 key，否则打错字符（k 不变）时界面不会刷新。
        val renderKey = "$articleLoadToken|${c.sectionIdx}|${c.sentenceIdx}|${c.wordIdx}|${c.charIdx}|${snapshot.isSpace}|${snapshot.isEnd}|${snapshot.rawInput}"
        if (renderKey == lastRenderKey) {
            val cur = _uiState.value
            if (cur.stats != snapshot.stats) {
                _uiState.value = cur.copy(
                    cursor = c,
                    isSpace = snapshot.isSpace,
                    isEnd = snapshot.isEnd,
                    stats = snapshot.stats,
                )
            }
            return
        }
        lastRenderKey = renderKey

        var totalTokens = 0
        var completedTokens = 0

        val sectionRenders = parsedSections.mapIndexed { sIdx, sentences ->
            val sentenceRenders = sentences.mapIndexed { tIdx, sentence ->
                val isCurrentSentence = (sIdx == c.sectionIdx && tIdx == c.sentenceIdx)
                val isSentenceCompleted = (sIdx < c.sectionIdx) || (sIdx == c.sectionIdx && tIdx < c.sentenceIdx)

                val tokenRenders = sentence.tokens.mapIndexed { wIdx, token ->
                    totalTokens++
                    val isCurrentToken = isCurrentSentence && (wIdx == c.wordIdx)
                    val isTokenCompleted = isSentenceCompleted || (isCurrentSentence && wIdx < c.wordIdx)

                    if (isTokenCompleted) {
                        completedTokens++
                        TokenRender(
                            idx = token.idx,
                            text = token.text,
                            type = token.type,
                            nextSpace = token.nextSpace,
                            isWaitingSpace = false,
                            isCurrent = false,
                            isCompleted = true,
                            correctPrefix = token.text,
                            errorChar = null,
                            remainingText = ""
                        )
                    } else if (isCurrentToken) {
                        val correctPrefix = token.text.take(c.charIdx)
                        val remaining = token.text.drop(c.charIdx)
                        TokenRender(
                            idx = token.idx,
                            text = token.text,
                            type = token.type,
                            nextSpace = token.nextSpace,
                            isWaitingSpace = snapshot.isSpace,
                            isCurrent = true,
                            isCompleted = false,
                            correctPrefix = correctPrefix,
                            typedRaw = snapshot.rawInput,
                            errorChar = null,
                            remainingText = remaining
                        )
                    } else {
                        TokenRender(
                            idx = token.idx,
                            text = token.text,
                            type = token.type,
                            nextSpace = token.nextSpace,
                            isWaitingSpace = false,
                            isCurrent = false,
                            isCompleted = false,
                            correctPrefix = "",
                            errorChar = null,
                            remainingText = token.text
                        )
                    }
                }

                SentenceRender(
                    idx = sentence.idx,
                    text = sentence.text,
                    translate = sentence.translate,
                    isCurrent = isCurrentSentence,
                    isCompleted = isSentenceCompleted,
                    tokens = tokenRenders
                )
            }
            SectionRender(idx = sIdx, sentences = sentenceRenders)
        }

        val progressPercent = if (totalTokens > 0) {
            (completedTokens.toFloat() / totalTokens.toFloat()) * 100f
        } else 0f

        _uiState.value = _uiState.value.copy(
            sections = sectionRenders,
            cursor = c,
            isSpace = snapshot.isSpace,
            isEnd = snapshot.isEnd,
            stats = snapshot.stats,
            progressPercent = progressPercent
        )
    }

    override fun onCleared() {
        super.onCleared()
        // 页面退出时立即补写进度
        currentArticle?.let { article ->
            val cur = _uiState.value.cursor
            viewModelScope.launch {
                progressRepository.save(article.id, cur)
            }
        }
        audioService.stop()
        tickerJob?.cancel()
    }
}
