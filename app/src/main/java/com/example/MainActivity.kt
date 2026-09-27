package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.typingarticle.app.TypingApplication
import com.example.typingarticle.feature.practice.PracticeViewModel
import com.example.typingarticle.feature.settings.SettingsViewModel
import com.example.typingarticle.feature.word.WordDictationViewModel
import com.example.ui.AppMode
import com.example.ui.AppRoot
import com.example.ui.AppView
import com.example.ui.LibTab
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var practiceViewModel: PracticeViewModel
    private lateinit var wordDictationViewModel: WordDictationViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    // 界面级状态
    private var modeState by mutableStateOf(AppMode.ARTICLE)
    private var viewState by mutableStateOf(AppView.PRACTICE)
    private var sidebarState by mutableStateOf(true)
    private var darkState by mutableStateOf(false)
    private var libTabState by mutableStateOf(LibTab.BOOK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TypingApplication).container

        practiceViewModel = PracticeViewModel(
            contentRepository = container.contentRepository,
            progressRepository = container.progressRepository,
            statisticRepository = container.statisticRepository,
            settingsRepository = container.settingsRepository,
            audioService = container.audioService,
            wordMarkRepository = container.wordMarkRepository,
            keyMapper = container.keyMapper,
        )

        wordDictationViewModel = WordDictationViewModel(
            wordRepository = container.wordRepository,
            wordMarkRepository = container.wordMarkRepository,
            wrongWordRepository = container.wrongWordRepository,
            statisticRepository = container.statisticRepository,
            settingsRepository = container.settingsRepository,
            audioService = container.audioService,
            keyMapper = container.keyMapper,
        )

        settingsViewModel = SettingsViewModel(
            contentRepository = container.contentRepository,
            wordRepository = container.wordRepository,
            settingsRepository = container.settingsRepository,
            audioService = container.audioService,
        )

        practiceViewModel.openArticle("book_english_writing_2010_2025", 0)
        wordDictationViewModel.openBook(com.example.typingarticle.core.data.builtin.BuiltinContentProvider.DEFAULT_WORDBOOK_ID)

        setContent {
            MyApplicationTheme(darkTheme = darkState) {
                val practiceState by practiceViewModel.uiState.collectAsStateWithLifecycle()
                val wordState by wordDictationViewModel.uiState.collectAsStateWithLifecycle()

                AppRoot(
                    sidebarOpen = sidebarState,
                    darkTheme = darkState,
                    mode = modeState,
                    view = viewState,
                    libTab = libTabState,
                    practiceState = practiceState,
                    wordState = wordState,
                    practiceVm = practiceViewModel,
                    wordVm = wordDictationViewModel,
                    settingsVm = settingsViewModel,
                    onSetSidebar = { sidebarState = it },
                    onToggleTheme = { darkState = !darkState },
                    onMode = { modeState = it },
                    onView = { viewState = it },
                    onLibTab = { libTabState = it },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        practiceViewModel.setSessionActive(true)
        wordDictationViewModel.setSessionActive(true)
    }

    override fun onPause() {
        super.onPause()
        practiceViewModel.setSessionActive(false)
        wordDictationViewModel.setSessionActive(false)
    }

    /**
     * 用 dispatchKeyEvent 抢占输入：优先于 Compose 界面消费按键，
     * 避免聚焦的按钮抢走空格 / 方向键，保证跟打输入稳定。
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && handleTypingKey(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** 返回 true 表示已消费该按键 */
    private fun handleTypingKey(event: KeyEvent): Boolean {
        val isCtrl = event.isCtrlPressed
        val isAlt = event.isAltPressed
        val uc = event.unicodeChar
        val keyStr = if (uc != 0) uc.toChar().toString() else ""
        val codeStr = when (event.keyCode) {
            KeyEvent.KEYCODE_SPACE -> "Space"
            KeyEvent.KEYCODE_DEL -> "Backspace"
            KeyEvent.KEYCODE_FORWARD_DEL -> "Backspace"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "ArrowRight"
            KeyEvent.KEYCODE_DPAD_LEFT -> "ArrowLeft"
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "Enter"
            else -> KeyEvent.keyCodeToString(event.keyCode)
        }

        // 界面级快捷键（Ctrl 组合）
        if (isCtrl && keyStr.lowercase() == "b") {
            sidebarState = !sidebarState
            return true
        }
        if (isCtrl && keyStr.lowercase() == "j") {
            darkState = !darkState
            return true
        }

        val isPrintable = keyStr.length == 1 && uc >= 32
        val isCommand = codeStr == "Space" || codeStr == "Backspace" ||
            codeStr == "ArrowLeft" || codeStr == "ArrowRight" || codeStr == "Enter"

        if (!isPrintable && !isCommand) {
            return false // 让返回键、音量键、上下键等交给系统
        }

        if (modeState == AppMode.WORD) {
            wordDictationViewModel.onKey(keyStr, codeStr, isCtrl, isAlt)
        } else {
            practiceViewModel.onKey(keyStr, codeStr, isCtrl, isAlt)
        }
        return true
    }
}
