package com.manueldidonna.virtualscheduler

abstract class OperatorContext {

    @PublishedApi
    internal abstract val internalTag: String

    @PublishedApi
    internal abstract val virtualScheduler: VirtualScheduler

    fun isAlive(): Boolean = virtualScheduler.validateTag(internalTag)
}

data class WrapperContext @PublishedApi internal constructor(
    override val virtualScheduler: VirtualScheduler,
    val wrapperTag: String
) : OperatorContext() {
    override val internalTag: String = wrapperTag
}

data class ScheduleContext @PublishedApi internal constructor(
    override val virtualScheduler: VirtualScheduler,
    val scheduleTag: String
) : OperatorContext() {
    override val internalTag: String = scheduleTag
}