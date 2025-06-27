package day4

import day1.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

open class FlatCombiningQueue<E : Any> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    override fun enqueue(element: E): Unit = enqueueOrDequeue(task = element) {
        queue.add(element)
    }

    override fun dequeue(): E? = enqueueOrDequeue(task = Dequeue) {
        queue.removeFirstOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <R> enqueueOrDequeue(task: Any, operation: () -> R): R {
        if (tryAcquireLock()) {
            val operationResult = operation()
            helpOthers()
            releaseLock()
            return operationResult
        }
        var cellIndex: Int
        while (true) {
            cellIndex = randomCellIndex()
            if (tasksForCombiner.compareAndSet(cellIndex, null, task)) {
                break
            }
        }
        while (true) {
            if (tasksForCombiner[cellIndex] is Result<*>) {
                val result = tasksForCombiner[cellIndex] as Result<R>
                tasksForCombiner.set(cellIndex, null)
                return result.value
            }
            if (tryAcquireLock()) {
                if (tasksForCombiner[cellIndex] is Result<*>) {
                    val result = tasksForCombiner[cellIndex] as Result<R>
                    tasksForCombiner.set(cellIndex, null)
                    releaseLock()
                    return result.value
                }
                tasksForCombiner.set(cellIndex, null)
                val operationResult = operation()
                helpOthers()
                releaseLock()
                return operationResult
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun helpOthers() {
        for (i in 0..<TASKS_FOR_COMBINER_SIZE) {
            val cur = tasksForCombiner[i]
            when (cur) {
                null -> continue
                is Result<*> -> continue
                Dequeue -> tasksForCombiner.set(i, Result(queue.removeFirstOrNull()))
                else -> tasksForCombiner.set(i, Result(queue.add(cur as E)))
            }
        }
    }

    private fun tryAcquireLock(): Boolean {
        return combinerLock.compareAndSet(false, true)
    }

    open fun releaseLock() {
        combinerLock.set(false)
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

// TODO: Put this token in `tasksForCombiner` for dequeue().
// TODO: enqueue()-s should put the inserting element.
private object Dequeue

// TODO: Put the result wrapped with `Result` when the operation in `tasksForCombiner` is processed.
private class Result<V>(
    val value: V
)
