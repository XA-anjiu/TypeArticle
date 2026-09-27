# 平板文章跟打与单词默写 App · TypeArticle

纯本地、离线优先的 Android 平板学习 App（Kotlin + Jetpack Compose）。核心是**文章跟打**与**单词默写**，面向华为 MatePad Pro 10.8（2021，HarmonyOS 2/3 / 无 GMS）+ 蓝牙实体键盘。

本文档是架构说明与前端对接指南。

---

## 1. 架构总览与目录结构

所有业务内核（数据层、解析层、打字状态机引擎、音频服务、依赖注入）与 UI 解耦；`core/parser`、`core/engine` 为纯 Kotlin，禁止依赖 `android.*`，可在 JVM 上直接单测。

```
com.example.typingarticle
├─ core
│  ├─ model         数据模型（Book, Article, Sentence, Token, Cursor, PracticeStats, WordStats, Settings）
│  ├─ parser        ArticleParser（引号归一、切段切句、词元化、nextSpace）
│  ├─ engine        TypingEngine（状态机）+ KeyMapper（实体键盘映射）
│  ├─ data          Room（Entity/Dao/Database）+ Repository + builtin 内置内容
│  ├─ audio         AudioService（系统 TTS + ExoPlayer 区间播放 + SoundPool/ToneGenerator）
│  └─ settings      SettingsRepository（DataStore Preferences）
├─ feature
│  ├─ practice      PracticeViewModel + PracticeUiState（文章跟打契约，无 Compose 依赖）
│  └─ word          WordDictationViewModel + WordUiState（单词默写契约，无 Compose 依赖）
├─ di               TypingDiContainer（模块解耦与单例管理）
└─ app              TypingApplication + MainActivity（极简真机烟测验证壳，后续由正式前端替换）
```

---

## 2. 关键规范与特性

1. **无 GMS / 纯本地离线优先**：无 Firebase / Google Play Services / Secrets 等任何 Google 依赖，适配华为无 GMS 环境。
2. **纯实体键盘**：输入层只做实体键盘映射；`isSpace` 等待态收到非空格键会**推进 + 重放该字符**，不吞键。软键盘/IME 不做。
3. **四层游标回退**：Backspace 支持跨字符 / 跨词 / 跨句 / 跨段精准逆序回落。
4. **引擎 100% 复用**：文章题与单词题共用同一 Tokenizer 与判定逻辑；单词作为“单 Token 且 nextSpace=false”的题，打完自动下一题，无需空格。
5. **断点续练**：游标变更 1.5s 防抖落盘，退出页面补写。
6. **计时准确**：仅当页面处于活动状态（`setSessionActive(true)`）时累计学习时间。
7. **过滤规则**：`ignoreSymbol`（跳过数字/符号）、`nameList`（人名跳过）、`ignoreSimpleWord`（简单词）、已掌握词自动跳过；学习词数只统计未被过滤的 Word。
8. **内置内容来自真实内容包**：见第 4 节。

---

## 3. 前端对接指南（消费 UI State 契约）

前端只需通过 ViewModel 消费 UI State 与调用公开方法，无需修改内核。

### 3.1 文章跟打（`PracticeViewModel`）

```kotlin
val uiState by practiceViewModel.uiState.collectAsStateWithLifecycle()

uiState.sections.forEach { section ->
    section.sentences.forEach { sentence ->
        sentence.tokens.forEach { token ->
            // token.isCurrent / isCompleted / isWaitingSpace / nextSpace
            // token.correctPrefix : 已连续正确的字符
            // token.typedRaw      : 当前词原始输入（含打错字符）→ 与 text 逐位比较可渲染“打错标红”
            // token.remainingText : 尚未输入的字符
        }
        if (uiState.translate && sentence.translate != null) Text(sentence.translate)
    }
}

practiceViewModel.onKey(keyStr, codeStr, isCtrl)
practiceViewModel.nextSentence(); practiceViewModel.prevSentence(); practiceViewModel.replay()
practiceViewModel.toggleDictation(); practiceViewModel.toggleTranslate()
practiceViewModel.setSessionActive(true/false) // 生命周期：进入/离开练习页时调用
```

