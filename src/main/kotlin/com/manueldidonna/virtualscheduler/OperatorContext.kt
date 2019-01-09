package com.manueldidonna.virtualscheduler

abstract class OperatorContext {
    internal abstract val internalTag: String
    internal abstract val virtualScheduler: VirtualScheduler

    fun isAlive(): Boolean = virtualScheduler.validateTag(internalTag)
}

data class WrapperContext internal constructor(
    override val virtualScheduler: VirtualScheduler,
    val wrapperTag: String
) : OperatorContext() {
    override val internalTag: String = wrapperTag
}

data class ScheduleContext internal constructor(
    override val virtualScheduler: VirtualScheduler,
    val scheduleTag: String
) : OperatorContext() {
    override val internalTag: String = scheduleTag
}