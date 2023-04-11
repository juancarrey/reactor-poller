package com.jcarrey.reactor.poller.core.concurrency;

class MaxConcurrencyControlFn implements ConcurrencyControlFunction {

    @Override
    public int calculateDelta(int currentConcurrency, ConcurrencyControlOperation operation) {
        return Integer.MAX_VALUE;
    }
}
