package com.example.typingarticle.core.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** 跨平台唯一 ID（替代 java.util.UUID） */
@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()
