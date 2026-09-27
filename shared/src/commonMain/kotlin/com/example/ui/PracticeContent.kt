package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.typingarticle.core.data.repository.WrongWordStat
import com.example.typingarticle.feature.practice.PracticeUiState
import com.example.typingarticle.feature.practice.PracticeViewModel
import com.example.typingarticle.feature.practice.SentenceRender
import com.example.typingarticle.feature.practice.TokenRender
import com.example.typingarticle.feature.word.WordDictationViewModel
import com.example.typingarticle.feature.word.WordUiState
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.launch

private val mono = FontFamily.Monospace
private val BODY_FONT = 34.sp
private val BODY_LINE = 52.sp

fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun dots(n: Int) = "·".repeat(n.coerceAtLeast(0))

/** 闪动光标 */
@Composable
private fun Caret() {
    val c = AppTheme.colors
    val t = rememberInfiniteTransition(label = "caret")
    val a by t.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1050, easing = LinearEasing), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Box(Modifier.width(2.dp).height(30.dp).background(if (a > 0.4f) c.primary else Color.Transparent))
}

/** 顶栏小按钮 */
@Composable
private fun MiniBtn(text: String, active: Boolean = false, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) c.primary else c.surface)
            .border(1.dp, if (active) c.primary else c.line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = if (active) c.onPrimary else c.text,
            fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 13.sp,
        )
    }
}

/**
 * 词元渲染：
 * - 已完成：淡
 * - 当前：主色高亮框 + 已对前缀(主色) + 打错(红底) + 光标 + 剩余(正常色)
 * - 未打：正常色（非当前句略淡）
 */
@Composable
private fun TokenText(token: TokenRender, dictation: Boolean, inCurrentSentence: Boolean) {
    val c = AppTheme.colors

    // 非当前句：整体淡色
    if (!inCurrentSentence) {
        Text(
            text = if (dictation) dots(token.text.length) else token.text,
            color = c.dim,
            fontFamily = mono,
            fontSize = BODY_FONT, lineHeight = BODY_LINE,
        )
        return
    }

    // 当前句：深色
    if (token.isCompleted) {
        Text(token.text, color = c.text, fontFamily = mono, fontSize = BODY_FONT, lineHeight = BODY_LINE)
        return
    }

    if (token.isCurrent) {
        val correctLen = token.correctPrefix.length
        val wrong = token.typedRaw.drop(correctLen)
        val remaining = (token.text.length - correctLen).coerceAtLeast(0)
        Box(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(c.primary.copy(alpha = 0.14f))
                .border(1.dp, c.primary, RoundedCornerShape(6.dp))
                .padding(horizontal = 2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (token.correctPrefix.isNotEmpty()) {
                    Text(token.correctPrefix, color = c.primary, fontFamily = mono, fontSize = BODY_FONT, lineHeight = BODY_LINE)
                }
                if (wrong.isNotEmpty()) {
                    Text(
                        wrong,
                        color = c.bad,
                        fontFamily = mono,
                        fontSize = BODY_FONT, lineHeight = BODY_LINE,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(c.badBg)
                            .padding(horizontal = 1.dp),
                    )
                }
                Caret()
                if (remaining > 0) {
                    Text(
                        if (dictation) dots(remaining) else token.text.drop(correctLen),
                        color = c.text,
                        fontFamily = mono,
                        fontSize = BODY_FONT, lineHeight = BODY_LINE,
                    )
                }
                if (token.isWaitingSpace) {
                    Box(Modifier.padding(start = 5.dp).width(12.dp).height(3.dp).background(c.primary))
                }
            }
        }
        return
    }

    Text(
        text = if (dictation) dots(token.text.length) else token.text,
        color = c.text,
        fontFamily = mono,
        fontSize = BODY_FONT, lineHeight = BODY_LINE,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceRow(sen: SentenceRender, dictation: Boolean) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        sen.tokens.forEach { token ->
            Box(Modifier.padding(end = if (token.nextSpace) 11.dp else 0.dp, bottom = 12.dp)) {
                TokenText(token, dictation, sen.isCurrent)
            }
        }
    }
}

@Composable
private fun Rail(modifier: Modifier = Modifier, progress: Float) {
    val c = AppTheme.colors
    Box(modifier.height(4.dp).clip(CircleShape).background(c.hover)) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(CircleShape)
                .background(c.primary)
        )
    }
}

