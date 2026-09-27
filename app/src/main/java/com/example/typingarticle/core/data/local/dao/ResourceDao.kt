package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.ResourceEntity

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resource WHERE `key` = :key LIMIT 1")
    suspend fun getResourceByKey(key: String): ResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)
}
