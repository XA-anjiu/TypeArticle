package com.example.desktop

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test

class InspectRealDbTest {

    @Test
    fun inspect() {
        val appDir = File(System.getProperty("user.home"), ".typearticle")
        val db = File(appDir, "typearticle.db")
        println("DB exists = ${db.exists()}  (${db.absolutePath})")
        if (!db.exists()) return

        val c = DesktopContainer(appDir)
        try {
            val book = runBlocking { c.wordRepository.getWordBook("wb_en2_s") }
            println("wordbook wb_en2_s = $book")
            val words = runBlocking { c.wordRepository.getWords("wb_en2_s") }
            println("word count = ${words.size}")
            if (words.isNotEmpty()) {
                println("first 3 words = ${words.take(3).map { it.word }}")
            }
            val prog = runBlocking { c.wordRepository.getProgress("wb_en2_s") }
            println("PROGRESS wb_en2_s = $prog")

            val books = runBlocking { c.wordRepository.observeWordBooks() }
            println("wordbooks = ${runBlocking { c.wordRepository.getWordBook("wb_en2_s")?.name }}")

            val articles = runBlocking { c.contentRepository.getArticleCount("book_english_writing_2010_2025") }
            println("article count = $articles")
        } finally {
            c.database.close()
        }
    }
}
