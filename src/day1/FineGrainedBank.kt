@file:Suppress("DuplicatedCode")

package day1

import day1.Bank.Companion.MAX_AMOUNT
import java.util.concurrent.locks.*

class FineGrainedBank(accountsNumber: Int) : Bank {
    private val accounts: Array<Account> = Array(accountsNumber) { Account() }

    override fun getAmount(id: Int): Long {
        val lock = accounts[id].lock
        lock.lock()
        try {
            val account = accounts[id]
            return account.amount
        } finally {
            lock.unlock()
        }
    }

    override fun deposit(id: Int, amount: Long): Long {
        val lock = accounts[id].lock
        lock.lock()
        try {
            require(amount > 0) { "Invalid amount: $amount" }
            val account = accounts[id]
            check(!(amount > MAX_AMOUNT || account.amount + amount > MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            return account.amount
        } finally {
            lock.unlock()
        }
    }

    override fun withdraw(id: Int, amount: Long): Long {
        val lock = accounts[id].lock
        lock.lock()
        try {
            require(amount > 0) { "Invalid amount: $amount" }
            val account = accounts[id]
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            return account.amount
        } finally {
            lock.unlock()
        }
    }

    override fun transfer(fromId: Int, toId: Int, amount: Long) {
        val (id1, id2) = listOf(fromId, toId).sorted()
        val lock1 = accounts[id1].lock
        val lock2 = accounts[id2].lock
        lock1.lock()
        lock2.lock()
        try {
            require(amount > 0) { "Invalid amount: $amount" }
            require(fromId != toId) { "fromId == toId" }
            val from = accounts[fromId]
            val to = accounts[toId]
            check(amount <= from.amount) { "Underflow" }
            check(!(amount > MAX_AMOUNT || to.amount + amount > MAX_AMOUNT)) { "Overflow" }
            from.amount -= amount
            to.amount += amount
        } finally {
            lock1.unlock()
            lock2.unlock()
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0

        /**
         * TODO: use this mutex to protect the account data.
         */
        val lock = ReentrantLock()
    }
}