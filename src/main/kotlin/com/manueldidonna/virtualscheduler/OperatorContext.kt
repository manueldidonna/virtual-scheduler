package com.manueldidonna.virtualscheduler

abstract class OperatorContext {
    internal abstract val internalTag: String
    internal abstract val virtualScheduler: VirtualScheduler

    fun isAlive(): Boolean = virtualScheduler.validateTag(internalTag)
}

data class ChildrenContext internal constructor(
    override val virtualScheduler: VirtualScheduler,
    val childrenTag: String
) : OperatorContext() {
    override val internalTag: String = childrenTag
}

data class ScheduleContext internal constructor(
    override val virtualScheduler: VirtualScheduler,
    val scheduleTag: String
) : OperatorContext() {
    override val internalTag: String = scheduleTag
}