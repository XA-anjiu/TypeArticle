package com.example.typingarticle.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "article",
    indices = [Index(value = ["book_id", "idx"])]
)
data class ArticleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "book_id") val bookId: String,
    val idx: Int,
    val title: String,
    @ColumnInfo(name = "title_translate") val titleTranslate: String? = null,
    val text: String,
    @ColumnInfo(name = "text_translate") val textTranslate: String? = null,
    @ColumnInfo(name = "audio_uri") val audioUri: String? = null,
    @ColumnInfo(name = "lrc_json") val lrcJson: String? = null,
    @ColumnInfo(name = "name_list_json") val nameListJson: String? = null
)
