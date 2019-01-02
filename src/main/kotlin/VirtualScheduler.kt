import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Manage a queue of [State] to schedule actions according to their priority.
 * All of them act sequentially.
 * Unless you use multiple instances of this class, you won't achieve parallelism.
 */
class VirtualScheduler : Continuation<Unit> {

    private val states = PriorityQueue<State>()
    private val discardedTags = hashSetOf<String>()
    private val isRunning = AtomicBoolean(false)

    /**
     * The current time of the VirtualScheduler. Increased within [run]
     */
    private var time: Long = 0

    override val context: SchedulerContext = SchedulerContext

    override fun resumeWith(result: Result<Unit>) {
        when {
            result.isFailure -> throw result.exceptionOrNull()!!
        }
    }

    /**
     * Create a continuation from [block] to be resumed on [run]
     *
     * @see [schedule]
     */
    internal fun createScheduledRoutine(startDelayInMillis: Long, tag: String, block: Routine): State {
        return State(
            time = time + startDelayInMillis,
            cont = block.createCoroutine(receiver = this, completion = this),
            tag = tag
        ).also { state -> states.add(state) }
    }

    /**
     * Create a [State] suspending the current routine
     * it allows other states to be executed on [run] according to their priority.
     *
     * @see [anonymous] [children]
     */
    internal suspend fun suspendRoutine(millis: Long, tag: String) {
        suspendCoroutine { cont: Continuation<Unit> ->
            states.add(State(time = millis + time, cont = cont, tag = tag))
        }
    }

    /**
     * Return true if [tag] is allowed (not discarded) or false otherwise
     *
     * @see [children] [alive]
     */
    internal fun validateTag(tag: String): Boolean {
        return !discardedTags.contains(tag)
    }

    /**
     * Add [tag] to [discardedTags]
     */
    fun discardTag(tag: String): VirtualScheduler {
        if (tag.isNotEmpty())
            discardedTags.add(tag)
        return this
    }

    /**
     * Remove [tag] from [discardedTags]
     */
    fun restoreTag(tag: String): VirtualScheduler {
        discardedTags.remove(tag)
        return this
    }

    /**
     * Delete all saved states. Interrupt the 'while block' within [run]
     */
    fun clear() {
        states.clear()
    }

    /**
     * Runs all the routines that have been scheduled so far
     *
     * It's safe to call run() many times.
     * If the previously invocation is still running, the newer ones will immediately returns.
     */
    suspend fun run() {
        if (isRunning.get()) return
        isRunning.set(true)
        var next: State? = states.poll()
        while (next != null) {
            if (!discardedTags.contains(next.tag)) {
                val timeToWait = next.time - time
                if (timeToWait > 0) {
                    // process time advance
                    delay(timeToWait)
                }
                time = next.time
                // Re-run the routine
                next.cont.resume(Unit)
            }
            // Pull the next
            next = states.poll()
        }
        isRunning.set(false)
    }
}