package com.manueldidonna.virtualscheduler

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GeneralBehaviorTest : VirtualSchedulerTest() {

    @Test
    fun clearTheSchedulerInterruptTheScheduling() = runBlocking {
        // arrange
        val action: () -> Unit = mock()

        // trigger
        vs.schedule(tag = commonTag) {
            // 3L - invoked
            wait(3L) { action() }

            // 8K & 13L - invoked / 18L - discarded
            for (i in 0 until 3)
                wait(5L) { action() }

            // this code never executes
            anonymous { action() }
        }.schedule(18L, tag = "secondSchedule") {
            // secondSchedule has an higher priority of the fourth wait statement
            // the scheduler is cleared before the fourth action is invoked
            vs.clear()
        }.run()

        // validation
        verify(action, times(3)).invoke()
    }

    @Test
    fun scheduleShouldStartEarlierButNotAddedInTime() = runBlocking {
        // arrange
        val action1: () -> Unit = mock()
        val action2: () -> Unit = mock()

        // trigger
        vs.schedule(50L, "firstSchedule") {
            alive { action1() }
        }
        // secondSchedule should begin before firstSchedule,
        // but when secondSchedule is added, the first is already captured
        // by the scheduler in the .run() while (next != null) loop
        launch {
            delay(30L)
            vs.schedule(6L, "secondSchedule") {
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

    @Test
    fun correctOrderOfExecution() = runBlocking {
        // arrange
        val actions = Array(9) { mock<() -> Unit>() }
        val commonTag = "schedule"

        // trigger
        vs.schedule(50L, tag = commonTag) {
            alive { actions[1].invoke() } // 500
            children {
                wait(10L) {
                    // wait4 is executed after anonymous3.
                    // each children create a suspension point of 0 millis.
                    // Thanks to this sp, the scheduler jump to wait2
                    // and it creates anonymous3 suspension point
                    // with the same priority of the next wait4 sp
                    // but the order of creation prevails.
                    actions[4].invoke() // 600
                }
                wait(20L) {
                    actions[5].invoke() // 800
                }
                alive { actions[6].invoke() } // 800
                wait(30L) {
                    actions[8].invoke() // 1100
                }
            }
        }.schedule(20L, tag = commonTag) {
            alive { actions[0].invoke() } // 200
            children {
                wait(30L) {
                    actions[2].invoke() // 500
                }
            }
            anonymous(10L) {
                actions[3].invoke() // 600
            }
        }.schedule(110L, commonTag) {
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