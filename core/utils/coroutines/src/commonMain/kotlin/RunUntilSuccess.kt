/*
 * Copyright (C) 2024 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.utils.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.retryWhen
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 尝试多次运行一个 block, 直到成功为止. 会在每次失败后等待一段时间再重试. 等待的时间会逐渐增加.
 * @see retryWhen
 */
fun <T> Flow<T>.retryWithBackoffDelay(
    maxAttempts: Long,
    predicate: suspend FlowCollector<T>.(cause: Throwable, attempt: Long) -> Boolean = { _, _ -> true },
): Flow<T> {
    require(maxAttempts > 0) { "maxAttempts must be positive" }
    return this.retryWhen { cause, attempt ->
        if (attempt >= maxAttempts) {
            throw cause
        }
        if (predicate(cause, attempt)) {
            delay(backoffDelay(attempt.toInt()))
            true
        } else {
            false
        }
    }
}

/**
 * 尝试多次运行一个 block, 直到成功为止. 会在每次失败后等待一段时间再重试. 等待的时间会逐渐增加.
 * @see retryWhen
 */
fun <T> Flow<T>.retryWithBackoffDelay(
    predicate: suspend FlowCollector<T>.(cause: Throwable, attempt: Long) -> Boolean = { _, _ -> true },
): Flow<T> {
    return this.retryWhen { cause, attempt ->
        if (predicate(cause, attempt)) {
            delay(backoffDelay(attempt.toInt()))
            true
        } else {
            false
        }
    }
}

internal fun backoffDelay(failureCount: Int): Duration {
    return when (failureCount) {
        0, 1 -> 1.seconds
        2 -> 2.seconds
        3 -> 4.seconds
        4 -> 8.seconds
        5 -> 16.seconds
        else -> 30.seconds
    }
}

// 解决 ios ambiguity 和 common 里不能直接构造
@Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
inline fun CancellationException(
    message: String? = null,
    cause: Throwable? = null
): kotlinx.coroutines.CancellationException {
    return kotlinx.coroutines.CancellationException(
        message = message,
        cause,
    ) // 加名字后就只能 resolve 到 Kotlin, 否则会在 JVM ambiguity
}
