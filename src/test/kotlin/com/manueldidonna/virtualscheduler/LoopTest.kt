package com.manueldidonna.virtualscheduler

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LoopTest : VirtualSchedulerTest() {

    @Test
    fun actionExecutesUntilSchedulerDeletion() = runBlocking {
        // arrange
        val action: () -> Unit = mock()

        // trigger
        vs.schedule(tag = commonTag) {
            while (isAlive()) {
                wait(5L) { action() }
            }
        }.schedule(21L, tag = "traitor") {
            vs.discardTag(commonTag)
        }.run()

        // validation
        verify(action, times(4)).invoke()
    }
}