@Composable
private fun Dots(state: PracticeUiState) {
    val c = AppTheme.colors
    var flat = 0
    var currentFlat = -1
    var total = 0
    state.sections.forEachIndexed { si, sec ->
        sec.sentences.forEachIndexed { ti, _ ->
            if (si == state.cursor.sectionIdx && ti == state.cursor.sentenceIdx) currentFlat = flat
            total++
            flat++
        }
    }
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.size(9.dp).clip(CircleShape).background(
                    if (i <= currentFlat) c.primary else c.active
                )
            )
        }
    }
}

@Composable
fun ArticlePractice(state: PracticeUiState, vm: PracticeViewModel, sidebarOpen: Boolean) {
    val c = AppTheme.colors
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(start = 42.dp, end = 42.dp, top = 22.dp)) {
            // 顶栏：标题 + 进度 + 右侧控制
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.title, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 30.sp)
                    if (!state.titleTranslate.isNullOrEmpty()) {
                        Text("  " + state.titleTranslate, color = c.soft, fontFamily = mono, fontSize = 13.sp)
                    }
                }
                Rail(Modifier.weight(1f), state.progressPercent / 100f)
                Text(
                    "第 ${state.articleIdx + 1}/${state.totalArticles} · ${state.progressPercent.toInt()}% · ${formatMs(state.stats.spendMs)} · 错 ${state.stats.wrongCount}",
                    color = c.soft, fontFamily = mono, fontSize = 13.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBtn(if (state.translate) "译文 开" else "译文 关", active = state.translate) { vm.toggleTranslate() }
                    MiniBtn("←") { vm.prevSentence() }
                    MiniBtn("R") { vm.replay() }
                    MiniBtn("→") { vm.nextSentence() }
                }
            }

            Dots(state)

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                // 收起侧栏时，两边留白加倍（更居中、不贴边）
                val sideMargin = if (sidebarOpen) 96.dp else 192.dp
                val colW = if (maxWidth - sideMargin < 980.dp) maxWidth - sideMargin else 980.dp

                val scroll = rememberScrollState()
                val scope = rememberCoroutineScope()
                val viewportPx = with(LocalDensity.current) { maxHeight.toPx() }
                var centeredKey by remember { mutableStateOf("") }

                Column(
                    Modifier
                        .width(colW)
                        .verticalScroll(scroll)
                        .padding(top = 26.dp, bottom = 60.dp)
                ) {
                    state.sections.forEachIndexed { si, sec ->
                        sec.sentences.forEachIndexed { ti, sen ->
                            val isCur = sen.isCurrent
                            Box(
                                Modifier.then(
                                    if (isCur) Modifier.onGloballyPositioned { coords ->
                                        val key = "${state.articleId}-$si-$ti"
                                        if (centeredKey != key) {
                                            centeredKey = key
                                            val top = coords.positionInParent().y
                                            val h = coords.size.height.toFloat()
                                            scope.launch {
                                                scroll.animateScrollTo(
                                                    (top - (viewportPx - h) / 2f).coerceAtLeast(0f).toInt()
                                                )
                                            }
                                        }
                                    } else Modifier
                                )
                            ) {
                                if (isCur) {
                                    SentenceRow(sen, state.dictation)
                                } else {
                                    // 非当前句：整句一次渲染（只用基本类型参数，可跳过重组），大幅减少 composable 数量
                                    Text(
                                        text = if (state.dictation) dots(sen.text.length) else sen.text,
                                        color = c.dim,
                                        fontFamily = mono,
                                        fontSize = BODY_FONT, lineHeight = BODY_LINE,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                    )
                                }
                            }
                            if (state.translate && !sen.translate.isNullOrEmpty()) {
                                Text(
                                    sen.translate,
                                    color = if (sen.isCurrent) c.soft else c.dim,
                                    fontFamily = mono, fontSize = 19.sp, lineHeight = 30.sp,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                            }
                            // 句子之间留白
                            Spacer(Modifier.height(10.dp))
                        }
                        // 段落之间更大留白
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
        }

        if (state.isEnd) {
            Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.94f)), contentAlignment = Alignment.Center) {
                CompletionCard(state, vm)
            }
        }
    }
}

@Composable
private fun CompletionCard(state: PracticeUiState, vm: PracticeViewModel) {
    val c = AppTheme.colors
    CardSurface(Modifier.width(760.dp)) {
        Column(Modifier.padding(34.dp)) {
            Text("本篇完成", color = c.primary, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 31.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.title} · 共 ${state.sections.sumOf { it.sentences.size }} 句",
                color = c.soft, fontFamily = mono, fontSize = 14.sp,
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EuStatBox(formatMs(state.stats.spendMs), "用时", Modifier.weight(1f))
                EuStatBox("${state.stats.totalWords}", "词数", Modifier.weight(1f))
                EuStatBox("${state.stats.wrongCount}", "错误", Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EuPrimary("重练本篇") { vm.openArticle(state.bookId, state.articleIdx, restart = true) }
                EuGhost("下一篇 →") { vm.nextArticle() }
            }
        }
    }
}

