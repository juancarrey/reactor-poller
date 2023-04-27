package com.jcarrey.reactor.poller.core;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlFunction;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlFunctions;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlTrigger;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyLockMechanism;
import lombok.Builder;
import lombok.Getter;
import reactor.util.annotation.Nullable;

import java.util.Optional;

@Getter
@Builder
public class ConcurrencyControlOptions<T> {
    /**
     * How many concurrent polls may happen at a given time based on the
     * request(N) amount from downstream
     */
    private final double initialConcurrency;
    /**
     * The minimum concurrent polls that may happen at any given time, must be >= 1.
     * We recommend setting this to 1.
     * Even if scale-down events happen, concurrent polls won't be lower than this number.
     */
    private final double minConcurrency;
    /**
     * The maximum amount of concurrent polls that may happen at any given time.
     * Even if scale-up events happen, concurrent polls won't be higher than this number.
     */
    private final double maxConcurrency;
    /**
     * Defines when to trigger a scale-up or scale-down based on the response from the poller
     * If there is no data, one would scale down, and when there is, it may scale up.
     */
    private final ConcurrencyControlTrigger<T> strategy;
    /**
     * How much to scale up the number of concurrent polling for each of the poll responses that triggered
     * a scale-up event.
     */
    private final ConcurrencyControlFunction scaleUpFn;
    /**
     * How much to scale down the number of concurrent polling for each of the poll responses that triggered
     * a scale-down event.
     */
    private final ConcurrencyControlFunction scaleDownFn;
    /**
     * The locking mechanism to update the concurrent number of pollers, as this is triggered by different concurrent pollers,
     * they may have race conditions. In most scenarios this should probably be just fine, and even more appropriate in some cases.
     * It will cause scale-up and downs to be slower, as they will compete to update with +1.
     *
     * With pessimistic lock, what happens is that linear scale-ups with +1, in reality may cause a *2, behaving like exponential,
     * as all pollers received a response that triggers a scale up event.
     */
    private final ConcurrencyLockMechanism lockMechanism;

    public ConcurrencyControlOptions(
            double initialConcurrency,
            double minConcurrency,
            double maxConcurrency,
            @Nullable ConcurrencyControlTrigger<T> strategy,
            @Nullable ConcurrencyControlFunction scaleUpFn,
            @Nullable ConcurrencyControlFunction scaleDownFn,
            @Nullable ConcurrencyLockMechanism lockMechanism
    ) {
        if (minConcurrency < 1) {
            throw new IllegalArgumentException("minConcurrency must be >= 1");
        }
        if (initialConcurrency > maxConcurrency || initialConcurrency < minConcurrency) {
            throw new IllegalArgumentException("initialConcurrency must be between min and max");
        }
        this.initialConcurrency = initialConcurrency;
        this.minConcurrency = minConcurrency;
        this.maxConcurrency = maxConcurrency;
        this.strategy = Optional.ofNullable(strategy).orElse(ConcurrencyControlTrigger.never());
        this.scaleUpFn = Optional.ofNullable(scaleUpFn).orElse(ConcurrencyControlFunctions.linear(1));
        this.scaleDownFn = Optional.ofNullable(scaleDownFn).orElse(ConcurrencyControlFunctions.linear(1));
        this.lockMechanism = Optional.ofNullable(lockMechanism).orElse(ConcurrencyLockMechanism.None);
    }
}
