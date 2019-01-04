package com.manueldidonna.virtualscheduler

typealias Routine = suspend VirtualScheduler.() -> Unit
typealias ScheduleBlock = suspend ScheduleContext.() -> Unit
typealias ChildrenBlock = suspend ChildrenContext.() -> Unit
typealias AnonymousBlock = suspend () -> Unit
