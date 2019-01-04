import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class VirtualSchedulerTest {

    private lateinit var vs: VirtualScheduler

    @Before
    fun setup() {
        vs = spy(VirtualScheduler())
    }

    @Test
    fun orderOfEvaluationForSchedules() {
        runBlocking {
            // arrange
            val action1: () -> Unit = mock()
            val action2: () -> Unit = mock()

            // trigger
            vs.schedule(400L, "firstSchedule") {
                alive { action1() }
            }
            // secondSchedule should begin before firstSchedule,
            // but when secondSchedule is added, the first is already captured
            // by the scheduler in the .run() while (next != null) loop
            GlobalScope.launch {
                delay(100)
                vs.schedule(100L, "secondSchedule") {
                    alive { action2() }
                }
            }
            vs.run()

            // validation
            inOrder(action1, action2) {
                verify(action1).invoke()
                verify(action2).invoke()
            }
        }
    }

    @Test
    fun scheduleDiscardedAndThenRestored() {
        runBlocking {
            // arrange
            val actionToNotInvoke: () -> Unit = mock()
            val actionToInvoke: () -> Unit = mock()
            val tagToDiscard = "scheduleToDiscard"

            // trigger
            vs.schedule(500L, tag = tagToDiscard) {
                actionToNotInvoke()  // (tagToDiscard, 500) is registered as suspension point
            }.schedule(700L, tag = tagToDiscard) {
                actionToInvoke() // (tagToDiscard, 700) is registered as suspension point
            }.schedule(200L, tag = "discardTag") {
                vs.discardTag(tagToDiscard) // (tagToDiscard, 500) is discarded before the execution of its block
            }.schedule(600L, tag = "restoreTag") {
                vs.restoreTag(tagToDiscard) // (tagToDiscard, 700) is restored before the execution of its block
            }.run() // the scheduler will wait anyway for 700 millis.
            // Validation checks are executed before and after the time processing, not during the process

            // validation
            verify(actionToInvoke, times(1)).invoke()
            verify(actionToNotInvoke, times(0)).invoke()
        }
    }

    @Test
    fun anonymousBlocksSurviveScheduleDestruction() {
        runBlocking {
            // arrange
            val action: () -> Unit = mock()
            val tagToDiscard = "schedule"

            // trigger
            vs.schedule(100L, tagToDiscard) {
                children {
                    wait(100L) {
                        action()
                    }
                }
                anonymous {
                    action()
                }
            }.schedule(190L, "traitor") {
                vs.discardTag(tagToDiscard)
            }.run()

            // validation
            verify(action, times(1)).invoke()
        }
    }
}