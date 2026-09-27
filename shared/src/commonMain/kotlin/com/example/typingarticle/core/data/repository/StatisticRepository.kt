package com.example.typingarticle.core.data.repository

import com.example.typingarticle.core.data.local.dao.StatisticDao
import com.example.typingarticle.core.data.local.entity.StatisticEntity
import kotlinx.coroutines.flow.Flow

/**
 * 学习统计数据仓库
 */
class StatisticRepository(
    private val statisticDao: StatisticDao
) {
    suspend fun saveStatistic(
        refType: String,
        refId: String,
        startedAt: Long,
        spendMs: Long,
        total: Int,
        wrong: Int
    ) {
        val entity = StatisticEntity(
            refType = refType,
            refId = refId,
            startedAt = startedAt,
            spendMs = spendMs,
            total = total,
            wrong = wrong
        )
        statisticDao.insertStatistic(entity)
    }

    fun observeStats(refType: String, refId: String): Flow<List<StatisticEntity>> {
        return statisticDao.observeStatisticsByRef(refType, refId)
    }

    fun observeAllStats(): Flow<List<StatisticEntity>> {
        return statisticDao.observeAllStatistics()
    }
}
