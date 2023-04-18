package com.jcarrey.reactor.poller.core;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface Poller<T> {
    Mono<T> poll();
}
