package day2

import day1.*
import java.util.concurrent.atomic.*

// TODO: Copy the code from `FAABasedQueueSimplified`
// TODO: and implement the infinite array on a linked list
// TODO: of fixed-size `Segment`s.
class FAABasedQueue<E> : Queue<E> {
    private val head: AtomicReference<Segment>
    private val tail: AtomicReference<Segment>

    init {
        val dummy = Segment(0)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    private val infiniteArray = AtomicReferenceArray<Any?>(1024) // conceptually infinite array
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    override fun enqueue(element: E) {
        while (true) {
            val i = enqIdx.getAndIncrement()
            if (infiniteArray.compareAndSet(i.toInt(), null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (deqIdx.get() >= enqIdx.get()) return null
            val i = deqIdx.getAndIncrement()
            if (infiniteArray.compareAndSet(i.toInt(), null, POISONED)) {
                continue
            }
            val result = infiniteArray.get(i.toInt()) as E
            infiniteArray.set(i.toInt(), null)
            return result
        }
    }
}

// TODO: Use me to construct a linked list of segments.
private class Segment(val id: Long) : AtomicReferenceArray<Any?>(SEGMENT_SIZE) {
    val next = AtomicReference<Segment?>(null)
}

// TODO: Use me to mark a cell poisoned.
private val POISONED = Any()

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2
