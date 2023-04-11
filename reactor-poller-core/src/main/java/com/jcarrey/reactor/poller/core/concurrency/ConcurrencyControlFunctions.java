package com.jcarrey.reactor.poller.core.concurrency;

public class ConcurrencyControlFunctions {

    /**
     * Linearly escale the concurrency by a power
     * If amount = 2, effectively add up concurrency by 2 each time (For scale-ups, scale-downs will reduce the amount similarly)
     *  When concurrency=1 -&gt; increase +2 resulting in 3
     *  When concurrency=2 -&gt; increase +2 resulting in 4
     *  When concurrency=3 -&gt; increase +2 resulting in 5
     *  When concurrency=4 -&gt; increase +2 resulting in 6
     * @param amount The amount to increase each time for scale-ups &amp; scale-down
     * @return a ConcurrencyControlFunction to be used in either scale-up or scale-down
     */
    public static ConcurrencyControlFunction linear(int amount) {
        return new LinearConcurrencyControlFn(amount);
    }

    /**
     * Exponentially escale the concurrency by a power
     * If power = 2, effectively multiply concurrency by 2 each time (For scale-ups, scale-downs will reduce the amount similarly)
     *  When concurrency=1 -&gt; increase +1 resulting in 2
     *  When concurrency=2 -&gt; increase +2 resulting in 4
     *  When concurrency=3 -&gt; increase +3 resulting in 6
     *  When concurrency=4 -&gt; increase +4 resulting in 8
     * @param pow The multiplier for exponentially increase concurrency
     * @return a ConcurrencyControlFunction to be used in either scale-up or scale-down
     */
    public static ConcurrencyControlFunction exponential(int pow) {
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
