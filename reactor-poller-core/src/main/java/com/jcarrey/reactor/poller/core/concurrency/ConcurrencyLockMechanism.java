package com.jcarrey.reactor.poller.core.concurrency;

public enum ConcurrencyLockMechanism {
    /**
     * Uses {@link java.util.concurrent.locks.ReentrantLock} for updating
     * the concurrency.
     */
    Pessimistic,
    /**
     * No locking is performed, many concurrent workers can update
     */
    None
}
