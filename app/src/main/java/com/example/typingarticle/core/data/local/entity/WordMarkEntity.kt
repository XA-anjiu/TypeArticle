package com.example.typingarticle.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_mark")
data class WordMarkEntity(
    @PrimaryKey val word: String,
    val kind: String, // "collect", "wrong", "known"
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
