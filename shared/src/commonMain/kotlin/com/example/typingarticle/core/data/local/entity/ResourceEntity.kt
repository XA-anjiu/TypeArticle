package com.example.typingarticle.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resource")
data class ResourceEntity(
    @PrimaryKey val key: String,
    val etag: String? = null,
    val path: String,
    val size: Long = 0L,
    val kind: String = "audio"
)
