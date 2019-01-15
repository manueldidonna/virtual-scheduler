package com.manueldidonna.virtualscheduler

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TagDeletionTest : VirtualSchedulerTest() {

    @Test
    fun scheduleTagDiscardedThenRestored() = runBlocking {
        // arrange
        val actionToNotInvoke: () -> Unit = mock()
        val actionToInvoke: () -> Unit = mock()

        // trigger
        // Validation checks are executed before and after a suspension point
        vs.schedule(5L, tag = commonTag) {
            actionToNotInvoke()  // (tagToDiscard, 5) is registered as suspension point
        }.schedule(7L, tag = commonTag) {
            actionToInvoke() // (tagToDiscard, 7) is registered as suspension point
        }.schedule(2L, tag = "discardTag") {
            // (tagToDiscard, 5) is discarded before the execution of its block
            vs.discardTag(commonTag)
        }.schedule(6L, tag = "restoreTag") {
            // (tagToDiscard, 7) is restored before the execution of its block
            vs.restoreTag(commonTag)
        }.run()

        // validation
        verify(actionToInvoke, times(1)).invoke()
        verify(actionToNotInvoke, times(0)).invoke()
    }

    @Test
    fun tagDiscardedAndThenRestored() = runBlocking {
        // arrange
        val actionToNotInvoke: () -> Unit = mock()
        val actionToInvoke: () -> Unit = mock()

        // trigger
        vs.schedule(tag = commonTag) {
            yield()
            // "traitor" schedule happen before this 'wait' block and it discards the tag
            wait(10L) {
                actionToNotInvoke()
            }

            // foreigner children restores the schedule tag
            // It overrides the schedule tag so it isn't discarded with the parent schedule
            children("foreigner") {
                alive { vs.restoreTag(scheduleTag) }
            }

            // alive invokes its block because of "foreigner" restoration
            alive { actionToInvoke() }
        }.schedule(7L, "traitor") {
            vs.discardTag(commonTag)
        }.run()

        // validation
        verify(actionToInvoke, times(1)).invoke()
        verify(actionToNotInvoke, times(0)).invoke()
    }

    @Test
    fun tagRestorationWithinDeadOperator() = runBlocking {
        // arrange
        val action: () -> Unit = mock()

        // trigger
        vs.schedule(tag = "schedule") {
            vs.discardTag(scheduleTag)
            dead {
                vs.restoreTag(scheduleTag)
                alive { action() }
            }
        }.run()

        // validation
        verify(action, times(1)).invoke()
    }

    @Test
    fun aliveIsNotEvaluatedAfterTagDeletion() = runBlocking {
        // arrange
        val actionToInvoke: () -> Unit = mock()
        val actionToNotInvoke: () -> Unit = mock()

        // trigger
        vs.schedule(tag = "schedule") {
            vs.discardTag(scheduleTag)
            alive { actionToNotInvoke() }
            dead { actionToInvoke() }
        }.run()

        // validation
        verify(actionToInvoke, times(1)).invoke()
        verify(actionToNotInvoke, times(0)).invoke()
    }
}