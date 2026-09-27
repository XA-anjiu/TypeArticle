package com.example.typingarticle.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.typingarticle.core.data.local.dao.ArticleDao
import com.example.typingarticle.core.data.local.dao.BookDao
import com.example.typingarticle.core.data.local.dao.ProgressDao
import com.example.typingarticle.core.data.local.dao.ResourceDao
import com.example.typingarticle.core.data.local.dao.StatisticDao
import com.example.typingarticle.core.data.local.dao.WordBookDao
import com.example.typingarticle.core.data.local.dao.WordDao
import com.example.typingarticle.core.data.local.dao.WordMarkDao
import com.example.typingarticle.core.data.local.dao.WordProgressDao
import com.example.typingarticle.core.data.local.dao.WrongWordDao
import com.example.typingarticle.core.data.local.entity.ArticleEntity
import com.example.typingarticle.core.data.local.entity.BookEntity
import com.example.typingarticle.core.data.local.entity.ProgressEntity
import com.example.typingarticle.core.data.local.entity.ResourceEntity
import com.example.typingarticle.core.data.local.entity.StatisticEntity
import com.example.typingarticle.core.data.local.entity.WordBookEntity
import com.example.typingarticle.core.data.local.entity.WordEntity
import com.example.typingarticle.core.data.local.entity.WordMarkEntity
import com.example.typingarticle.core.data.local.entity.WordProgressEntity
import com.example.typingarticle.core.data.local.entity.WrongWordEntity

@Database(
    entities = [
        BookEntity::class,
        ArticleEntity::class,
        ProgressEntity::class,
        StatisticEntity::class,
        WordMarkEntity::class,
        WordBookEntity::class,
        WordEntity::class,
        WordProgressEntity::class,
        ResourceEntity::class,
        WrongWordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun articleDao(): ArticleDao
    abstract fun progressDao(): ProgressDao
    abstract fun statisticDao(): StatisticDao
    abstract fun wordMarkDao(): WordMarkDao
    abstract fun wordBookDao(): WordBookDao
    abstract fun wordDao(): WordDao
    abstract fun wordProgressDao(): WordProgressDao
    abstract fun resourceDao(): ResourceDao
    abstract fun wrongWordDao(): WrongWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "typing_article.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
