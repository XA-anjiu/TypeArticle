package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.WrongWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WrongWordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: WrongWordEntity): Long

    @Query("UPDATE wrong_word SET count = count + 1, updated_at = :now WHERE word = :word")
    suspend fun bump(word: String, now: Long)

    @Query("SELECT * FROM wrong_word ORDER BY count DESC, updated_at DESC")
    fun observe(): Flow<List<WrongWordEntity>>

    @Query("SELECT * FROM wrong_word ORDER BY count DESC, updated_at DESC")
    suspend fun all(): List<WrongWordEntity>

    @Query("DELETE FROM wrong_word WHERE word = :word")
    suspend fun delete(word: String)

    @Query("DELETE FROM wrong_word")
    suspend fun clear()
}
