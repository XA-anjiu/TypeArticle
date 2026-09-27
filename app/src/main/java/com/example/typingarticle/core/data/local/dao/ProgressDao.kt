package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE article_id = :articleId LIMIT 1")
    fun observeProgress(articleId: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE article_id = :articleId LIMIT 1")
    suspend fun getProgress(articleId: String): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE article_id = :articleId")
    suspend fun clearProgress(articleId: String)
}
