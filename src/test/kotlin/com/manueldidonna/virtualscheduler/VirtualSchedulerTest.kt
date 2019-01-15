package com.manueldidonna.virtualscheduler

import com.nhaarman.mockitokotlin2.spy
import org.junit.Before


abstract class VirtualSchedulerTest {

    protected val commonTag = "common-tag"

    protected lateinit var vs: VirtualScheduler

    @Before
    fun setup() {
        vs = spy(VirtualScheduler())
    }
}