package com.jcarrey.reactor.poller.sqs;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

import java.time.Duration;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@Slf4j
public class AwsInfra {

    public static LocalStackContainer createLocalStack() {
        var localstackImage = DockerImageName.parse("localstack/localstack:1.4.0");
        var localStackContainer = new LocalStackContainer(localstackImage)
                .withPrivilegedMode(true)
                .withEnv("AWS_ACCESS_KEY_ID", "foobar")
                .withEnv("AWS_SECRET_ACCESS_KEY", "foobar")
                .withEnv("AWS_REGION", "us-east-1")
                .withEnv("DOCKER_HOST", "unix:///var/run/docker.sock")
                .withServices(SQS);
        localStackContainer.start();
        return localStackContainer;
    }

    public static Mono<String> createQueue(SqsAsyncClient client, String queueName) {
        return Mono.fromFuture(client.createQueue(CreateQueueRequest.builder()
                .queueName(queueName)
                .build()))
                .map(CreateQueueResponse::queueUrl);
    }

    public static SqsAsyncClient createSqsClient(LocalStackContainer localstack) {
        var asyncHttpClientBuilder = NettyNioAsyncHttpClient.builder()
                .maxConcurrency(300)
                .maxPendingConnectionAcquires(10_000)
                .connectionMaxIdleTime(Duration.ofSeconds(60))
                .connectionTimeout(Duration.ofSeconds(10))
                .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10));

        return SqsAsyncClient.builder()
                .httpClientBuilder(asyncHttpClientBuilder)
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                        )
                )
                .region(Region.of(localstack.getRegion()))
                .build();
    }
}
