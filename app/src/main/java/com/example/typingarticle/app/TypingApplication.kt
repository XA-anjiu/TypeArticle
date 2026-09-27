package com.example.typingarticle.app

import android.app.Application
import com.example.typingarticle.core.data.builtin.BuiltinContentProvider
import com.example.typingarticle.di.TypingDiContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TypingApplication : Application() {

    lateinit var container: TypingDiContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = TypingDiContainer(this)

        // 首次启动异步填充内置 32 篇真题范文与核心高频词库
        applicationScope.launch {
            BuiltinContentProvider.populateIfEmpty(
                context = this@TypingApplication,
                contentRepo = container.contentRepository,
                wordRepo = container.wordRepository
            )
        }
    }
}
