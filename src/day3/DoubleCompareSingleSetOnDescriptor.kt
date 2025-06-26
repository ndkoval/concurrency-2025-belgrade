@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.DoubleCompareSingleSetOnDescriptor
import day3.DoubleCompareSingleSetOnDescriptor.Status.*
import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class DoubleCompareSingleSetOnDescriptor<E : Any>(initialValue: E) : DoubleCompareSingleSet<E> {
    private val a = AtomicReference<Any>()
    private val b = AtomicReference<E>()

    init {
        a.set(initialValue)
        b.set(initialValue)
    }

    override fun getA(): E {
        while (true) {
            val curA = a.get()
            when {
                curA is DoubleCompareSingleSetOnDescriptor<*>.DcssDescriptor -> curA.process()
                else -> return curA as E
            }
        }
    }

    override fun dcss(expectedA: E, updateA: E, expectedB: E): Boolean {
        val descriptor = DcssDescriptor(expectedA, updateA, expectedB)
        descriptor.apply()
        return descriptor.status.get() == SUCCESS
    }

    private inner class DcssDescriptor(
        val expectedA: E, val updateA: E, val expectedB: E
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            while (true) {
                val curA = a.get()
                when {
                    curA is DoubleCompareSingleSetOnDescriptor<*>.DcssDescriptor -> curA.process()

                    curA === expectedA -> {
                        if (!a.compareAndSet(expectedA, this)) continue
                        process()
                        return
                    }

                    else -> return
                }
            }
        }

        fun process() {
            applyLogically()
            applyPhysically()
        }

        private fun applyLogically() {
            if (b.get() === expectedB) {
                status.compareAndSet(UNDECIDED, SUCCESS)
            } else {
                status.compareAndSet(UNDECIDED, FAILED)
            }
        }

        private fun applyPhysically() {
            if (status.get() == SUCCESS) {
                a.compareAndSet(this, updateA)
            } else {
                a.compareAndSet(this, expectedA)
            }
        }
    }

    override fun setB(value: E) {
        b.set(value)
    }

    override fun getB(): E {
        return b.get()
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}