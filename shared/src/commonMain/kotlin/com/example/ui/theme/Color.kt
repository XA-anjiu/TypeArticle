package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 应用设计 token（对标 EUI-NEO theme.h + 已确认的 mockup）
 * - 主色 primary: #3871E0
 * - 浅色底 #F2F2F7 / 卡面 #FFFFFF
 * - 深色底 #000000（纯黑，已去蓝调）/ 卡面 #141414
 * - 圆角：card 18 / button 16 / feature 22 / field 6
 */
data class AppColors(
    val bg: Color,
    val surface: Color,
    val hover: Color,
    val active: Color,
    val text: Color,
    val soft: Color,
    val line: Color,
    val primary: Color,
    val onPrimary: Color,
    val bad: Color,
    val badBg: Color,
    val dim: Color,
    val isDark: Boolean
)

val LightColors = AppColors(
    bg = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    hover = Color(0xFFE6E6E6),
    active = Color(0xFFCCCCCC),
    text = Color(0xFF000000),
    soft = Color(0x9E000000),
    line = Color(0xB8CCCCCC),
    primary = Color(0xFF3871E0),
    onPrimary = Color(0xFFF0F7FF),
    bad = Color(0xFFE5484D),
    badBg = Color(0xFFFBE3E4),
    dim = Color(0x59000000),
    isDark = false
)

val DarkColors = AppColors(
    bg = Color(0xFF000000),
    surface = Color(0xFF141414),
    hover = Color(0xFF262626),
    active = Color(0xFF383838),
    text = Color(0xFFFFFFFF),
    soft = Color(0x9EFFFFFF),
    line = Color(0x24FFFFFF),
    primary = Color(0xFF3871E0),
    onPrimary = Color(0xFFF0F7FF),
    bad = Color(0xFFFF6B6B),
    badBg = Color(0x33E5484D),
    dim = Color(0x57FFFFFF),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

object Radius {
    val card = 18.dp
    val btn = 16.dp
    val feature = 22.dp
    val field = 6.dp
}
