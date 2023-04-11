package com.jcarrey.reactor.poller.sqs;

import com.jcarrey.reactor.poller.core.Poller;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public class SqsPoller implements Poller<ReceiveMessageResponse> {
    private final SqsAsyncClient client;
    private final ReceiveMessageRequest request;

    public SqsPoller(SqsAsyncClient client, ReceiveMessageRequest request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public Mono<ReceiveMessageResponse> poll() {
        return Mono.fromFuture(client.receiveMessage(request));
    }
}
