package com.jcarrey.reactor.poller.sqs;

public class SqsPollerTests {
/*
    @Test
    public void readme(){
        var sqsClient = SqsAsyncClient.builder().build();
        var queueUrl = "....";
        var receiveRequest =  ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .build();

        var options = ConcurrencyControlOptions.<ReceiveMessageResponse>builder()
                .initialConcurrency(1)
                .maxConcurrency(10)
                .minConcurrency(1)
                .strategy(SqsStrategies.thresholdScaleUp(8))
                .scaleUpFn(new MaxConcurrencyControlFn())
                .scaleDownFn(new LinearConcurrencyControlFn())
                .build();

        PollerFlux.adaptative(new SqsPoller(sqsClient, receiveRequest), options).subscribe();
    }
 */
}
