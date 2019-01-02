import kotlin.coroutines.Continuation


/**
 * A sort of suspension point scheduled by [VirtualScheduler] and resumed after its creation
 */
data class State internal constructor(
    val time: Long,
    val tag: String,
    internal val cont: Continuation<Unit>
) : Comparable<State> {
    override fun compareTo(other: State): Int = time.compareTo(other.time)
}