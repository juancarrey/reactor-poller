package com.jcarrey.reactor.poller.core;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlFunction;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlTrigger;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder
public class ConcurrencyControlOptions<T> {
    private final int initialConcurrency;
    private final int minConcurrency;
    private final int maxConcurrency;
    private final ConcurrencyControlTrigger<T> strategy;
    private final ConcurrencyControlFunction scaleUpFn;
    private final ConcurrencyControlFunction scaleDownFn;

    public ConcurrencyControlOptions(
            int initialConcurrency,
            int minConcurrency,
            int maxConcurrency,
            ConcurrencyControlTrigger<T> strategy,
            ConcurrencyControlFunction scaleUpFn,
            ConcurrencyControlFunction scaleDownFn
    ) {
        if (minConcurrency < 1) {
            throw new IllegalArgumentException("minConcurrency must be >= 1");
        }
        if (initialConcurrency > maxConcurrency || initialConcurrency < minConcurrency) {
            throw new IllegalArgumentException("initialConcurrency must be between min and max");
        }
        if (maxConcurrency < minConcurrency) {
            throw new IllegalArgumentException("maxConcurrency must be >= 1 and higher than minConcurrency");
        }
        this.initialConcurrency = initialConcurrency;
        this.minConcurrency = minConcurrency;
        this.maxConcurrency = maxConcurrency;
        this.strategy = Objects.requireNonNull(strategy);
        this.scaleUpFn = Objects.requireNonNull(scaleUpFn);
        this.scaleDownFn = Objects.requireNonNull(scaleDownFn);
    }
}