@Composable
fun WordPractice(state: WordUiState, vm: WordDictationViewModel) {
    val c = AppTheme.colors
    var showWrong by remember { mutableStateOf(false) }
    val wrongStats by vm.wrongStats.collectAsState()
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(start = 42.dp, end = 42.dp, top = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(state.bookName, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 30.sp)
                Text("${state.currentIdx + 1}/${state.totalWords}", color = c.soft, fontFamily = mono, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBtn(if (state.showMeaning) "释义 开" else "释义 关", active = state.showMeaning) { vm.toggleMeaning() }
                    MiniBtn("重读") { vm.replayWord() }
                    MiniBtn("←") { vm.prevWord() }
                    MiniBtn("→") { vm.nextWord() }
                    MiniBtn("错词") { showWrong = true }
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                // 释义 / 音标 / 输入框整体上移，按可显示区高度的 15%
                val upShift = maxHeight * 0.15f
                Column(Modifier.offset(y = -upShift), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!state.isListeningMode && state.showMeaning && !state.trans.isNullOrEmpty()) {
                        Text(state.trans!!, color = c.soft, fontFamily = mono, fontSize = 20.sp)
                        Spacer(Modifier.height(10.dp))
                    }
                    if (state.showPhonetic && !state.phonetic.isNullOrEmpty()) {
                        Text(state.phonetic!!, color = c.soft, fontFamily = mono, fontSize = 16.sp)
                        Spacer(Modifier.height(20.dp))
                    }
                    state.tokenRender?.let { r ->
                        CardSurface {
                            Row(
                                Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (r.correctPrefix.isNotEmpty()) {
                                    Text(r.correctPrefix, color = c.primary, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 54.sp)
                                }
                                val wrong = r.typedRaw.drop(r.correctPrefix.length)
                                if (wrong.isNotEmpty()) {
                                    Text(wrong, color = c.bad, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 54.sp)
                                }
                                Caret()
                                if (r.remainingText.isNotEmpty()) {
                                    Text(dots(r.remainingText.length), color = c.soft, fontFamily = mono, fontSize = 40.sp, lineHeight = 54.sp)
                                }
                            }
                        }
                    }
                    if (state.isWrongOnlyMode) {
                        Spacer(Modifier.height(14.dp))
                        Text("错词重练中", color = c.bad, fontFamily = mono, fontSize = 13.sp)
                    }
                }
            }
        }

        if (state.isEnd) {
            Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.94f)), contentAlignment = Alignment.Center) {
                CardSurface(Modifier.width(600.dp)) {
                    Column(
                        Modifier.padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🎉", fontSize = 44.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "恭喜，本词库已默写完成！",
                            color = c.primary, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${state.bookName} · 共 ${state.totalWords} 词 · 用时 ${formatMs(state.stats.spendMs)} · 错 ${state.stats.wrongCount}",
                            color = c.soft, fontFamily = mono, fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            EuPrimary("再来一遍") { vm.restartBook() }
                            EuGhost("看错词") { showWrong = true }
                        }
                    }
                }
            }
        }

        if (showWrong) {
            WrongStatsDialog(
                stats = wrongStats,
                onCreate = {
                    val name = "错词 · " + java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    scope.launch {
                        vm.createBookFromWrongStats(name)
                        showWrong = false
                    }
                },
                onClear = { vm.clearWrongStats() },
                onDismiss = { showWrong = false },
            )
        }
    }
}

@Composable
private fun WrongStatsDialog(
    stats: List<WrongWordStat>,
    onCreate: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AppTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        CardSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(26.dp)) {
                Text("错词统计", color = c.primary, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "后台共记录 ${stats.size} 个错词（同词只存一次，重复出错累加次数）",
                    color = c.soft, fontFamily = mono, fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                if (stats.isEmpty()) {
                    Text("暂无错词记录 🎉", color = c.soft, fontFamily = mono, fontSize = 15.sp)
                } else {
                    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                        stats.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(s.word, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.weight(1f))
                                Text("×${s.count}", color = c.bad, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    EuPrimary("制作成新词库") { onCreate() }
                    EuGhost("清空统计") { onClear() }
                    Spacer(Modifier.weight(1f))
                    EuGhost("忽略") { onDismiss() }
                }
            }
        }
    }
}

