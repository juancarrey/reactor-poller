package com.jcarrey.reactor.poller.sqs;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlTrigger;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.Noop;
import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleDown;
import static com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlOperation.ScaleUp;

@AllArgsConstructor(access = AccessLevel.MODULE)
@Builder
class SqsThresholdConcurrencyControl implements ConcurrencyControlTrigger<ReceiveMessageResponse> {

    private final int scaleUpThreshold;

    @Override
    public ConcurrencyControlOperation calculate(ReceiveMessageResponse response) {
        if (!response.sdkHttpResponse().isSuccessful() || response.messages().isEmpty()) {
            return ScaleDown;
        }

        if (response.messages().size() >= scaleUpThreshold) {
            return ScaleUp;
        }

        return Noop;
    }
}
