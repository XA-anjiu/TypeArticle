package com.example.typingarticle.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.typingarticle.core.util.newId

@Entity(tableName = "statistic")
data class StatisticEntity(
    @PrimaryKey val id: String = newId(),
    @ColumnInfo(name = "ref_type") val refType: String, // "article" / "word"
    @ColumnInfo(name = "ref_id") val refId: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "spend_ms") val spendMs: Long,
    val total: Int,
    val wrong: Int
)
