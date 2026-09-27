package com.example.typingarticle.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.typingarticle.core.util.newId

@Entity(
    tableName = "word",
    indices = [Index(value = ["book_id", "idx"])]
)
data class WordEntity(
    @PrimaryKey val id: String = newId(),
    @ColumnInfo(name = "book_id") val bookId: String,
    val idx: Int,
    val word: String,
    val phonetic0: String? = null,
    val phonetic1: String? = null,
    val trans: String? = null,
    @ColumnInfo(name = "sentences_json") val sentencesJson: String? = null,
    @ColumnInfo(name = "phrases_json") val phrasesJson: String? = null
)
