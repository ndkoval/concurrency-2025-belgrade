@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class DoubleCompareSingleSetOnLockedState<E : Any>(initialValue: E) : DoubleCompareSingleSet<E> {
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
                curA === LOCKED -> continue
                else -> return curA as E
            }
        }
    }

    override fun dcss(
        expectedA: E, updateA: E, expectedB: E
    ): Boolean {
        while (true) {
            val curA = a.getAndSet(LOCKED)
            when (curA) {
                LOCKED -> continue

                expectedA -> {
                    if (b.get() === expectedB) {
                        a.set(updateA)
                        return true
                    } else {
                        a.set(expectedA)
                        return false
                    }
                }

                else -> {
                    a.set(curA)
                    return false
                }
            }
        }
    }

    override fun setB(value: E) {
        b.set(value)
    }

    override fun getB(): E {
        return b.get()
    }
}

// TODO: Store me in `a` to indicate that the reference is "locked".
// TODO: Other operations should wait in an active loop until the
// TODO: value changes.
private val LOCKED = "Locked"
