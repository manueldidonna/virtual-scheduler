abstract class OperatorContext {
    internal abstract val tag: String
    internal abstract val virtualScheduler: VirtualScheduler
}

data class ChildrenContext internal constructor(
    override val virtualScheduler: VirtualScheduler,
    val childrenTag: String
) : OperatorContext() {
    override val tag: String = childrenTag
}

data class ScheduleContext internal constructor(
    override val virtualScheduler: VirtualScheduler,
    val scheduleTag: String
) : OperatorContext() {
    override val tag: String = scheduleTag
}