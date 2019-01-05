# VirtualScheduler

A library to schedule actions according to a temporal order. 

- Written in kotlin (depends on [kotlinx.coroutines 1.1.0](https://github.com/Kotlin/kotlinx.coroutines))
- Based on Continuation and Coroutines concepts
- Nice, simple and extensible DSL
- Suitable to write readable code
- Easy cancellable actions **(easier than plain coroutines)**

## Current Version

```
virtual_scheduler_version = X.Y.Z
```

## Gradle
#### Jcenter

Check that you have the jcenter repository.

```
// Add Jcenter to your repositories if needed
repositories {
	jcenter()    
}
```
#### Dependencies

```
implementation
```
## Quick Start
The entire library can be used through an instance of `VirtualScheduler` and several extension functions.

```
val vs = VirtualScheduler()
```

### Create a schedule
Each schedule returns an instance of `VirtualScheduler`

```
vs.schedule(startDelayInMillis = 100L, tag = "first"){
    doSomething() // after 100 milliseconds
}
```
### Combine more schedules
```
vs.schedule(100L, "first"){
    doSomething() // after 100 milliseconds
}.schedule(300L, "second"){
    // after 200 milliseconds from 'doSomething()' end
    doSomethingAgain()
}
```
### Start scheduling
Use `suspend fun VirtualScheduler.run()` to start scheduling in the current thread or pass a **CoroutineDispatcher/CoroutineScope** as a parameter

### Discard a schedule
[Here](https://github.com/manueldidonna/VirtualScheduler/blob/2e8e04c1a5bf82e728683da7230acf4899ca382b/src/test/kotlin/com/manueldidonna/virtualscheduler/VirtualSchedulerTest.kt#L79-L102) an advanced example.

```
vs.discardTag(tag = "first") // tag used for the schedule 
```
A schedule can not be deleted while it is being processed by the scheduler.

Use **children**, **wait** and **alive** to still abort actions within schedule. They are lazy evaluated.

## Children
Children wraps actions under the same tag allowing them to be aborted together.
**It's lazy evaluated within a schedule.**
> Nested children aren't yet fully supported. If their tag is omitted, children will always inherit it from the schedule

```
vs.schedule(tag = commonTag) {
    doSomething()
    // if tag is omitted, children inherits it from parent schedule
    children(tag = "foreigner") {
        doSomethingElse()
    }
}
```
## Wait
Wait creates a suspension point delayed by **@parameter:delayInMillis**.

**It's lazy evaluated within a schedule.**
> Wait is **only** allowed within children
```
vs.schedule(tag = commonTag) {
    children {
        wait(300L) { action() } // 300L - invoked
        // the scheduler is cleared before the third cycle is invoked
        for (i in 0 until 3)
            wait(50L) { action() } // two out of three actions have been invoked
    }
}.schedule(449L, tag = commonTag) {
    // 449L because virtual scheduler priority system is wonderful
    vs.clear() // clear delete all the suspension points and stop the virtualscheduler
}.run()
```

## Alive
Alive checks if the receiver **(schedule or children)** is still active **(not discarded)**

**It's lazy evaluated within a schedule.**
> Alive doesn't suspend the routine
```
vs.restoreTag(tag = "foreigner")

/** within a schedule */
children("foreigner") {
    alive { vs.discardTag(scheduleTag) } // alive's receiver is foreigner children
}

// alive doesn't invoke its block because foreigner discard the schedule
alive { actionToInvoke() } // alive's receiver is the schedule

actionToNotInvoke() // error, this action will be invoked because there isn't any validity check
```