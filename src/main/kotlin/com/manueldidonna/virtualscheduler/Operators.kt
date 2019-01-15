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
 * Anonymous is a wrapper and it can't be deleted by tag.
 * Every statement within anonymous will inherits an empty tag.
 * Anonymous creates a suspension point delayed by [delayInMillis].
 *
 * It's lazy evaluated within a schedule or another wrapper.
 */
suspend inline fun OperatorContext.anonymous(delayInMillis: Long = 0L, block: WrapperBlock) {
    virtualScheduler.suspendRoutine(delayInMillis, "")
    WrapperContext(virtualScheduler = this.virtualScheduler, wrapperTag = "").block()
}

/**
 * Children is a wrapper and it creates an empty suspension point.
 * Every statements within children will inherit the same tag.
 * Children checks for [tag] validity before [block] execution.
 *
 * It's lazy evaluated within a schedule or another wrapper.
 */
suspend inline fun OperatorContext.children(tag: String = this.internalTag, block: WrapperBlock) {
    virtualScheduler.suspendRoutine(0, "")
    if (!virtualScheduler.validateTag(tag)) return
    WrapperContext(virtualScheduler = virtualScheduler, wrapperTag = tag).block()
}

/**
 * Wait checks if the receiver [OperatorContext.internalTag]
 * is still valid and then it invokes [block].
 * Wait creates a suspension point delayed by [delayInMillis].
 * It does a check before and after the suspension.

 * It's lazy evaluated within a schedule or another wrapper.
 */
suspend inline fun OperatorContext.wait(delayInMillis: Long, block: NoContextBlock) {
    if (!this.virtualScheduler.validateTag(this.internalTag)) return
    this.virtualScheduler.suspendRoutine(delayInMillis, this.internalTag)
    if (!this.virtualScheduler.validateTag(this.internalTag)) return
    block()
}

/**
 * Alive checks if the receiver [OperatorContext.internalTag]
 * is still valid and then it invokes [block].
 * Alive doesn't create a suspension point.
 *
 * It's lazy evaluated within a schedule or another wrapper.
 */
@Suppress("RedundantSuspendModifier")
suspend inline fun OperatorContext.alive(block: NoContextBlock) {
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
@Suppress("RedundantSuspendModifier")
suspend inline fun OperatorContext.dead(block: NoContextBlock) {
    if (this.virtualScheduler.validateTag(this.internalTag)) return
    block()
}

/**
 * Yield creates an empty suspension point.
 * It allows other points to be executed within
 * other schedules according to their priority.
 */
suspend inline fun OperatorContext.yield() {
    virtualScheduler.suspendRoutine(0, "")
}