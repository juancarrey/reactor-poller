package com.jcarrey.reactor.poller.core.concurrency;

@FunctionalInterface
public interface ConcurrencyControlFunction {
    /**
     * Returns the amount to be scaled up/down
     * @param currentConcurrency The current concurrency of the poller which might be used to calculate the delta
     * @param operation The operation that is going to happen, so it may differentiate between going up or down
     * @return The delta that will be added up (on scale-up) or substracted (on scale-down) from currentConcurrency
     *         keeping min and max concurrency settings.
     */
    int calculateDelta(int currentConcurrency, ConcurrencyControlOperation operation);
}
