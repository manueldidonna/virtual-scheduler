package com.manueldidonna.virtualscheduler

import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Manage a queue of [SuspensionPoint] to schedule actions
 * according to their priority. All of them act sequentially.
 *
 * Unless you use multiple instances of this class,
 * you won't achieve parallelism.
 */
class VirtualScheduler : Continuation<Unit> {

    private val points = PriorityQueue<SuspensionPoint>()
    private val discardedTags = hashSetOf<String>()
    private val isRunning = AtomicBoolean(false)

    /**
     * The current time of the com.manueldidonna.virtualscheduler.VirtualScheduler. Increased within [run]
     */
    private var time: Long = 0

    override val context: SchedulerContext = SchedulerContext

    override fun resumeWith(result: Result<Unit>) {
        when {
            result.isFailure -> throw result.exceptionOrNull()!!
        }
    }

    /**
     * Create a continuation from [block] to be resumed on [run]
     *
     * @see [schedule]
     */
    internal fun createScheduledRoutine(startDelayInMillis: Long, tag: String, block: Routine): SuspensionPoint {
        return SuspensionPoint(
            time = time + startDelayInMillis,
            cont = block.createCoroutine(receiver = this, completion = this),
            tag = tag
        ).also { point -> points.add(point) }
    }

    /**
     * Create a [SuspensionPoint] suspending the current routine
     * it allows other points to be executed on [run] according to their priority.
     *
     * @see [anonymous] [children] [wait]
     */
    internal suspend fun suspendRoutine(millis: Long, tag: String) {
        suspendCoroutine { cont: Continuation<Unit> ->
            points.add(SuspensionPoint(time = millis + time, cont = cont, tag = tag))
        }
    }

    /**
     * Return true if [tag] is allowed (not discarded) or false otherwise
     *
     * @see [children] [alive]
     */
    internal fun validateTag(tag: String): Boolean {
        return !discardedTags.contains(tag)
    }

    /**
     * Add [tag] to [discardedTags]
     */
    fun discardTag(tag: String): VirtualScheduler {
        if (tag.isNotEmpty())
            discardedTags.add(tag)
        return this
    }

    /**
     * Remove [tag] from [discardedTags]
     */
    fun restoreTag(tag: String): VirtualScheduler {
        discardedTags.remove(tag)
        return this
    }

    /**
     * Delete all saved points. Interrupt the 'while block' within [run]
     */
    fun clear() {
        points.clear()
        discardedTags.clear()
    }

    /**
     * Runs all the routines that have been scheduled so far
     *
     * It's safe to call run() many times.
     * If the previously invocation is still running, the newer ones will immediately returns.
     */
    suspend fun run() {
        if (isRunning.get()) return
        isRunning.set(true)
        var next: SuspensionPoint? = points.poll()
        while (next != null) {
            val timeToWait = next.time - time
            if (timeToWait > 0) {
                // process time advance
                delay(timeToWait)
            }
            time = next.time
            // Re-run the routine
            next.cont.resume(Unit)
            // Pull the next
            next = points.poll()
        }
        isRunning.set(false)
    }
}