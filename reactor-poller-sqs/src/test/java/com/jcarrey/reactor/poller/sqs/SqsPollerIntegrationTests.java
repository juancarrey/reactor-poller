package com.jcarrey.reactor.poller.sqs;

import com.jcarrey.reactor.poller.core.ConcurrencyControlOptions;
import com.jcarrey.reactor.poller.core.ReactorPoller;
import com.jcarrey.reactor.poller.core.concurrency.ConcurrencyControlFunctions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class SqsPollerIntegrationTests {

    private static SqsAsyncClient client;
    private String queueUrl;

    private static final int NUM_MESSAGES = 1000;

    @BeforeAll
    public static void setUpInfra() {
        var localStack = AwsInfra.createLocalStack();
        client = AwsInfra.createSqsClient(localStack);
    }

    @BeforeEach
    public void setUpTest() {
        this.queueUrl = AwsInfra.createQueue(client, "queue-" + UUID.randomUUID()).block();
        log.info("Queue created {}", queueUrl);

        Mono.fromFuture(client.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build())).block();
        log.info("Adding messages to SQS queue {}", queueUrl);
        IntStream.range(0, NUM_MESSAGES / 10).parallel().forEach(i -> sendRandomMessages().block());
        log.info("All messages entered into SQS");
    }

    @Test
    public void testSqsPoller() {
        var receiveRequest =  ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .build();

        var options = ConcurrencyControlOptions.<ReceiveMessageResponse>builder()
                .initialConcurrency(100)
                .maxConcurrency(300)
                .minConcurrency(1)
                .strategy(SqsStrategies.thresholdScaleUp(8))
                .scaleUpFn(ConcurrencyControlFunctions.max())
                .scaleDownFn(ConcurrencyControlFunctions.max())
                .build();

        var poller = ReactorPoller.adaptative(new SqsPoller(client, receiveRequest), options)
                .flatMapIterable(ReceiveMessageResponse::messages);

        var duration = StepVerifier.create(poller)
                .expectNextCount(NUM_MESSAGES)
                .thenCancel()
                .verify(Duration.ofSeconds(60));

        // LocalStack is kinda slow
        log.info("Consumed {} messages in {}ms", NUM_MESSAGES, duration.toMillis());
    }

    private Mono<SendMessageBatchResponse> sendRandomMessages() {
        return Mono.fromFuture(client.sendMessageBatch(SendMessageBatchRequest.builder()
                .entries(randomMessages())
                .queueUrl(queueUrl)
                .build()))
                .publishOn(Schedulers.boundedElastic());
    }

    private Collection<SendMessageBatchRequestEntry> randomMessages() {
        return IntStream.rangeClosed(1, 10)
                .mapToObj(i -> SendMessageBatchRequestEntry.builder()
                        .id(String.valueOf(i))
                        .messageBody(String.valueOf(System.nanoTime()))
                        .build())
                .collect(Collectors.toList());
    }
}
