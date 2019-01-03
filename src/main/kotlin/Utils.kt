import kotlinx.coroutines.*

fun VirtualScheduler.run(dispatcher: CoroutineDispatcher): Job {
    return GlobalScope.launch(dispatcher) { run() }
}

fun VirtualScheduler.run(scope: CoroutineScope): Job {
    return scope.launch { run() }
}