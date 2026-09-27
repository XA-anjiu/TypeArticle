package com.example.typingarticle.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_book")
data class WordBookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val source: String = "builtin" // "builtin", "import"
)
