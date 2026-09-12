package com.samdori93.yeoksadam.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * 코루틴 디스패처 추상화 — 테스트에서 교체 가능하도록 주입한다.
 * I/O 작업은 항상 [io] 를 통해 수행한다(CLAUDE.md §14 DO).
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

/** 프로덕션 기본 구현. DI 로 제공한다(core:common 은 순수 Kotlin 이라 Hilt 모듈은 :app/core:data 에). */
class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}
