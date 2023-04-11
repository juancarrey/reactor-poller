package com.jcarrey.reactor.poller.sqs;

import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlTrigger;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public class SqsStrategies {

    /**
     * {@link SqsThresholdConcurrencyControl}
     * @param scaleUpThreshold - Minimum number of messages in a response to trigger a scale-up event.
     * @return A ConcurrencyControl that will trigger scale downs for errors and empty responses, and will trigger
     *  scale ups if the amount of messages returned is > scaleUpThreshold
     */
    public static ConcurrencyControlTrigger<ReceiveMessageResponse> thresholdScaleUp(int scaleUpThreshold) {
        return new SqsThresholdConcurrencyControl(scaleUpThreshold);
    }
}
