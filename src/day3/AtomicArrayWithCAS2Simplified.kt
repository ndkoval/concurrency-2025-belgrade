@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2Simplified.Status.*
import java.util.concurrent.atomic.*


// This implementation never stores `null` values.
class AtomicArrayWithCAS2Simplified<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        val cur = array[index]
        if (cur is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor) {
            return when (cur.status.get()) {
                AtomicArrayWithCAS2SingleWriter.Status.SUCCESS -> if (index == cur.index1) cur.update1 else cur.update2
                else -> if (index == cur.index1) cur.expected1 else cur.expected2
            } as E
        }
        return cur as E

        // TODO: the cell can store CAS2Descriptor
        return array[index] as E
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        val descriptor = CAS2Descriptor(
            index1 = index1, expected1 = expected1, update1 = update1,
            index2 = index2, expected2 = expected2, update2 = update2
        )
        descriptor.apply()
        return descriptor.status.get() === SUCCESS
    }

    inner class CAS2Descriptor(
        private val index1: Int,
        private val expected1: E,
        private val update1: E,
        private val index2: Int,
        private val expected2: E,
        private val update2: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            while (true) {
                val cur1 = array[index1]
                val cur2 = array[index2]
                when {
                    cur1 is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor &&
                            cur2 is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor -> {
                        cur1.process()
                        cur2.process()
                    }

                    cur1 is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor &&
                            cur2 == expected2 -> {
                        cur1.process()
                        if (!array.compareAndSet(index2, expected2, this)) continue
                        process()
                    }

                    cur1 == expected1 &&
                            cur2 is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor -> {
                        cur2.process()
                        if (!array.compareAndSet(index1, expected1, this)) continue
                        process()
                    }

                    cur1 == expected1 && cur2 == expected2 -> {
                        if (!array.compareAndSet(index1, expected1, this)) continue
                        if (!array.compareAndSet(index2, expected2, this)) continue
                        process()
                        return
                    }

                    else -> {
                        status.compareAndSet(
                            Status.UNDECIDED,
                            Status.FAILED
                        )
                        return
                    }
                }
            }

            // TODO: Install the descriptor, update the status, and update the cells;
            // TODO: create functions for each of these three phases.
        }

        fun process() {
            applyLogically()
            applyPhysically()
        }

        private fun applyLogically() {
            status.compareAndSet(
                Status.UNDECIDED,
                Status.SUCCESS
            )
        }

        private fun applyPhysically() {
            if (status.get() == Status.SUCCESS) {
                array.compareAndSet(index1, this, update1)
                array.compareAndSet(index2, this, update2)
            } else {
                array.compareAndSet(index1, this, expected1)
                array.compareAndSet(index2, this, expected2)
            }
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}