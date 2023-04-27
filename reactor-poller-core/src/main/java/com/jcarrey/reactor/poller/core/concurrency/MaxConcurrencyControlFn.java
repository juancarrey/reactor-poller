package com.jcarrey.reactor.poller.core.concurrency;

class MaxConcurrencyControlFn implements ConcurrencyControlFunction {

    @Override
    public double calculateDelta(double currentConcurrency, ConcurrencyControlOperation operation) {
        return Double.MAX_VALUE;
    }
}
