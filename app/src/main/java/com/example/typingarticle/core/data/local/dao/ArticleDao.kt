package com.example.typingarticle.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.typingarticle.core.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM article WHERE book_id = :bookId ORDER BY idx ASC")
    fun observeArticlesByBook(bookId: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM article WHERE book_id = :bookId ORDER BY idx ASC")
    suspend fun getArticlesByBook(bookId: String): List<ArticleEntity>

    @Query("SELECT * FROM article WHERE book_id = :bookId AND idx = :idx LIMIT 1")
    suspend fun getArticleByBookAndIndex(bookId: String, idx: Int): ArticleEntity?

    @Query("SELECT * FROM article WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Query("SELECT COUNT(*) FROM article WHERE book_id = :bookId")
    suspend fun getArticleCountByBook(bookId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Query("DELETE FROM article WHERE book_id = :bookId")
    suspend fun deleteArticlesByBook(bookId: String)
}
