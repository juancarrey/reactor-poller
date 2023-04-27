package com.jcarrey.reactor.poller.core.concurrency;

class ExponentialConcurrencyControlFn implements ConcurrencyControlFunction {

    private final double power;

    public ExponentialConcurrencyControlFn(double power) {
        if (power <= 1d) {
            throw new IllegalArgumentException("Power must be > 1");
        }
        this.power = power;
    }

    @Override
    public double calculateDelta(double currentConcurrency, ConcurrencyControlOperation operation) {
        // When power = 2, each concurrent worker needs to add +1
        // When power = 3, each concurrent worker needs to add +2
        // When power = 4, each concurrent worker needs to add +3
        return power - 1;
    }
}
