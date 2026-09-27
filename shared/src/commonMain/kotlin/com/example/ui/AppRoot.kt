package com.example.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.typingarticle.feature.practice.PracticeUiState
import com.example.typingarticle.feature.practice.PracticeViewModel
import com.example.typingarticle.feature.settings.SettingsViewModel
import com.example.typingarticle.feature.settings.WordBookItemUi
import com.example.typingarticle.feature.word.WordDictationViewModel
import com.example.typingarticle.feature.word.WordUiState
import com.example.ui.theme.AppTheme
import com.example.ui.theme.Radius

private val mono = FontFamily.Monospace

enum class AppMode { ARTICLE, WORD }
enum class AppView { PRACTICE, SETTINGS }
enum class LibTab { BOOK, WORD }

@Composable
fun AppRoot(
    sidebarOpen: Boolean,
    darkTheme: Boolean,
    mode: AppMode,
    view: AppView,
    libTab: LibTab,
    practiceState: PracticeUiState,
    wordState: WordUiState,
    practiceVm: PracticeViewModel,
    wordVm: WordDictationViewModel,
    settingsVm: SettingsViewModel,
    onSetSidebar: (Boolean) -> Unit,
    onToggleTheme: () -> Unit,
    onMode: (AppMode) -> Unit,
    onView: (AppView) -> Unit,
    onLibTab: (LibTab) -> Unit,
    onImportArticles: () -> Unit = {},
    onImportWords: () -> Unit = {},
    logo: Painter? = null,
) {
    val c = AppTheme.colors
    val wordBooks by settingsVm.wordBooks.collectAsState()
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            val sideWidth by animateDpAsState(if (sidebarOpen) 248.dp else 0.dp, label = "sideWidth")
            Box(Modifier.width(sideWidth).fillMaxHeight().clip(RoundedCornerShape(0.dp))) {
                if (sidebarOpen) {
                    Sidebar(
                        logo = logo,
                        mode = mode,
                        darkTheme = darkTheme,
                        practiceState = practiceState,
                        wordBooks = wordBooks,
                        activeWordBookId = wordState.bookId,
                        onCollapse = { onSetSidebar(false) },
                        onOpenArticle = { idx -> practiceVm.openArticle(practiceState.bookId, idx); onMode(AppMode.ARTICLE) },
                        onMode = onMode,
                        onToggleDictation = { practiceVm.toggleDictation() },
                        onToggleTheme = onToggleTheme,
                        onOpenSettings = { onView(AppView.SETTINGS) },
                        onOpenWordBook = { id ->
                            wordVm.openBook(id)
                            onMode(AppMode.WORD)
                            onView(AppView.PRACTICE)
                        },
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (view) {
                    AppView.SETTINGS -> SettingsContent(
                        libTab = libTab,
                        settingsVm = settingsVm,
                        onLibTab = onLibTab,
                        onBack = { onView(AppView.PRACTICE) },
                        onImportArticles = onImportArticles,
                        onImportWords = onImportWords,
                    )
                    AppView.PRACTICE -> if (mode == AppMode.ARTICLE) {
                        ArticlePractice(practiceState, practiceVm, sidebarOpen)
                    } else {
                        WordPractice(wordState, wordVm)
                    }
                }

                if (!sidebarOpen) {
                    EdgeTab(Modifier.align(Alignment.CenterStart)) { onSetSidebar(true) }
                }
            }
        }
    }
}

@Composable
private fun Brand(logo: Painter?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (logo != null) {
            Image(
                painter = logo,
                contentDescription = null,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(AppTheme.colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text("TY", color = AppTheme.colors.onPrimary, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Text("TypeArticle", color = AppTheme.colors.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    }
}

@Composable
private fun Sidebar(
    logo: Painter?,
    mode: AppMode,
    darkTheme: Boolean,
    practiceState: PracticeUiState,
    wordBooks: List<WordBookItemUi>,
    activeWordBookId: String,
    onCollapse: () -> Unit,
    onOpenArticle: (Int) -> Unit,
    onMode: (AppMode) -> Unit,
    onToggleDictation: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWordBook: (String) -> Unit,
) {
    val c = AppTheme.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Brand(logo)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(c.surface).clickable { onCollapse() },
                contentAlignment = Alignment.Center
            ) { Text("‹", color = c.soft, fontFamily = mono, fontSize = 16.sp) }
        }

        if (mode == AppMode.ARTICLE) {
            EuSectionLabel("文章")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(practiceState.articleList, key = { it.idx }) { item ->
                    val active = item.idx == practiceState.articleIdx
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.btn))
                            .background(if (active) c.primary else c.surface)
                            .clickable { onOpenArticle(item.idx) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(item.title, color = if (active) c.onPrimary else c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (!item.titleTranslate.isNullOrEmpty()) {
                            Text(item.titleTranslate, color = if (active) c.onPrimary.copy(alpha = 0.85f) else c.soft, fontFamily = mono, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            EuSectionLabel("词库")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(wordBooks, key = { it.id }) { book ->
                    val active = book.id == activeWordBookId
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.btn))
                            .background(if (active) c.primary else c.surface)
                            .clickable { onOpenWordBook(book.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(book.name, color = if (active) c.onPrimary else c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(if (book.source == "builtin") "内置" else "导入", color = if (active) c.onPrimary.copy(alpha = 0.85f) else c.soft, fontFamily = mono, fontSize = 12.sp)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeSwitch(mode, onMode)
            EuToggleRow("默写", checked = practiceState.dictation, modifier = Modifier.fillMaxWidth()) { onToggleDictation() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EuChip("设置", modifier = Modifier.weight(1f), onClick = onOpenSettings)
                EuChip(if (darkTheme) "浅色" else "夜间", modifier = Modifier.weight(1f), onClick = onToggleTheme)
            }
        }
    }
}

@Composable
private fun ModeSwitch(mode: AppMode, onMode: (AppMode) -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(Radius.btn)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.line, shape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("文章" to AppMode.ARTICLE, "单词" to AppMode.WORD).forEach { (label, target) ->
            val active = mode == target
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (active) c.primary else Color.Transparent)
                    .clickable { onMode(target) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (active) c.onPrimary else c.soft, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun EdgeTab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier.width(26.dp).height(70.dp)
            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
            .background(c.surface).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text("›", color = c.soft, fontFamily = mono, fontSize = 16.sp) }
}
