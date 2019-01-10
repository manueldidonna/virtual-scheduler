package com.manueldidonna.virtualscheduler

typealias Routine = suspend VirtualScheduler.() -> Unit
typealias ScheduleBlock = suspend ScheduleContext.() -> Unit
typealias WrapperBlock = WrapperContext.() -> Unit
typealias NoContextBlock = () -> Unit