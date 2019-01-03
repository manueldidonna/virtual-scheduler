import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
            }.schedule(140L, "traitor") {
                vs.discardTag(tagToDiscard)
            }.run()

            // validation
            verify(action, times(1)).invoke()
        }
    }
}