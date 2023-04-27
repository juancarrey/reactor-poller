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
        if (operation == ConcurrencyControlOperation.ScaleDown) {
            return (1 - 1 / power);
        }

        return power - 1;
    }
}
