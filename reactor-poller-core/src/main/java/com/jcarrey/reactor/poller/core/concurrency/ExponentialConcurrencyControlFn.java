package com.jcarrey.reactor.poller.core.concurrency;

class ExponentialConcurrencyControlFn implements ConcurrencyControlFunction {

    private final int power;

    public ExponentialConcurrencyControlFn() {
        this(2);
    }

    public ExponentialConcurrencyControlFn(int power) {
        this.power = power;
    }

    @Override
    public int calculateDelta(int currentConcurrency, ConcurrencyControlOperation operation) {
        if (operation == ConcurrencyControlOperation.ScaleDown) {
            return currentConcurrency / power;
        } else {
            return currentConcurrency * power - currentConcurrency;
        }
    }
}
