package com.example.typingarticle.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_progress")
data class WordProgressEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: String,
    val idx: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
