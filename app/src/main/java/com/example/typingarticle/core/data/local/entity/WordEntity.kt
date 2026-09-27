package com.example.typingarticle.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "word",
    indices = [Index(value = ["book_id", "idx"])]
)
data class WordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "book_id") val bookId: String,
    val idx: Int,
    val word: String,
    val phonetic0: String? = null,
    val phonetic1: String? = null,
    val trans: String? = null,
    @ColumnInfo(name = "sentences_json") val sentencesJson: String? = null,
    @ColumnInfo(name = "phrases_json") val phrasesJson: String? = null
)
