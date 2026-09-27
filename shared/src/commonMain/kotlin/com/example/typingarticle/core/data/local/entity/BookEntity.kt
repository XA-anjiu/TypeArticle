package com.example.typingarticle.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book")
data class BookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val lang: String = "en",
    val version: Int = 1,
    val length: Int = 0,
    val source: String = "builtin"
)
