abstract class BaseContext {
    internal abstract val tag: String
    internal abstract val virtualScheduler: VirtualScheduler
}

data class ChildrenContext internal constructor(
    internal override val virtualScheduler: VirtualScheduler,
    val childrenTag: String
) : BaseContext() {
    internal override val tag: String = childrenTag
}

data class ScheduleContext internal constructor(
    internal override val virtualScheduler: VirtualScheduler,
    val scheduleTag: String
) : BaseContext() {
    internal override val tag: String = scheduleTag
}