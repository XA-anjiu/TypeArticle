package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.Radius

private val mono = FontFamily.Monospace

/** 圆角卡片容器 */
@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    radius: Dp = Radius.card,
    color: Color = AppTheme.colors.surface,
    bordered: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .then(if (bordered) Modifier.border(1.dp, c.line, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        content = content,
    )
}

/** 胶囊 / 侧栏条目 */
@Composable
fun EuChip(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(Radius.btn)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (active) c.primary else c.surface)
            .border(1.dp, if (active) c.primary else c.line, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (active) c.onPrimary else c.text,
            fontFamily = mono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun EuPrimary(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.btn))
            .background(c.primary)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = c.onPrimary, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun EuGhost(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.btn))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(Radius.btn))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun EuDanger(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.btn))
            .background(c.badBg)
            .border(1.dp, c.bad.copy(alpha = 0.45f), RoundedCornerShape(Radius.btn))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = c.bad, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** 可点按键帽（快捷键设置用） */
@Composable
fun EuKeycap(text: String, recording: Boolean = false, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (recording) c.primary else c.bg)
            .border(1.dp, if (recording) c.primary else c.line, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (recording) c.onPrimary else c.text,
            fontFamily = mono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

/** 开关行 */
@Composable
fun EuToggleRow(label: String, checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.btn))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(Radius.btn))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(12.dp))
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(44.dp)
                .size(width = 44.dp, height = 24.dp)
                .clip(CircleShape)
                .background(if (checked) c.primary else c.active),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (checked) 22.dp else 3.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

/** 分段控件 */
@Composable
fun EuSegmented(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.btn))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(Radius.btn))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { i, t ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected == i) c.primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    t,
                    color = if (selected == i) c.onPrimary else c.soft,
                    fontFamily = mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

/** 统计小盒 */
@Composable
fun EuStatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.btn))
            .background(c.bg)
            .border(1.dp, c.line, RoundedCornerShape(Radius.btn))
            .padding(14.dp),
    ) {
        Text(value, color = c.text, fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = c.soft, fontFamily = mono, fontSize = 12.sp)
    }
}

@Composable
fun EuSectionLabel(text: String) {
    Text(
        text = text,
        color = AppTheme.colors.soft,
        fontFamily = mono,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp),
    )
}
