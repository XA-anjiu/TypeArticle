package com.example.typingarticle.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey @ColumnInfo(name = "article_id") val articleId: String,
    @ColumnInfo(name = "section_i") val sectionI: Int = 0,
    @ColumnInfo(name = "sentence_i") val sentenceI: Int = 0,
    @ColumnInfo(name = "word_i") val wordI: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
