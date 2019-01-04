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
        if (this.validateTag(tag)){
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
 * Children wraps actions under the same tag allowing them to be aborted easily.
 * It also checks if [tag] is still valid before it evaluates [block].
 * Children does create a suspension point.
 *
 * It's lazy evaluated within a schedule.
 */
suspend fun ScheduleContext.children(tag: String = this.scheduleTag, block: ChildrenBlock) {
    virtualScheduler.suspendRoutine(0, "")
    if (!virtualScheduler.validateTag(tag)) return
    ChildrenContext(virtualScheduler = virtualScheduler, childrenTag = tag).block()
}

/**
 * Alive checks if the receiver [OperatorContext.tag]
 * is still valid and then it invokes [block].
 * Alive doesn't create a suspension point.
 *
 * It's lazy evaluated within a schedule.
 */
suspend fun OperatorContext.alive(block: suspend () -> Unit) {
    if (!this.virtualScheduler.validateTag(this.tag)) return
    block()
}

/**
 * Wait creates a suspension point delayed by [delayInMillis].
 * Check if the receiver [ChildrenContext.childrenTag] is still valid.
 *
 * It's lazy evaluated within a schedule.
 */
suspend fun ChildrenContext.wait(delayInMillis: Long, block: suspend () -> Unit) {
    if (!this.virtualScheduler.validateTag(this.childrenTag)) return
    this.virtualScheduler.suspendRoutine(delayInMillis, this.childrenTag)
    if (!this.virtualScheduler.validateTag(this.childrenTag)) return
    block()
}