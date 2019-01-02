/**
 * Each schedule wraps a sequence of sequentially actions.
 * You can't discard the schedule if it's been evaluated but you can abort actions after each suspension point
 *
 * @param startDelayInMillis an initial delay before the execution of [block]
 * @param tag used to discard the schedule. Within [block] it's [ScheduleContext.scheduleTag]
 * @param block is lazy evaluated
 *
 * @return [VirtualScheduler] instance
 */
fun VirtualScheduler.schedule(startDelayInMillis: Long = 0L, tag: String, block: ScheduleBlock): VirtualScheduler {
    createScheduledRoutine(startDelayInMillis, tag) {
        ScheduleContext(virtualScheduler = this, scheduleTag = tag).block()
    }
    return this
}

/**
 * Anonymous create a suspension point that can't be deleted by tag within a schedule.
 * If the schedule has been evaluated, [block] [AnonymousBlock] will surely executes
 */
suspend fun ScheduleContext.anonymous(timeInMillis: Long = 0L, block: AnonymousBlock) {
    virtualScheduler.suspendRoutine(timeInMillis, "")
    block()
}

/**
 * Children wraps many actions under the same tag allowing them to be aborted easily.
 * It also checks if [tag] is still valid before it evaluates [block]
 */
suspend fun ScheduleContext.children(tag: String = this.scheduleTag, block: ChildrenBlock) {
    virtualScheduler.suspendRoutine(0, "")
    if (!virtualScheduler.validateTag(tag)) return
    ChildrenContext(virtualScheduler = virtualScheduler, childrenTag = tag).block()
}

/**
 * Alive checks if the receiver [BaseContext.tag] is still valid and then it invokes [block]
 * It's lazy evaluated within a schedule.
 */
suspend fun BaseContext.alive(block: suspend () -> Unit) {
    if (!this.virtualScheduler.validateTag(this.tag)) return
    block()
}