**渲染错误字符**：对当前词，用 `token.typedRaw` 与 `token.text` 逐位对比——
前缀相同部分=已正确，出现分歧的位置即打错字符（`typedRaw` 比 `correctPrefix` 更长的部分），可标红。

### 3.2 单词默写（`WordDictationViewModel`）

```kotlin
val wordState by wordDictationViewModel.uiState.collectAsStateWithLifecycle()

Text(wordState.trans ?: "")          // 释义
Text(wordState.phonetic ?: "")       // 音标
val r = wordState.tokenRender        // r.correctPrefix / r.typedRaw / r.remainingText

wordDictationViewModel.onKey(key, code)
wordDictationViewModel.replayWord(); wordDictationViewModel.replayWrongOnly()
wordDictationViewModel.setSessionActive(true/false)
```

---

## 4. 内容包（内置文章 / 词库）

### 4.1 内置文章（真实内容，来自 assets）
内置书《英语二写作真题范文 2010–2025》（32 篇）来自：

```
app/src/main/assets/articles_2010_2025.json
```

首次启动时由 `BuiltinContentProvider.populateIfEmpty()` 读取并写入本地库（书籍 id：`book_english_writing_2010_2025`）。

- 格式：`Article[]` JSON 数组，字段 `title / titleTranslate / text / textTranslate / audioSrc? / lrcPosition? / nameList?`。
- `text`：段落间空行（`\n\n`），段内一句一行（`\n`）；`textTranslate` 与之逐句逐段对齐。
- **替换内容**：直接改这个 assets 文件即可，无需改代码（清空应用数据后重启生效）。

运行时导入用户文章：`ContentRepository.importPackage(json)`（顶层 `Article[]`）。

### 4.2 内置词库
`BuiltinContentProvider.getBuiltinWords()` 内置一份示例高频词（30 词）。可在设置页导入自己的词表，或在代码中替换。
导入接口：`WordRepository.importWords(content, bookName, format)`，支持 `.txt`（一行一词）/ `.json`（`string[]` 或 `{word, phonetic0, phonetic1, trans}[]`）/ 逗号制表格。

---

## 5. 构建与签名

```bash
# Windows
gradlew.bat assembleDebug
gradlew.bat test            # 运行核心单元测试

# macOS / Linux
./gradlew assembleDebug
./gradlew test
```

- **debug**：使用 AGP 默认调试签名，无需任何额外文件。
- **release（可选签名）**：若提供 keystore 才启用签名；否则产出未签名包。通过环境变量提供：
  `KEYSTORE_PATH`（默认 `./my-upload-key.jks`）、`STORE_PASSWORD`、`KEY_ALIAS`（默认 `upload`）、`KEY_PASSWORD`。
- 环境：`compileSdk 36 / targetSdk 34 / minSdk 26`，JDK 17，AGP 9.1.1，Kotlin 2.2.10，KSP 2.3.12，Gradle 9.3.1（wrapper 已包含）。

---

## 6. 测试

`app/src/test` 覆盖核心坑点：空格等待、`isSpace` 重放不吞键、跨段回删、`nameList` 跳过、完成事件、同词连续错合并、跳转游标、**rawInput 保留错字**、简单词/已掌握词跳过。

---

## 7. 变更记录

- **内置文章**：从“程序生成的模板文本”改为读取 `assets/articles_2010_2025.json` 的真实 32 篇范文。
- **错误字符可渲染**：`EngineSnapshot` 新增 `rawInput`，`TokenRender` 新增 `typedRaw`，支持“打错标红”。
- **构建可用**：补齐 `gradlew` / `gradlew.bat` / `gradle-wrapper.jar`；debug 不再依赖缺失的 `debug.keystore`；release 签名改为可选。
- **去 Google 化**：移除 google-services / secrets 插件、Firebase / Play Services / Credentials / 相机等未用依赖与残留。
- **计时准确**：新增 `setSessionActive()`，仅活动时计时。
- **过滤规则落地**：`ignoreSimpleWord` 与“已掌握词”接入引擎跳过与学习词数统计。
- **大屏依赖**：加入 `material3-window-size-class` 与 `material3-adaptive`（供平板三窗格前端使用）。
