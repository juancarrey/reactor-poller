package com.jcarrey.reactor.poller.core;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlFunction;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlTrigger;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyLockMechanism;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleDown;
import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleUp;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@Slf4j
@ExtendWith({MockitoExtension.class})
public class PesimisticLockingConcurrencyTests {

    @Mock
    private Poller<Integer> poller;

    @Mock
    private ConcurrencyControlTrigger<Integer> strategy;

    @Mock
    private ConcurrencyControlFunction scaleUp;

    @Mock
    private ConcurrencyControlFunction scaleDown;

    private Flux<Integer> pipeline;

    @BeforeEach
    public void setup() {
        Mockito.when(poller.poll()).thenReturn(Mono.just(1));

        var options = ConcurrencyControlOptions.<Integer>builder()
                .initialConcurrency(5)
                .minConcurrency(1)
                .maxConcurrency(10)
                .strategy(strategy)
                .scaleUpFn(scaleUp)
                .scaleDownFn(scaleDown)
                .lockMechanism(ConcurrencyLockMechanism.Pessimistic)
                .build();

        this.pipeline = ReactorPoller.adaptative(poller, options)
                .log();
    }

    @Test
    public void scalesUpBasedOnStrategy() {
        Mockito.when(scaleUp.calculateDelta(anyDouble(), any())).thenReturn(1d);
        Mockito.when(strategy.calculate(1)).thenReturn(ScaleUp);

        StepVerifier.create(pipeline)
                .expectNextCount(10)
                .thenCancel()
                .verify();

        Mockito.verify(scaleUp, times(1)).calculateDelta(5, ScaleUp);
        Mockito.verify(scaleUp, times(1)).calculateDelta(6, ScaleUp);
        Mockito.verify(scaleUp, times(1)).calculateDelta(7, ScaleUp);
        Mockito.verify(scaleUp, times(1)).calculateDelta(8, ScaleUp);
        Mockito.verify(scaleUp, times(1)).calculateDelta(9, ScaleUp);
        Mockito.verify(scaleUp, never()).calculateDelta(10, ScaleUp);
        Mockito.verify(scaleUp, never()).calculateDelta(1, ScaleUp);
    }

    @Test
    public void scalesUpToMaxConcurrency() {
        Mockito.when(scaleUp.calculateDelta(anyDouble(), any())).thenReturn(Double.MAX_VALUE);
        Mockito.when(strategy.calculate(1)).thenReturn(ScaleUp);

        StepVerifier.create(pipeline)
                .expectNextCount(10)
                .thenCancel()
                .verify();

        Mockito.verify(scaleUp, times(1)).calculateDelta(5, ScaleUp);
        Mockito.verify(scaleUp, never()).calculateDelta(10, ScaleUp);
        Mockito.verify(scaleUp, never()).calculateDelta(1, ScaleUp);
    }

    @Test
    public void scalesDownBasedOnStrategy() {
        Mockito.when(scaleDown.calculateDelta(anyDouble(), any())).thenReturn(1d);
        Mockito.when(strategy.calculate(1)).thenReturn(ScaleDown);

        StepVerifier.create(pipeline)
                .expectNextCount(10)
                .thenCancel()
                .verify();

        Mockito.verify(scaleDown, times(1)).calculateDelta(5, ScaleDown);
        Mockito.verify(scaleDown, times(1)).calculateDelta(4, ScaleDown);
        Mockito.verify(scaleDown, times(1)).calculateDelta(3, ScaleDown);
        Mockito.verify(scaleDown, times(1)).calculateDelta(2, ScaleDown);
        Mockito.verify(scaleDown, never()).calculateDelta(10, ScaleDown);
        Mockito.verify(scaleDown, never()).calculateDelta(1, ScaleDown);
    }

    @Test
    public void scalesUpToMinConcurrency() {
        Mockito.when(scaleDown.calculateDelta(anyDouble(), any())).thenReturn(Double.MAX_VALUE);
        Mockito.when(strategy.calculate(1)).thenReturn(ScaleDown);

        StepVerifier.create(pipeline)
                .expectNextCount(10)
                .thenCancel()
                .verify();

        Mockito.verify(scaleDown, times(1)).calculateDelta(5, ScaleDown);
        Mockito.verify(scaleDown, never()).calculateDelta(10, ScaleDown);
        Mockito.verify(scaleDown, never()).calculateDelta(1, ScaleDown);
    }

    @Test
    public void testOnPollerErrorShouldContinue() {
        var counter = new AtomicInteger(1);
        Mockito.when(poller.poll()).thenReturn(Mono.fromSupplier(counter::incrementAndGet).flatMap((currentValue) -> {
            if (currentValue % 2 == 0) {
                return Mono.just(currentValue);
            } else {
                return Mono.error(new RuntimeException());
            }
        }));

        var options = ConcurrencyControlOptions.<Integer>builder()
                .initialConcurrency(1)
                .minConcurrency(1)
                .maxConcurrency(1)
                .strategy(strategy)
                .scaleUpFn(scaleUp)
                .scaleDownFn(scaleDown)
                .build();

        Mockito.when(strategy.calculate(anyInt())).thenReturn(ConcurrencyControlOperation.Noop);
        this.pipeline = ReactorPoller.adaptative(poller, options)
                .log();

        StepVerifier.create(pipeline)
                .expectNextCount(10)
                .thenCancel()
                .verify(Duration.ofSeconds(1));
    }

    @Test
    public void shouldStopOnNeverOperator() {
        var counter = new AtomicInteger(1);
        Mockito.when(poller.poll()).thenReturn(Mono.fromSupplier(counter::incrementAndGet));

        Mockito.when(strategy.calculate(anyInt())).thenReturn(ConcurrencyControlOperation.Noop);
        Mockito.when(strategy.calculate(5)).thenReturn(ScaleUp);
        Mockito.when(strategy.calculate(6)).thenReturn(ScaleUp);
        Mockito.when(strategy.calculate(7)).thenReturn(ScaleUp);
        Mockito.when(strategy.calculate(8)).thenReturn(ConcurrencyControlOperation.Noop);
        Mockito.when(scaleUp.calculateDelta(anyDouble(), any())).thenReturn(1d);

        StepVerifier.create(pipeline)
                .expectNextCount(10)
                .thenCancel()
                .verify();

        Mockito.verify(scaleUp, times(1)).calculateDelta(5, ScaleUp);
        Mockito.verify(scaleUp, times(1)).calculateDelta(6, ScaleUp);
        Mockito.verify(scaleUp, times(1)).calculateDelta(7, ScaleUp);
        Mockito.verify(scaleUp, never()).calculateDelta(8, ScaleUp);
    }
}
