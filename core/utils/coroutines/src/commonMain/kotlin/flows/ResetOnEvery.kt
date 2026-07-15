package me.him188.ani.utils.coroutines.flows

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 创建一个 flow, 当 [durationMillis] 时间内没有新的元素时, 会调用 [reset] 方法.
 *
 * 使用 generation 计数器代替时钟（不依赖系统时间，避免时钟回拨问题）。
 */
fun <T> Flow<T>.resetStale(
    durationMillis: Long,
    reset: suspend FlowCollector<T>.() -> Unit,
): Flow<T> {
    val upstream = this
    return channelFlow {
        val collector: FlowCollector<T> = FlowCollector { value -> send(value) }
        coroutineScope {
            val state = object {
                @Volatile var generation = 0L
                @Volatile var lastSeenGeneration = -1L
            }
            launch {
                while (isActive) {
                    delay(durationMillis)
                    val currentGen = state.generation
                    if (state.lastSeenGeneration == currentGen) {
                        // No new events since last check → reset
                        reset(collector)
                    }
                    state.lastSeenGeneration = currentGen
                }
            }
            upstream.collect {
                state.generation++
                send(it)
            }
        }
    }
}
