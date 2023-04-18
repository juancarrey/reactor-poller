package com.jcarrey.reactor.poller.core;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyLockMechanism;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;

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
                    .onErrorStop()
                    .doOnTerminate(pendingRequests::decrementAndGet)
                    .doOnError(err -> this.onRequest(subscriber))
                    .subscribe(el -> {
                        if (!subscriber.isCancelled()) {
                            this.adaptConcurrency(el);
                            subscriber.next(el);
                            this.onRequest(subscriber);
                        }
                    });
        }
    }

    private void adaptConcurrency(T element) {
        var strategy = options.getStrategy();
        var operation = strategy.calculate(element);
        if (!isNoop(operation)) {
            if (options.getLockMechanism() == ConcurrencyLockMechanism.Pessimistic) {
                concurrencyUpdateLock.lock();
            }

            tryAdaptConcurrencyWithPermit(operation);

            if (options.getLockMechanism() == ConcurrencyLockMechanism.Pessimistic) {
                concurrencyUpdateLock.unlock();
            }
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
        return switch (delta) {
            case 0 -> current;
            case Integer.MAX_VALUE -> max;
            case Integer.MIN_VALUE -> min;
            default -> Integer.min(Integer.max(current + delta, min), max);
        };
    }

    private boolean isNoop(ConcurrencyControlOperation operation) {
        if (operation == Noop) return true;
        var concurrency = currentConcurrency.get();
        return operation == ScaleUp && concurrency == options.getMaxConcurrency()
             || operation == ScaleDown && concurrency == options.getMinConcurrency();
    }
}
