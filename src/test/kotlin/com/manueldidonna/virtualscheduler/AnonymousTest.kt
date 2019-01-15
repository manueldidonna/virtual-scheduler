package com.manueldidonna.virtualscheduler

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test


class AnonymousTest : VirtualSchedulerTest() {

    @Test
    fun anonymousSurvivesTagDeletion() = runBlocking {
        // arrange
        val action: () -> Unit = mock()

        // trigger
        vs.schedule(10L, commonTag) {
            // not executed
            wait(10L) { action() }
            // anonymous ignores the scheduleTag deletion
            anonymous { action() }
        }.schedule(19L, "traitor") {
            vs.discardTag(commonTag)
        }.run()

        // validation
        verify(action, times(1)).invoke()
    }

    @Test
    fun waitOperatorWithinAnonymous() = runBlocking {
        // arrange
        val action: () -> Unit = mock()

        // trigger
        vs.schedule(tag = commonTag) {
            alive {
                vs.discardTag(scheduleTag)
                anonymous {
                    // wait parent is anonymous and not schedule
                    wait(1L) { action() }
                }
            }
            dead {
                anonymous {
                    // children overrides anonymous tag (that can't be discarded) with discarded scheduleTag
                    children(scheduleTag) { action() }
                }
            }
        }.run()

        // trigger
        verify(action, times(1)).invoke()
    }
}