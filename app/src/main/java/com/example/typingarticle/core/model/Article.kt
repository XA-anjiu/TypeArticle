package com.example.typingarticle.core.model

/**
 * 文章实体数据契约
 *
 * @param id 文章唯一标识
 * @param bookId 所属书本 ID
 * @param idx 文章在书本中的顺序下标
 * @param title 标题
 * @param titleTranslate 标题译文
 * @param text 正文（段落之间双换行 \n\n，段内一句一行 \n）
 * @param textTranslate 译文（段落与行结构与 text 完全相同）
 * @param audioSrc 句音频源路径（本地文件或远程 URL）
 * @param lrcPosition 每句的 [start, end] 音频时间戳（秒）列表
 * @param nameList 需自动跳过的词（如人名、地名、称谓）
 */
data class Article(
    val id: String,
    val bookId: String = "",
    val idx: Int = 0,
    val title: String,
    val titleTranslate: String? = null,
    val text: String,
    val textTranslate: String? = null,
    val audioSrc: String? = null,
    val lrcPosition: List<Pair<Double, Double>> = emptyList(),
    val nameList: List<String> = emptyList()
)
