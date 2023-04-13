package com.jcarrey.reactor.poller.core;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicBoolean;
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

    private final AtomicLong requested = new AtomicLong();
    private final AtomicInteger currentConcurrency;
    private final AtomicLong pendingRequests = new AtomicLong(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final ReentrantLock concurrencyUpdateLock = new ReentrantLock();

    public AdaptativeConcurrencyControl(Poller<T> poller, ConcurrencyControlOptions<T> options) {
        this.poller = poller;
        this.options = options;
        this.currentConcurrency = new AtomicInteger(options.getInitialConcurrency());
    }

    @Override
    public void accept(FluxSink<T> subscriber) {
        subscriber.onRequest(requestCount -> this.onRequest(subscriber, requestCount));
        subscriber.onCancel(this::onCancel);
        subscriber.onDispose(this::onDispose);
    }

    private void onRequest(FluxSink<T> subscriber, long count) {
        if (count != Long.MAX_VALUE) {
            requested.addAndGet(count);
        } else {
            requested.set(Long.MAX_VALUE);
        }

        // Trigger available space
        consume(subscriber);
    }

    private void onCancel() {
        cancelled.compareAndSet(false, true);
    }

    private void onDispose() {
        cancelled.compareAndSet(false, true);
    }

    private void consume(FluxSink<T> subscriber) {
        if (cancelled.get()) {
            log.trace("Cancelled - No more consumption");
            return;
        }

        var availableConcurrency = currentConcurrency.get() - pendingRequests.get();
        for (int i = 0; i < availableConcurrency; i++) {
            pendingRequests.incrementAndGet();

            poller.poll()
                    // Move away from poller thread - whatever that is
                    .publishOn(Schedulers.boundedElastic())
                    .onErrorStop()
                    .doOnError(err -> {
                        // FreeUp concurrent slot and keep requesting
                        log.warn("sqs-poll failed - skip", err);
                        this.finishRequest();
                        this.consume(subscriber);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(el -> {
                        this.adaptConcurrency(el);
                        this.finishRequest();
                        this.next(subscriber, el);
                        this.consume(subscriber);
                    });
        }
    }

    private void next(FluxSink<T> subscriber, T el) {
        if (!cancelled.get()) {
            subscriber.next(el);
        }
    }

    private void finishRequest() {
        pendingRequests.decrementAndGet();
        if (requested.get() != Long.MAX_VALUE) {
            // Handled one of requested items
            requested.decrementAndGet();
        }
    }

    private void adaptConcurrency(T element) {
        if (this.cancelled.get()) {
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
