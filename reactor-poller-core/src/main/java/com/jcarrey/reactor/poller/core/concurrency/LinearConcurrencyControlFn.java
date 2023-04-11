package com.jcarrey.reactor.poller.core.concurrency;

class LinearConcurrencyControlFn implements ConcurrencyControlFunction {

    private final int amount;

    public LinearConcurrencyControlFn() {
        this(1);
    }

    public LinearConcurrencyControlFn(int amount) {
        this.amount = amount;
    }

    @Override
    public int calculateDelta(int currentConcurrency, ConcurrencyControlOperation operation) {
        return amount;
    }
}
