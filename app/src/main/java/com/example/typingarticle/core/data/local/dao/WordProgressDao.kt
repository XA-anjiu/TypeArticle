package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.WordProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordProgressDao {
    @Query("SELECT * FROM word_progress WHERE book_id = :bookId LIMIT 1")
    fun observeWordProgress(bookId: String): Flow<WordProgressEntity?>

    @Query("SELECT * FROM word_progress WHERE book_id = :bookId LIMIT 1")
    suspend fun getWordProgress(bookId: String): WordProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWordProgress(progress: WordProgressEntity)

    @Query("DELETE FROM word_progress WHERE book_id = :bookId")
    suspend fun clearWordProgress(bookId: String)
}
