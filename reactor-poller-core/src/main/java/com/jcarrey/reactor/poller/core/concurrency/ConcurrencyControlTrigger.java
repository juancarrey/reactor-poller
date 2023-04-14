package com.jcarrey.reactor.poller.core.concurrency;

@FunctionalInterface
public interface ConcurrencyControlTrigger<T> {
    static <T> ConcurrencyControlTrigger<T> never() {
        return __ -> ConcurrencyControlOperation.Noop;
    }

    /**
     * Returns true if the system should adapt concurrency for the given response
     * @param pollerResponse The response from the poller
     * @return Which action should be taken using appropriate {@link ConcurrencyControlFunction}
     */
    ConcurrencyControlOperation calculate(T pollerResponse);
}
