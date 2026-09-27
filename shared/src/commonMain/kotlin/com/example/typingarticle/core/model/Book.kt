package com.example.typingarticle.core.model

/**
 * 书籍对象
 *
 * @param id 书籍标识
 * @param name 书名
 * @param description 描述
 * @param lang 语言（如 "en"）
 * @param translateLanguage 翻译语言（如 "zh-CN"）
 * @param version 版本号
 * @param length 文章总数
 * @param source 来源：builtin (内置) / import (导入)
 * @param articles 包含的文章列表
 */
data class Book(
    val id: String,
    val name: String,
    val description: String? = null,
    val lang: String = "en",
    val translateLanguage: String = "zh-CN",
    val version: Int = 1,
    val length: Int = 0,
    val source: String = "builtin",
    val articles: List<Article> = emptyList()
)
