package com.manueldidonna.virtualscheduler

import kotlin.coroutines.Continuation

/**
 * A point in the flow of a routine scheduled by [VirtualScheduler] and resumed after its creation
 */
data class SuspensionPoint internal constructor(
    val time: Long,
    val tag: String,
    internal val cont: Continuation<Unit>
) : Comparable<SuspensionPoint> {
    override fun compareTo(other: SuspensionPoint): Int = time.compareTo(other.time)
}