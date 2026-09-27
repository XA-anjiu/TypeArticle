package com.example.typingarticle.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 错词统计（隐藏词库）：按单词去重，仅累计错误次数，不出现在侧栏 / 词库列表。
 */
@Entity(tableName = "wrong_word")
data class WrongWordEntity(
    @PrimaryKey val word: String,
    val count: Int = 1,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
