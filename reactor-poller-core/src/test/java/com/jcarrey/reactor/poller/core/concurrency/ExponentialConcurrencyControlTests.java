package com.jcarrey.reactor.poller.core.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleUp;

@Slf4j
@ExtendWith({MockitoExtension.class})
public class ExponentialConcurrencyControlTests {

    @Test
    public void exponentialGrowthScalesByTwo() {
        var control = new ExponentialConcurrencyControlFn(2d);
        Assertions.assertEquals(1d, control.calculateDelta(1d, ScaleUp));
        Assertions.assertEquals(1d, control.calculateDelta(2d, ScaleUp));
        Assertions.assertEquals(1d, control.calculateDelta(4d, ScaleUp));
    }

    @Test
    public void exponentialGrowthScalesByFour() {
        var control = new ExponentialConcurrencyControlFn(3d);
        Assertions.assertEquals(2d, control.calculateDelta(1d, ScaleUp)); // 1*3 = 3 -> 1*2 + 1 = 3
        Assertions.assertEquals(2d, control.calculateDelta(2d, ScaleUp)); // 2*3 = 6 -> 2*2 + 2 = 6
        Assertions.assertEquals(2d, control.calculateDelta(3d, ScaleUp)); // 3*3 = 9 -> 3*2 + 3 = 9
    }
}
