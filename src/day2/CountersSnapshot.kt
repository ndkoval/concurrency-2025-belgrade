package day2

import java.util.concurrent.atomic.*

class CountersSnapshot {
    val counter1 = AtomicLong(0)
    val counter2 = AtomicLong(0)
    val counter3 = AtomicLong(0)

    fun incrementCounter1() = counter1.getAndIncrement()
    fun incrementCounter2() = counter2.getAndIncrement()
    fun incrementCounter3() = counter3.getAndIncrement()

    fun countersSnapshot(): Triple<Long, Long, Long> {
        while (true) {
            val counter1Snapshot = counter1.get()
            val counter2Snapshot = counter2.get()
            val counter3Snapshot = counter3.get()
            if (counter1.get() == counter1Snapshot &&
                counter2.get() == counter2Snapshot &&
                counter3.get() == counter3Snapshot
            ) {
                return Triple(counter1Snapshot, counter2Snapshot, counter3Snapshot)
            }
        }
    }
}