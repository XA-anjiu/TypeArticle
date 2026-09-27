package com.example.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.typingarticle.feature.practice.PracticeViewModel
import com.example.typingarticle.feature.settings.SettingsViewModel
import com.example.typingarticle.feature.word.WordDictationViewModel
import com.example.ui.AppMode
import com.example.ui.AppRoot
import com.example.ui.AppView
import com.example.ui.LibTab
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window as AwtWindow
import java.io.File

private const val MIN_WINDOW_W = 960
private const val MIN_WINDOW_H = 640

private enum class ResizeEdge { TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

fun main() = application {
    val appDir = remember { File(System.getProperty("user.home"), ".typearticle") }
    val container = remember { DesktopContainer(appDir) }

    val practiceVm = remember {
        PracticeViewModel(
            contentRepository = container.contentRepository,
            progressRepository = container.progressRepository,
            statisticRepository = container.statisticRepository,
            settingsRepository = container.settingsRepository,
            audioService = container.audioService,
            wordMarkRepository = container.wordMarkRepository,
            keyMapper = container.keyMapper,
        )
    }
    val wordVm = remember {
        WordDictationViewModel(
            wordRepository = container.wordRepository,
            wordMarkRepository = container.wordMarkRepository,
            wrongWordRepository = container.wrongWordRepository,
            statisticRepository = container.statisticRepository,
            settingsRepository = container.settingsRepository,
            audioService = container.audioService,
            keyMapper = container.keyMapper,
        )
    }
    val settingsVm = remember {
        SettingsViewModel(
            contentRepository = container.contentRepository,
            wordRepository = container.wordRepository,
            settingsRepository = container.settingsRepository,
            audioService = container.audioService,
        )
    }

    LaunchedEffect(Unit) {
        DesktopBuiltinContent.populateIfEmpty(appDir, container.contentRepository, container.wordRepository)
        practiceVm.openArticle(DesktopBuiltinContent.BUILTIN_BOOK_ID, 0)
        wordVm.openBook(DesktopBuiltinContent.DEFAULT_WORDBOOK_ID)
    }

    var mode by remember { mutableStateOf(AppMode.ARTICLE) }
    var view by remember { mutableStateOf(AppView.PRACTICE) }
    var sidebar by remember { mutableStateOf(true) }
    var dark by remember { mutableStateOf(false) }
    var libTab by remember { mutableStateOf(LibTab.BOOK) }

    val appIcon = remember {
        runCatching {
            val stream = object {}.javaClass.getResourceAsStream("/icon.png") ?: return@runCatching null
            BitmapPainter(loadImageBitmap(stream))
        }.getOrNull()
    }

    val windowState = rememberWindowState(width = 1320.dp, height = 880.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "TypeArticle",
        icon = appIcon,
        undecorated = true,
        transparent = true,
        state = windowState,
        onPreviewKeyEvent = { e ->
            if (e.type != KeyEventType.KeyDown) {
                false
            } else {
                handleKey(
                    e = e,
                    mode = mode,
                    onToggleSidebar = { sidebar = !sidebar },
                    onToggleTheme = { dark = !dark },
                    onKeyPractice = { k, c, ctrl, alt -> practiceVm.onKey(k, c, ctrl, alt) },
                    onKeyWord = { k, c, ctrl, alt -> wordVm.onKey(k, c, ctrl, alt) },
                )
            }
        },
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(MIN_WINDOW_W, MIN_WINDOW_H) }

        val toggleMaximize = {
            windowState.placement =
                if (windowState.placement == WindowPlacement.Maximized) WindowPlacement.Floating
                else WindowPlacement.Maximized
            Unit
        }

        MyApplicationTheme(darkTheme = dark) {
            val c = AppTheme.colors
            val radius = if (windowState.placement == WindowPlacement.Maximized) 0.dp else 12.dp
            val shape = RoundedCornerShape(radius)
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(c.bg)
                        .then(if (radius > 0.dp) Modifier.border(1.dp, c.line, shape) else Modifier)
                ) {
                Column(Modifier.fillMaxSize()) {
                    WindowBar(
                        dragModifier = Modifier.pointerInput(Unit) {
                            var grabDx = 0
                            var grabDy = 0
                            detectDragGestures(
                                onDragStart = { _ ->
                                    val sp = MouseInfo.getPointerInfo()?.location
                                    if (sp != null) {
                                        grabDx = sp.x - window.x
                                        grabDy = sp.y - window.y
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (windowState.placement != WindowPlacement.Maximized) {
                                        val sp = MouseInfo.getPointerInfo()?.location
                                        if (sp != null) {
                                            val screen = Toolkit.getDefaultToolkit().screenSize
                                            val nx = (sp.x - grabDx).coerceIn(-(window.width - 120), screen.width - 120)
                                            val ny = (sp.y - grabDy).coerceIn(0, screen.height - 60)
                                            window.location = Point(nx, ny)
                                        }
                                    }
                                },
                            )
                        },
                        onDoubleClick = toggleMaximize,
                        onMinimize = { windowState.isMinimized = true },
                        onToggleMaximize = toggleMaximize,
                        onClose = { exitApplication() },
                    )

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val ps by practiceVm.uiState.collectAsState()
                        val ws by wordVm.uiState.collectAsState()
                        AppRoot(
                            logo = appIcon,
                            sidebarOpen = sidebar,
                            darkTheme = dark,
                            mode = mode,
                            view = view,
                            libTab = libTab,
                            practiceState = ps,
                            wordState = ws,
                            practiceVm = practiceVm,
                            wordVm = wordVm,
                            settingsVm = settingsVm,
                            onSetSidebar = { sidebar = it },
                            onToggleTheme = { dark = !dark },
                            onMode = { mode = it },
                            onView = { view = it },
                            onLibTab = { libTab = it },
                            onImportArticles = { pickTextFile(window, "导入文章 JSON（Article[]）")?.let { settingsVm.importArticles(it) } },
                            onImportWords = { pickTextFile(window, "导入词库（.txt / .json）")?.let { settingsVm.importWords(it, "导入词库") } },
                        )
                    }
                }
                }

                ResizeOverlay(window = window, windowState = windowState)
            }
        }
    }
}

