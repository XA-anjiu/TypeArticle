package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.typingarticle.core.audio.KeyboardSounds
import com.example.typingarticle.core.model.Settings
import com.example.typingarticle.feature.settings.SettingsViewModel
import com.example.ui.theme.AppTheme

private val mono = FontFamily.Monospace

@Composable
fun SettingsContent(
    libTab: LibTab,
    settingsVm: SettingsViewModel,
    onLibTab: (LibTab) -> Unit,
    onBack: () -> Unit,
    onImportArticles: () -> Unit = {},
    onImportWords: () -> Unit = {},
) {
    val c = AppTheme.colors
    val books by settingsVm.books.collectAsState()
    val wordBooks by settingsVm.wordBooks.collectAsState()
    val settings by settingsVm.settings.collectAsState()
    var showPrompt by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 42.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("设置", color = c.primary, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 31.sp)
                Text("导入 / 管理你的书库与词库 · 全部保存在本机", color = c.soft, fontFamily = mono, fontSize = 14.sp)
            }
            Spacer(Modifier.weight(1f))
            EuGhost("← 回到跟打") { onBack() }
        }

        EuSegmented(listOf("书库 · 文章", "词库 · 单词"), selected = libTab.ordinal) {
            onLibTab(if (it == 0) LibTab.BOOK else LibTab.WORD)
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (libTab == LibTab.BOOK) "共 ${books.size} 个书库 · ${books.sumOf { it.length }} 篇"
                        else "共 ${wordBooks.size} 个词库",
                        color = c.soft, fontFamily = mono, fontSize = 13.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    if (libTab == LibTab.BOOK) {
                        EuPrimary("＋ 导入文章（.json）") { onImportArticles() }
                    } else {
                        EuPrimary("＋ 导入单词（.txt / .json）") { onImportWords() }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    EuChip("文章 · AI 提示词", active = showPrompt == "article") {
                        showPrompt = if (showPrompt == "article") null else "article"
                    }
                    EuChip("单词 · AI 提示词", active = showPrompt == "word") {
                        showPrompt = if (showPrompt == "word") null else "word"
                    }
                }
            }
            if (showPrompt == "article") {
                item { PromptCard("文章：AI 转换提示词", "把这段提示词连同你的文章一起发给任意 AI，让它输出可导入的 JSON。", ARTICLE_PROMPT) }
            }
            if (showPrompt == "word") {
                item { PromptCard("单词：AI 转换提示词", "把这段提示词连同你的词表一起发给任意 AI，让它输出可导入的格式。", WORD_PROMPT) }
            }

            if (libTab == LibTab.BOOK) {
                items(books, key = { it.id }) { b ->
                    CardSurface(Modifier.fillMaxWidth(), radius = com.example.ui.theme.Radius.btn) {
                        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(b.name, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${b.length} 篇 · ${if (b.source == "builtin") "内置" else "导入"}", color = c.soft, fontFamily = mono, fontSize = 12.sp)
                            }
                            EuDanger("删除") { settingsVm.deleteBook(b.id) }
                        }
                    }
                }
            } else {
                items(wordBooks, key = { it.id }) { b ->
                    CardSurface(Modifier.fillMaxWidth(), radius = com.example.ui.theme.Radius.btn) {
                        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(b.name, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(if (b.source == "builtin") "内置" else "导入", color = c.soft, fontFamily = mono, fontSize = 12.sp)
                            }
                            EuDanger("删除") { settingsVm.deleteWordBook(b.id) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
            item { ShortcutsCard() }
            item { SoundCard(settings, settingsVm) }
        }
    }
}

private val ARTICLE_PROMPT = """
请把我提供的文章转换成「TypeArticle」App 可导入的 JSON（顶层是数组，每个元素一篇文章）。
严格按以下规则输出：只输出 JSON，不要任何解释，也不要用 Markdown 代码块包裹。

字段：
- title          文章标题（字符串，必填）
- titleTranslate 标题中文（字符串，可选）
- text           正文（字符串，必填）
- textTranslate  正文中文（字符串，可选，需与 text 逐句逐段对齐）

text 与 textTranslate 的结构规则：
- 段落之间用空行分隔，即两个换行 \n\n
- 段落内部一句一行，即每句末尾一个换行 \n
- textTranslate 与 text 的分段数、句数必须 1:1 对应（同样 \n 分句、\n\n 分段），没有译文可省略该字段
- 标点保持原文；引号统一为半角 ' 和 "

示例：
[
  {
    "title": "感谢信",
    "titleTranslate": "感谢信",
    "text": "Dear Joe,\nI would like to extend my heartfelt thanks.\n\nYours sincerely,\nLi Ming",
    "textTranslate": "亲爱的乔：\n我想向您表达衷心的感谢。\n\n此致\n李明"
  }
]
""".trimIndent()

private val WORD_PROMPT = """
请把我提供的单词 / 词表转换成「TypeArticle」App 可导入的格式。
只输出内容本身，不要任何解释。

三种可用格式（优先 JSON）：
1. .json（推荐，可带音标与释义）：顶层数组，元素形如
   {"word": "单词", "phonetic0": "/美式音标/", "trans": "词性+中文释义"}
2. .txt：每行一个单词（无音标、无释义）
3. .csv：每行「单词,音标,音标2,释义」

规则：
- 同一单词只保留一条，按小写去重
- 单词保持原形与大小写（如 PhD、WeChat、eco-friendly）
- 音标用美式、用斜杠包裹；释义带词性前缀（n. / v. / a. / ad.），一词多义用中文分号隔开

示例（.json）：
[
  {"word": "apple", "phonetic0": "/ˈæpl/", "trans": "n. 苹果"},
  {"word": "banana", "phonetic0": "/bəˈnɑːnə/", "trans": "n. 香蕉"}
]
""".trimIndent()

@Composable
private fun PromptCard(title: String, hint: String, prompt: String) {
    val c = AppTheme.colors
    val clipboard = LocalClipboardManager.current
    CardSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                EuGhost("一键复制") { clipboard.setText(AnnotatedString(prompt)) }
            }
            Spacer(Modifier.height(6.dp))
            Text(hint, color = c.soft, fontFamily = mono, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.bg)
                    .border(1.dp, c.line, RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(prompt, color = c.text, fontFamily = mono, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun ShortcutsCard() {
    val c = AppTheme.colors
    var recording by remember { mutableStateOf<String?>(null) }
    val rows = listOf(
        "下一句 / 下一词" to "回车 或 →",
        "上一句 / 上一词" to "Alt + 空格 或 ←",
        "重播当前句 / 词" to "R",
        "单词确认 / 进下一词" to "空格",
        "开启 / 关闭译文" to "Ctrl + T",
        "默写 / 跟打 切换" to "Ctrl + D",
        "收起 / 展开侧栏" to "Ctrl + B",
        "深色 / 浅色 切换" to "Ctrl + J",
    )
    CardSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("快捷键", color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.weight(1f))
                EuGhost("恢复默认") { recording = null }
            }
            Spacer(Modifier.height(6.dp))
            rows.forEach { (label, key) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    EuKeycap(
                        text = if (recording == key) "按键…" else key,
                        recording = recording == key,
                    ) { recording = if (recording == key) null else key }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundCard(settings: Settings, vm: SettingsViewModel) {
    val c = AppTheme.colors
    CardSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 20.dp)) {
            Text("音效", color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EuToggleRow("按键音", settings.keySoundEnabled, Modifier.weight(1f)) { vm.setKeySoundEnabled(it) }
                EuToggleRow("出错提示音", settings.errorSoundEnabled, Modifier.weight(1f)) { vm.setErrorSoundEnabled(it) }
            }
            Spacer(Modifier.height(14.dp))
            Text("键盘音效（点击切换并试听）", color = c.soft, fontFamily = mono, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyboardSounds.ALL.forEach { name ->
                    Box(Modifier.padding(bottom = 8.dp)) {
                        EuChip(
                            text = name,
                            active = settings.keyboardSound == name,
                            onClick = { vm.setKeyboardSound(name) },
                        )
                    }
                }
            }
        }
    }
}
