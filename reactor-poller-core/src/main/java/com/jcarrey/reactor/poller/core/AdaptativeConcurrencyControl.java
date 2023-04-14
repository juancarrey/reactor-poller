package com.jcarrey.reactor.poller.core;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.Noop;
import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleDown;
import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleUp;

@Slf4j
class AdaptativeConcurrencyControl<T> implements Consumer<FluxSink<T>> {
    private final Poller<T> poller;
    private final ConcurrencyControlOptions<T> options;

    private final AtomicInteger currentConcurrency;
    private final AtomicLong pendingRequests = new AtomicLong(0);
    private final ReentrantLock concurrencyUpdateLock = new ReentrantLock();

    public AdaptativeConcurrencyControl(Poller<T> poller, ConcurrencyControlOptions<T> options) {
        this.poller = poller;
        this.options = options;
        this.currentConcurrency = new AtomicInteger(options.getInitialConcurrency());
    }

    @Override
    public void accept(FluxSink<T> subscriber) {
        subscriber.onRequest(requestCount -> this.onRequest(subscriber));
    }

    private void onRequest(FluxSink<T> subscriber) {
        consume(subscriber);
    }

    private void consume(FluxSink<T> subscriber) {
        if (subscriber.isCancelled()) {
            log.trace("Cancelled - No more consumption");
            return;
        }

        var availableConcurrency = currentConcurrency.get() - pendingRequests.get();
        var maxRequests = Math.min(subscriber.requestedFromDownstream(), availableConcurrency);
        for (int i = 0; i < maxRequests; i++) {
            if (subscriber.isCancelled()) {
                break;
            }

            pendingRequests.incrementAndGet();
            poller.poll()
                    // Move away from poller thread - whatever that is
                    .publishOn(Schedulers.boundedElastic())
                    .onErrorStop()
                    .doOnError(err -> {
                        // FreeUp concurrent slot and keep requesting
                        log.warn("poller failed - skip", err);
                        this.finishRequest();
                        this.consume(subscriber);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(el -> {
                        this.adaptConcurrency(subscriber, el);
                        this.finishRequest();
                        this.next(subscriber, el);
                        this.consume(subscriber);
                    });
        }
    }

    private void next(FluxSink<T> subscriber, T el) {
        if (!subscriber.isCancelled()) {
            subscriber.next(el);
        }
    }

    private void finishRequest() {
        pendingRequests.decrementAndGet();
    }

    private void adaptConcurrency(FluxSink<T> subscriber, T element) {
        if (subscriber.isCancelled()) {
            return;
        }
        var strategy = options.getStrategy();
        var operation = strategy.calculate(element);
        if (!isNoop(operation)) {
            log.trace("Executing operation {}", operation);
            concurrencyUpdateLock.lock();
            tryAdaptConcurrencyWithPermit(operation);
            concurrencyUpdateLock.unlock();
        }
    }

    private void tryAdaptConcurrencyWithPermit(ConcurrencyControlOperation operation) {
        try {
            currentConcurrency.getAndUpdate(current -> {
                if (isNoop(operation)) {
                    return current;
                }

                var delta = operation == ScaleDown
                        ? -Math.abs(options.getScaleDownFn().calculateDelta(current, operation))
                        : Math.abs(options.getScaleUpFn().calculateDelta(current, operation));
                var next = calculateNext(current, delta);
                if (log.isTraceEnabled()) {
                    log.trace("[concurrency-update current={}, next={}", current, next);
                }
                return next;
            });
        } catch (Exception error) {
            log.warn("Could not update concurrency.", error);
        }
    }

    private int calculateNext(int current, int delta) {
        var max = options.getMaxConcurrency();
        var min = options.getMinConcurrency();
        switch (delta) {
            case 0: return current;
            case Integer.MAX_VALUE: return max;
            case Integer.MIN_VALUE: return min;
            default: return Integer.min(Integer.max(current + delta, min), max);
        }
    }

    private boolean isNoop(ConcurrencyControlOperation operation) {
        if (operation == Noop) return true;
        var concurrency = currentConcurrency.get();
        return operation == ScaleUp && concurrency == options.getMaxConcurrency()
             || operation == ScaleDown && concurrency == options.getMinConcurrency();
    }
}