@Composable
private fun WindowBar(
    dragModifier: Modifier,
    onDoubleClick: () -> Unit,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(30.dp).background(c.bg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(dragModifier)
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleClick() }) }
        )
        BarButton("—", onMinimize)
        BarButton("▢", onToggleMaximize)
        BarButton("✕", onClose, danger = true)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun BarButton(glyph: String, onClick: () -> Unit, danger: Boolean = false) {
    val c = AppTheme.colors
    Box(
        Modifier.size(width = 34.dp, height = 22.dp).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = if (danger) c.bad else c.soft, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
private fun ResizeOverlay(window: AwtWindow, windowState: WindowState) {
    Box(Modifier.fillMaxSize()) {
        ResizeHandle(ResizeEdge.TOP, Modifier.align(Alignment.TopCenter).fillMaxWidth().height(5.dp), window, windowState)
        ResizeHandle(ResizeEdge.BOTTOM, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(5.dp), window, windowState)
        ResizeHandle(ResizeEdge.LEFT, Modifier.align(Alignment.CenterStart).fillMaxHeight().width(5.dp), window, windowState)
        ResizeHandle(ResizeEdge.RIGHT, Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(5.dp), window, windowState)
        ResizeHandle(ResizeEdge.TOP_LEFT, Modifier.align(Alignment.TopStart).size(8.dp), window, windowState)
        ResizeHandle(ResizeEdge.TOP_RIGHT, Modifier.align(Alignment.TopEnd).size(8.dp), window, windowState)
        ResizeHandle(ResizeEdge.BOTTOM_LEFT, Modifier.align(Alignment.BottomStart).size(8.dp), window, windowState)
        ResizeHandle(ResizeEdge.BOTTOM_RIGHT, Modifier.align(Alignment.BottomEnd).size(8.dp), window, windowState)
    }
}

@Composable
private fun ResizeHandle(edge: ResizeEdge, modifier: Modifier, window: AwtWindow, windowState: WindowState) {
    Box(
        modifier.pointerInput(edge) {
            detectDragGestures { change, drag ->
                change.consume()
                if (windowState.placement == WindowPlacement.Maximized) return@detectDragGestures

                var x = window.x
                var y = window.y
                var w = window.width
                var h = window.height
                val dx = drag.x.toInt()
                val dy = drag.y.toInt()

                when (edge) {
                    ResizeEdge.LEFT, ResizeEdge.TOP_LEFT, ResizeEdge.BOTTOM_LEFT -> { x += dx; w -= dx }
                    ResizeEdge.RIGHT, ResizeEdge.TOP_RIGHT, ResizeEdge.BOTTOM_RIGHT -> { w += dx }
                    else -> {}
                }
                when (edge) {
                    ResizeEdge.TOP, ResizeEdge.TOP_LEFT, ResizeEdge.TOP_RIGHT -> { y += dy; h -= dy }
                    ResizeEdge.BOTTOM, ResizeEdge.BOTTOM_LEFT, ResizeEdge.BOTTOM_RIGHT -> { h += dy }
                    else -> {}
                }

                if (w < MIN_WINDOW_W) {
                    if (edge == ResizeEdge.LEFT || edge == ResizeEdge.TOP_LEFT || edge == ResizeEdge.BOTTOM_LEFT) {
                        x -= (MIN_WINDOW_W - w)
                    }
                    w = MIN_WINDOW_W
                }
                if (h < MIN_WINDOW_H) {
                    if (edge == ResizeEdge.TOP || edge == ResizeEdge.TOP_LEFT || edge == ResizeEdge.TOP_RIGHT) {
                        y -= (MIN_WINDOW_H - h)
                    }
                    h = MIN_WINDOW_H
                }

                window.setBounds(x, y, w, h)
            }
        }
    )
}

private fun handleKey(
    e: KeyEvent,
    mode: AppMode,
    onToggleSidebar: () -> Unit,
    onToggleTheme: () -> Unit,
    onKeyPractice: (String, String, Boolean, Boolean) -> Unit,
    onKeyWord: (String, String, Boolean, Boolean) -> Unit,
): Boolean {
    val cp = e.utf16CodePoint
    val keyStr = if (cp in 32..0xFFFF) cp.toChar().toString() else ""
    val codeStr = when (e.key) {
        Key.Spacebar -> "Space"
        Key.Backspace -> "Backspace"
        Key.DirectionLeft -> "ArrowLeft"
        Key.DirectionRight -> "ArrowRight"
        Key.Enter, Key.NumPadEnter -> "Enter"
        else -> e.key.toString()
    }
    val isCtrl = e.isCtrlPressed
    val isAlt = e.isAltPressed

    if (isCtrl && keyStr.equals("b", ignoreCase = true)) { onToggleSidebar(); return true }
    if (isCtrl && keyStr.equals("j", ignoreCase = true)) { onToggleTheme(); return true }

    val isPrintable = keyStr.length == 1 && cp >= 32
    val isCommand = codeStr == "Space" || codeStr == "Backspace" ||
        codeStr == "ArrowLeft" || codeStr == "ArrowRight" || codeStr == "Enter"
    if (!isPrintable && !isCommand) return false

    if (mode == AppMode.WORD) onKeyWord(keyStr, codeStr, isCtrl, isAlt)
    else onKeyPractice(keyStr, codeStr, isCtrl, isAlt)
    return true
}

private fun pickTextFile(owner: Frame, title: String): String? {
    return try {
        val dialog = FileDialog(owner, title, FileDialog.LOAD)
        dialog.isMultipleMode = false
        dialog.isVisible = true
        val file = dialog.files.firstOrNull() ?: return null
        file.readText(Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }
}
