package com.manueldidonna.virtualscheduler

import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A coroutine context for a [VirtualScheduler] based on [EmptyCoroutineContext]
 */
object SchedulerContext : CoroutineContext, Serializable {
    private const val serialVersionUID: Long = 0
    private fun readResolve(): Any = SchedulerContext

    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? = null
    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R = initial
    override fun plus(context: CoroutineContext): CoroutineContext = context
    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext = this
    override fun hashCode(): Int = 0
    override fun toString(): String = "com.manueldidonna.virtualscheduler.SchedulerContext"
}
