package com.jcarrey.reactor.poller.core;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyLockMechanism;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.stream.Stream;

import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleUp;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith({MockitoExtension.class})
public class PerformanceTests {
    /**
     * Delay is 25ms, so in 1sec it fits 40.
     * All code should take no more than ~5ms per concurrent op, so we should aim for 35ms per concurrent pool.
     * But in build CI it's kind of slow, so we aim for ~30ms instead, which means 10ms execution per concurrent op :L
     */
    private static final double THROUGHPUT_PER_CONCURRENCY = 30d;

    private static final int TOTAL_MESSAGES = 100000;
    private static final double MAX_CONCURRENCY = 1000d;
    //
    // 1 concurrency >35 msg/s
    // 500 concurrency >17500 msg/s
    // 1K concurrency >35000 msg/s

    @ParameterizedTest
    @MethodSource("pipelines")
    public void shouldRun(String name, Flux<Integer> pipeline) {
        var duration = StepVerifier.create(pipeline)
                .expectNextCount(TOTAL_MESSAGES)
                .thenCancel()
                .verify();

        var throughput = 1000d * (double) TOTAL_MESSAGES / duration.toMillis();
        var expectedMinimumThroughput = THROUGHPUT_PER_CONCURRENCY * MAX_CONCURRENCY;
        log.info("{} polled {} messages in {}ms. Throughput={}msg/s", name, TOTAL_MESSAGES, duration.toMillis(), throughput);
        assertTrue(expectedMinimumThroughput < throughput, "Expected minimum of %s but was %s".formatted(expectedMinimumThroughput, throughput));
    }

    private static Stream<Arguments> pipelines() {
        return Stream.of(
                Arguments.of("parallel-pessimistic", pipeline(Schedulers.parallel(), ConcurrencyLockMechanism.Pessimistic)),
                Arguments.of("boundedElastic-pessimistic", pipeline(Schedulers.boundedElastic(), ConcurrencyLockMechanism.Pessimistic)),
                Arguments.of("immediate-pessimistic", pipeline(Schedulers.immediate(), ConcurrencyLockMechanism.Pessimistic)),
                Arguments.of("parallel-none", pipeline(Schedulers.parallel(), ConcurrencyLockMechanism.None)),
                Arguments.of("boundedElastic-none", pipeline(Schedulers.boundedElastic(), ConcurrencyLockMechanism.None)),
                Arguments.of("immediate-none", pipeline(Schedulers.immediate(), ConcurrencyLockMechanism.None))
        );
    }

    private static Flux<Integer> pipeline(Scheduler scheduler, ConcurrencyLockMechanism lockMechanism) {
        return ReactorPoller.adaptative(poller(scheduler), opts(lockMechanism));
    }

    private static Poller<Integer> poller(Scheduler scheduler) {
        return () -> Mono.delay(Duration.ofMillis(25))
                .publishOn(scheduler)
                .thenReturn(1)
                .subscribeOn(scheduler);
    }

    private static ConcurrencyControlOptions<Integer> opts(ConcurrencyLockMechanism lockMechanism) {
        return ConcurrencyControlOptions.<Integer>builder()
                .initialConcurrency(1)
                .minConcurrency(1)
                .maxConcurrency(MAX_CONCURRENCY)
                .strategy(__ -> ScaleUp)
                .scaleUpFn((c, op) -> Double.MAX_VALUE)
                .scaleDownFn((c, op) -> Double.MAX_VALUE)
                .lockMechanism(lockMechanism)
                .build();
    }
}
