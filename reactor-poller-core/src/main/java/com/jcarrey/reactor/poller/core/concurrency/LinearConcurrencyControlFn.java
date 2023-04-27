package com.jcarrey.reactor.poller.core.concurrency;

class LinearConcurrencyControlFn implements ConcurrencyControlFunction {

    private final double amount;

    public LinearConcurrencyControlFn() {
        this(1);
    }

    public LinearConcurrencyControlFn(double amount) {
        this.amount = amount;
    }

    @Override
    public double calculateDelta(double currentConcurrency, ConcurrencyControlOperation operation) {
        return amount / Math.round(currentConcurrency);
    }
}
