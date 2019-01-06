package com.manueldidonna.virtualscheduler

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
    fun clearTheScheduler() {
        runBlocking {
            // arrange
            val commonTag = "schedule"
            val action: () -> Unit = mock()
            val anonAction: () -> Unit = mock()

            // trigger
            vs.schedule(tag = commonTag) {
                children {
                    wait(300L) { action() } // 300L - invoked
                    // 350L % 400L invoked.
                    // the scheduler is cleared before action is invoked Before 450L is invoked
                    for (i in 0 until 3)
                        wait(50L) { action() }
                }
                anonymous { anonAction() }
            }.schedule(449L, tag = commonTag) {
                vs.clear() // 449L because virtual scheduler priority system is wonderful
            }.run()

            // validation
            verify(action, times(3)).invoke()
            verify(anonAction, times(0)).invoke()
        }
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
                // (tagToDiscard, 500) is discarded before the execution of its block
                vs.discardTag(tagToDiscard)
            }.schedule(600L, tag = "restoreTag") {
                // (tagToDiscard, 700) is restored before the execution of its block
                vs.restoreTag(tagToDiscard)
            }.run() // the scheduler will wait anyway for 700 millis.
            // Validation checks are executed before and after the time processing, not during the process

            // validation
            verify(actionToInvoke, times(1)).invoke()
            verify(actionToNotInvoke, times(0)).invoke()
        }
    }

    @Test
    fun tagDiscardedAndThenRestored() {
        runBlocking {
            // arrange
            val tagToDiscard = "schedule"
            val actionToNotInvoke: () -> Unit = mock()
            val actionToInvoke: () -> Unit = mock()

            // trigger
            vs.schedule(tag = tagToDiscard) {
                children {
                    // "traitor" schedule happen before this 'wait' block and it discards the tag
                    wait(100L) {
                        actionToNotInvoke()
                    }
                }
                // this children restores the internalTag.
                // It overrides the schedule tag so it isn't discarded with the parent schedule
                children("foreigner") {
                    alive { vs.restoreTag(scheduleTag) }
                }
                // alive invokes its block because of "foreigner" restoration
                alive { actionToInvoke() }
            }.schedule(70L, "traitor") {
                vs.discardTag(tagToDiscard)
            }.run()

            // validation
            verify(actionToInvoke, times(1)).invoke()
            verify(actionToNotInvoke, times(0)).invoke()
        }
    }

    @Test
    fun anonymousBlocksSurviveScheduleDestruction() {
        runBlocking {
            // arrange
            val tagToDiscard = "schedule"
            val action: () -> Unit = mock()

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

    @Test
    fun correctOrderOfExecution() {
        runBlocking {
            // arrange
            val actions = Array(9) { mock<() -> Unit>() }
            val commonTag = "schedule"

            // trigger
            vs.schedule(500L, tag = commonTag) {
                alive { actions[1].invoke() } // 500
                children {
                    wait(100L) {
                        // wait4 is executed after anonymous3.
                        // each children create a suspension point of 0 millis.
                        // Thanks to this sp, the scheduler jump to wait2
                        // and it creates anonymous3 suspension point
                        // with the same priority of the next wait4 sp
                        // but the order of creation prevails.
                        actions[4].invoke() // 600
                    }
                    wait(200L) {
                        actions[5].invoke() // 800
                    }
                    alive { actions[6].invoke() } // 800
                    wait(300L) {
                        actions[8].invoke() // 1100
                    }
                }
            }.schedule(200L, tag = commonTag) {
                alive { actions[0].invoke() } // 200
                children {
                    wait(300L) {
                        actions[2].invoke() // 500
                    }
                }
                anonymous(100L) {
                    actions[3].invoke() // 600
                }
            }.schedule(1100L, commonTag) {
                alive { actions[7].invoke() }
            }.run()

            // validation
            inOrder(*actions) {
                verify(actions[0]).invoke()
                verify(actions[1]).invoke()
                verify(actions[2]).invoke()
                verify(actions[3]).invoke()
                verify(actions[4]).invoke()
                verify(actions[5]).invoke()
                verify(actions[6]).invoke()
                verify(actions[7]).invoke()
                verify(actions[8]).invoke()
            }
        }
    }
}