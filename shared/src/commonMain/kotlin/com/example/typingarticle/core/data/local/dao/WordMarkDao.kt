package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.WordMarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordMarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMark(mark: WordMarkEntity)

    @Query("SELECT * FROM word_mark WHERE kind = :kind ORDER BY created_at DESC")
    fun observeMarksByKind(kind: String): Flow<List<WordMarkEntity>>

    @Query("SELECT * FROM word_mark WHERE kind = :kind ORDER BY created_at DESC")
    suspend fun getMarksByKind(kind: String): List<WordMarkEntity>

    @Query("DELETE FROM word_mark WHERE word = :word")
    suspend fun deleteMarkByWord(word: String)

    @Query("DELETE FROM word_mark WHERE kind = :kind")
    suspend fun clearMarksByKind(kind: String)
}
