package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM word WHERE book_id = :bookId ORDER BY idx ASC")
    fun observeWordsByBook(bookId: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM word WHERE book_id = :bookId ORDER BY idx ASC")
    suspend fun getWordsByBook(bookId: String): List<WordEntity>

    @Query("SELECT * FROM word WHERE book_id = :bookId AND idx = :idx LIMIT 1")
    suspend fun getWordByBookAndIdx(bookId: String, idx: Int): WordEntity?

    @Query("SELECT * FROM word WHERE word = :wordText LIMIT 1")
    suspend fun getWordByText(wordText: String): WordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("DELETE FROM word WHERE book_id = :bookId")
    suspend fun deleteWordsByBook(bookId: String)
}
