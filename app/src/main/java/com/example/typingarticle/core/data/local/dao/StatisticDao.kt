package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.StatisticEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistic(stat: StatisticEntity)

    @Query("SELECT * FROM statistic WHERE ref_type = :refType AND ref_id = :refId ORDER BY started_at DESC")
    fun observeStatisticsByRef(refType: String, refId: String): Flow<List<StatisticEntity>>

    @Query("SELECT * FROM statistic ORDER BY started_at DESC")
    fun observeAllStatistics(): Flow<List<StatisticEntity>>
}
