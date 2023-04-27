package com.jcarrey.reactor.poller.core.concurrency;

public class ConcurrencyControlFunctions {

    /**
     * Linearly escale the concurrency by an amount on each complete cycle
     *  When concurrency=1 -&gt; increase +1 resulting in 2
     *  When concurrency=2 -&gt; increase +0.5 (each) resulting in +1
     *  When concurrency=3 -&gt; increase +0.33 (each) resulting in +1
     *  When concurrency=4 -&gt; increase +0.25 (each) resulting in +1
     * @param amount The amount to increase each time for scale-ups &amp; scale-down
     * @return a ConcurrencyControlFunction to be used in either scale-up or scale-down
     */
    public static ConcurrencyControlFunction linear(double amount) {
        return new LinearConcurrencyControlFn(amount);
    }

    /**
     * Exponentially escale the concurrency by a power
     *  If power = 2, effectively multiply concurrency by 2 each complete cycle
     *  When concurrency=1 -&gt; increase +1 (each) resulting in +1
     *  When concurrency=2 -&gt; increase +1 (each) resulting in +2
     *  When concurrency=3 -&gt; increase +1 (each) resulting in +3
     *  When concurrency=4 -&gt; increase +1 (each) resulting in +4
     * @param pow The multiplier for exponentially increase concurrency
     * @return a ConcurrencyControlFunction to be used in either scale-up or scale-down
     */
    public static ConcurrencyControlFunction exponential(double pow) {
        return new ExponentialConcurrencyControlFn(pow);
    }

    /**
     * Directly scale to either MAX on scale-ups or MIN on scale-downs
     * @return a ConcurrencyControlFunction to be used in either scale-up or scale-down
     */
    public static ConcurrencyControlFunction max() {
        return new MaxConcurrencyControlFn();
    }
}
