package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.WordBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBookDao {
    @Query("SELECT * FROM word_book ORDER BY id ASC")
    fun observeWordBooks(): Flow<List<WordBookEntity>>

    @Query("SELECT * FROM word_book WHERE id = :id LIMIT 1")
    suspend fun getWordBookById(id: String): WordBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordBook(book: WordBookEntity)

    @Query("DELETE FROM word_book WHERE id = :id")
    suspend fun deleteWordBookById(id: String)
}
