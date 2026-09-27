<p align="center">
  <img src="docs/logo.png" alt="TypeArticle" width="130">
</p>

<div align="center">

# TypeArticle

**文章跟打 · 单词默写，把英语写作练成肌肉记忆。**

平板与桌面上的英语学习工具。<br/>
32 篇英二范文逐句跟打，1188 词三档默写，实体键盘随手就练。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/XA-anjiu/TypeArticle?style=flat)](https://github.com/XA-anjiu/TypeArticle/stargazers)

[下载](#-快速开始) · [使用说明](#-使用说明) · [反馈问题](https://github.com/XA-anjiu/TypeArticle/issues)

</div>

---

## TypeArticle 是什么？

TypeArticle 是一款纯本地、离线优先的英语学习应用，把「文章跟打」和「单词默写」两件事做到极致。

- **文章跟打**：内置 32 篇考研英语二写作真题范文（2010–2025），逐句对照中文译文，跟着打就能练手感、练句式。
- **单词默写**：内置 1188 词三档词表，带中文释义与美式音标，按固定乱序依次默写，进度自动保存，写完整库还有祝贺卡片。
- **实体键盘**：为蓝牙/外接键盘优化，空格确认、回车切句、Alt+空格回退，打字音效齐活。

Android（华为 MatePad 等无 GMS 设备）与 Windows 桌面端共用同一套内核。

---

## ✨ 功能特性

- 📖 **32 篇范文** - 考研英语二写作真题 2010–2025，段句与逐句译文严格 1:1 对齐
- 🔤 **三档词表** - S 必背（246）/ A 话题备用（372）/ B 仅备查（570），共 1188 词
- 🗣 **释义 + 音标** - 每个单词都带中文释义和美式音标，看得懂才记得住
- ⌨️ **跟打式默写** - 逐字符判定，打错标红、退格可删，四层游标精准回退
- 👁 **默写 / 看词切换** - 一键隐藏或显示单词，先看后背或直接凭记忆拼
- 📊 **错词统计** - 后台按词去重、累计出错次数，可一键生成「错词」词库
- 🔁 **进度续练** - 词库与文章都自动断点续练，切走再回来接着练
- 🎉 **完成祝贺** - 整库默写完弹出祝贺卡片，支持「再来一遍」
- 🔈 **打字音效** - 移植 TypeWords 的机械键盘音效，六种键盘可切换、可试听
- 🗣️ **在线 TTS（可选）** - 接入小米 MiMo 语音合成，朗读句子/单词；未配置则回退系统 TTS
- 🌙 **深浅主题** - 浅色 / 夜间自由切换，圆角卡片、沉浸极简
- 📥 **一键导入** - 文章与单词均支持导入，设置页内置「AI 转换提示词」可一键复制

---

## 🚀 快速开始

### 方式一：直接下载（推荐）

前往 [Releases](https://github.com/XA-anjiu/TypeArticle/releases) 下载：

- **Android**：安装 `TypeArticle-1.0.0.apk`（首次需在系统里允许「未知来源安装」）
- **Windows 桌面版**：解压 `TypeArticle-Desktop-1.0.0-windows-x64.zip`，双击 `TypeArticle.exe` 即可（自带运行时，无需安装 Java）

**系统要求：**

- Android 8.0+（`minSdk 26`）；无 GMS 设备（如华为）同样可用
- Windows 10 / 11 x64

### 方式二：从源码构建

```bash
# Android APK
./gradlew :app:assembleDebug

# 桌面端（运行 / 打包）
./gradlew :desktop:run
./gradlew :desktop:createDistributable

# 运行单元测试
./gradlew test
```

> Windows 用户把 `./gradlew` 换成 `gradlew.bat`。

---

## 📁 项目结构

```
typearticle/
├── app/                      # Android 应用（Kotlin + Jetpack Compose）
│   └── src/main/
│       ├── assets/           # 内置文章包 + 三档词表（含释义/音标）
│       ├── res/raw/          # 打字音效
│       └── java/com/example/ # UI + ViewModel + Room/DataStore/音频
├── shared/                   # KMP 共享内核（纯 Kotlin）
│   ├── core/engine           # 打字状态机（游标 / 空格等待 / 回退）
│   ├── core/parser           # 切段切句、词元化
│   ├── core/model            # 数据模型
│   └── ui/                   # 跨端共享界面（Compose Multiplatform）
├── desktop/                  # 桌面端（Compose Multiplatform）
│   └── src/jvmMain/          # 入口、桌面容器、音频/文件实现
├── gradle/libs.versions.toml
└── README.md
```

---

## 🛠️ 技术栈

- **语言**: Kotlin 2.2.10
- **界面**: Jetpack Compose（Android） / Compose Multiplatform 1.10.3（桌面共享）
- **架构**: Kotlin Multiplatform（`:app` + `:shared` + `:desktop`），内核 UI 无关、可 JVM 单测
- **数据**: Room（Android）/ Room + SQLite（桌面）、DataStore Preferences（设置）、断点续练
- **音频**: Media3 ExoPlayer、Android SoundPool / TextToSpeech，可选接入小米 MiMo TTS
- **构建**: Gradle 9.3.1 / AGP 9.1.1 / KSP

---

## 📖 使用说明

### 1. 切换模式

侧栏底部「**文章 / 单词**」切换：文章模式列出范文，单词模式列出词库（S / A / B）。

### 2. 文章跟打

点击左侧文章开始。当前句加深、当前词高亮带光标，打错的字符标红，按 `Backspace` 可跨词、跨句、跨段回退。顶栏可开关译文、重播、上下句。

### 3. 单词默写

- 侧栏「**默写**」开关：**开启**时隐藏单词凭记忆拼写，**关闭**时显示单词边看边打。
- 当前词拼完后按 **空格 / 回车** 才进入下一词，方便检查拼写。
- 默写位置会**自动保存**，退出或切换词库后再回来，接着上次的词继续。
- 整库默写完成后弹出 🎉 祝贺卡片，可「再来一遍」或「看错词」。

### 4. 错词统计

单词页右上角「**错词**」打开统计卡片：同词只存一条、累计出错次数，可「**制作成新词库**」把错词生成一个可见词库，或「清空统计」。

### 5. 导入自己的内容

进入「设置」，点「**文章 · AI 提示词**」或「**单词 · AI 提示词**」一键复制提示词，连同你的内容发给任意 AI，让它输出符合格式的 JSON/TXT/CSV，再点「导入」选择文件即可。

---

## ⌨️ 快捷键

| 功能 | 按键 |
|---|---|
| 下一句 / 下一词 | `回车` 或 `→` |
| 上一句 / 上一词 | `Alt + 空格` 或 `←` |
| 重播当前句 / 词 | `R` |
| 单词确认 / 进下一词 | `空格` |
| 开启 / 关闭译文 | `Ctrl + T` |
| 默写 / 跟打 切换 | `Ctrl + D` |
| 收起 / 展开侧栏 | `Ctrl + B` |
| 深色 / 浅色 切换 | `Ctrl + J` |

---

## ⚙️ 配置项

设置项保存在本机（DataStore），并可在设置页管理书库 / 词库。

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 在线语音合成 | 小米 MiMo TTS，在 `local.properties` 配置后启用 | 关闭（走系统 TTS） |
| `MIMO_API_KEY` | MiMo API Key（仅本机，不入库） | 空 |
| `MIMO_TTS_VOICE` | 音色（英文 `Mia/Chloe/Milo/Dean`，中文 `冰糖/茉莉/苏打/白桦`） | `Mia` |
| 按键音 / 出错提示音 | 打字音效开关 | 开启 |
| 键盘音效 | 机械键盘 / 笔记本键盘 等，可试听 | 机械键盘1 |
| 过滤规则 | 忽略符号 / 简单词 / 已掌握词 | 关 |

> 启用在线 TTS：在项目根目录 `local.properties`（已在 `.gitignore`，不会提交）写入 `MIMO_API_KEY=sk-...`，重新构建即可；合成结果会缓存到本机，断网也能重播。

---

## 🐛 已知问题

- 数据库结构升级时（如新增功能）会**清空本地数据**（自建词库、错词统计），升级后需重新导入
- 升级后**首次启动**可能有几秒「导入内置内容」的空窗，重启即可
- 桌面版为 Windows x64 打包；其他平台需自行 `createDistributable`
- Release 中的 APK 为 **debug 版**，用于体验；正式分发建议自行签发 release 版

---

## 📄 许可证

MIT License

---

## 🙏 致谢

- [小米 MiMo](https://mimo.mi.com/) - 在线语音合成（TTS）
- [TypeWords](https://github.com/zyronon/type-words) - 打字音效移植来源
- Jetpack Compose / Compose Multiplatform / Room
