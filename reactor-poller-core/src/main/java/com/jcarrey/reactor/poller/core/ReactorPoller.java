package com.jcarrey.reactor.poller.core;

import reactor.core.publisher.Flux;

public class ReactorPoller {

    /**
     * Creates an adaptative poller that adapts concurrency based on the options
     * @param poller The source of elements that are polled
     * @param options Options to control the concurrency adaptability
     * @param <T> The type of the elements being polled and transformed into a Flux
     * @return A Flux containing elements that are polled concurrently out of the poller
     */
    public static <T> Flux<T> adaptative(Poller<T> poller, ConcurrencyControlOptions<T> options) {
        return Flux.create(new AdaptativeConcurrencyControl<>(poller, options));
    }

    private ReactorPoller() {}
}
