package com.manueldidonna.virtualscheduler

/**
 * A schedule wraps a sequence of sequentially actions.
 * All schedules are registered before their [block]s have been evaluated.
 *
 * A schedule can't be discarded if it's been evaluated.
 * Use [children], [alive], [wait] to still abort actions within them.
 *
 * @return [VirtualScheduler] instance
 */
fun VirtualScheduler.schedule(startDelayInMillis: Long = 0L, tag: String, block: ScheduleBlock): VirtualScheduler {
    if (!this.validateTag(tag)) return this
    createScheduledRoutine(startDelayInMillis, tag) {
        if (this.validateTag(tag)) {
            ScheduleContext(virtualScheduler = this, scheduleTag = tag).block()
        }
    }
    return this
}

/**
 * Anonymous creates a suspension point delayed by [delayInMillis].
 * It can't be deleted by tag within a schedule.
 * If the schedule has been evaluated, [block] [AnonymousBlock] will surely executes.
 *
 * It's lazy evaluated within a schedule.
 */
suspend fun ScheduleContext.anonymous(delayInMillis: Long = 0L, block: AnonymousBlock) {
    virtualScheduler.suspendRoutine(delayInMillis, "")
    block()
}

/**
 * Children wraps actions under the same [tag].
 * Children checks if [tag] is still valid
 * and then it evaluates [block].
 * Children does always create an anonymous suspension point.
 *
 * It's lazy evaluated within a schedule or another wrapper.
 */
suspend fun OperatorContext.children(tag: String = this.internalTag, block: ChildrenBlock) {
    virtualScheduler.suspendRoutine(0, "")
    if (!virtualScheduler.validateTag(tag)) return
    ChildrenContext(virtualScheduler = virtualScheduler, childrenTag = tag).block()
}

/**
 * Alive checks if the receiver [OperatorContext.internalTag]
 * is still valid and then it invokes [block].
 * Alive doesn't create a suspension point.
 *
 * It's lazy evaluated within a schedule or another wrapper.
 */
suspend fun OperatorContext.alive(block: suspend () -> Unit) {
    if (!this.virtualScheduler.validateTag(this.internalTag)) return
    block()
}

/**
 * Dead checks if the receiver [OperatorContext.internalTag]
 * isn't valid and then it invokes [block].
 * Dead doesn't create a suspension point.
 *
 * It's lazy evaluated within a schedule or another wrapper.
 */
suspend fun OperatorContext.dead(block: suspend () -> Unit) {
    if (this.virtualScheduler.validateTag(this.internalTag)) return
    block()
}

/**
 * Wait checks if the receiver [OperatorContext.internalTag]
 * is still valid and then it invokes [block].
 * Wait creates a suspension point delayed by [delayInMillis].
 * It does a check before and after the suspension.

 * It's lazy evaluated within a schedule or another wrapper.
 */
suspend fun OperatorContext.wait(delayInMillis: Long, block: suspend () -> Unit) {
    if (!this.virtualScheduler.validateTag(this.internalTag)) return
    this.virtualScheduler.suspendRoutine(delayInMillis, this.internalTag)
    if (!this.virtualScheduler.validateTag(this.internalTag)) return
    block()
}