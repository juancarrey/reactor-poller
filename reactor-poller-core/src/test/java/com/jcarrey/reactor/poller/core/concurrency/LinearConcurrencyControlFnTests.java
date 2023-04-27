package com.jcarrey.reactor.poller.core.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleUp;

@Slf4j
@ExtendWith({MockitoExtension.class})
public class LinearConcurrencyControlFnTests {

    @Test
    public void linearScalesByOne() {
       var control = new LinearConcurrencyControlFn(1d);
        Assertions.assertEquals(1d, control.calculateDelta(1d, ScaleUp));
        Assertions.assertEquals(0.5d, control.calculateDelta(2d, ScaleUp));
        Assertions.assertEquals(0.25d, control.calculateDelta(4d, ScaleUp));
    }

    @Test
    public void linearScalesByTwo() {
        var control = new LinearConcurrencyControlFn(2d);
        Assertions.assertEquals(2d, control.calculateDelta(1d, ScaleUp));
        Assertions.assertEquals(1d, control.calculateDelta(2d, ScaleUp));
        Assertions.assertEquals(0.5d, control.calculateDelta(4d, ScaleUp));
    }
}
