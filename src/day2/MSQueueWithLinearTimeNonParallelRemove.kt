@file:Suppress("FoldInitializerAndIfToElvis", "DuplicatedCode")

package day2

import day2.MSQueueWithOnlyLogicalRemove.Node
import java.util.concurrent.atomic.*

class MSQueueWithLinearTimeNonParallelRemove<E> : QueueWithRemove<E> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val node = Node(element)
            val curTail = tail.get()
            val success = curTail.next.compareAndSet(null, node)
            tail.compareAndSet(curTail, if (success) node else curTail.next.get())
            if (curTail.extractedOrRemoved) curTail.remove()
            if (success) return
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val curHead = head.get()
            val curHeadNext = curHead.next.get()
            if (curHeadNext == null) return null
            if (head.compareAndSet(curHead, curHeadNext)) {
                if (curHeadNext.remove()) {
                    return curHeadNext.element
                }
            }
        }
    }

    override fun remove(element: E): Boolean {
        // Traverse the linked list, searching the specified
        // element. Try to remove the corresponding node if found.
        // DO NOT CHANGE THIS CODE.
        var node = head.get()
        while (true) {
            val next = node.next.get()
            if (next == null) return false
            node = next
            if (node.element == element && node.remove()) return true
        }
    }

    /**
     * This is an internal function for tests.
     * DO NOT CHANGE THIS CODE.
     */
    override fun validate() {
        check(tail.get().next.get() == null) {
            "tail.next must be null"
        }
        var node = head.get()
        // Traverse the linked list
        while (true) {
            if (node !== head.get() && node !== tail.get()) {
                check(!node.extractedOrRemoved) {
                    "Removed node with element ${node.element} found in the middle of this queue"
                }
            }
            node = node.next.get() ?: break
        }
    }

    // TODO: Node is an inner class for accessing `head` in `remove()`
    private inner class Node(
        var element: E?
    ) {
        val next = AtomicReference<Node?>(null)

        /**
         * TODO: Both [dequeue] and [remove] should mark
         * TODO: nodes as "extracted or removed".
         */
        private val _extractedOrRemoved = AtomicBoolean(false)
        val extractedOrRemoved
            get() =
                _extractedOrRemoved.get()

        fun markExtractedOrRemoved(): Boolean =
            _extractedOrRemoved.compareAndSet(false, true)

        /**
         * Removes this node from the queue structure.
         * Returns `true` if this node was successfully
         * removed, or `false` if it has already been
         * removed by [remove] or extracted by [dequeue].
         */
        fun remove(): Boolean {
            val removed = markExtractedOrRemoved()
            if (this != tail.get() && this != head.get()) {
                val curPrev = findPrev()
                val curNext = next.get()
                curPrev?.next?.set(curNext)
            }
            return removed
        }

        private fun findPrev(): Node? {
            var node = head.get()
            while (node.next.get() != this) {
                node = node.next.get() ?: return null
            }
            return node
        }
    }
}
