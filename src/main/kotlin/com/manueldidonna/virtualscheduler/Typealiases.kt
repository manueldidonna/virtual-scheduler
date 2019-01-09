package com.manueldidonna.virtualscheduler

typealias Routine = suspend VirtualScheduler.() -> Unit
typealias ScheduleBlock = suspend ScheduleContext.() -> Unit
typealias ChildrenBlock = suspend WrapperContext.() -> Unit
typealias AnonymousBlock = suspend WrapperContext.() -> Unit
