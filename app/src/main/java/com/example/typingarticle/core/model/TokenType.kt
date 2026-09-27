package com.example.typingarticle.core.model

/**
 * 词元类型定义
 *
 * 优先级（高到低）：
 * 1. Number: 货币符号+数字、纯数字（含千分位、小数、百分比）
 * 2. Word: 英文缩写（含点）、英文单词（含撇号与连字符）
 * 3. Symbol: 标点符号与非空白单字符
 */
enum class TokenType {
    Number,
    Word,
    Symbol
}
