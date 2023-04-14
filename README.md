# Reactor Poller

A reactive SQS Poller (Any other polling source can be implemented) that adapts the concurrency to the response:
 * When there is messages, concurrency can grow in different strategies:
     * fastest: When there is messages grow to MAX, when there is no messages for a concurrent 'worker' scale down by 1.
     * linear: When there is messages grow by +N, when there is no messages for a concurrent 'worker' scale down by 1.
     * exponential: When there is messages grow by *N, when there is no messages for a concurrent 'worker' scale down by 1.

# Installation 

This library is published to maven central, you can use either raw `reactor-poller-core` or `reactor-poller-sqs`.

[Reactor Poller Core](https://central.sonatype.com/artifact/com.jcarrey/reactor-poller-core/)
[Reactor Poller SQS](https://central.sonatype.com/artifact/com.jcarrey/reactor-poller-sqs/)

```xml 
<dependency>
    <groupId>com.jcarrey</groupId>
    <artifactId>reactor-poller-core</artifactId>
    <version>0.0.3</version>
</dependency>
```

```yaml
implementation 'com.jcarrey:reactor-poller-core:0.0.3'
```

```xml
<dependency>
    <groupId>com.jcarrey</groupId>
    <artifactId>reactor-poller-sqs</artifactId>
    <version>0.0.3</version>
</dependency>
```
```yaml
implementation 'com.jcarrey:reactor-poller-sqs:0.0.3'
```

## Sample usages

### Raw API usage sample

This is raw usage sample, that could be extended to any polling source
```java
var random = new Random();
Poller<Integer> poller = () -> Mono.fromSupplier(() -> random.nextInt(3));
var options = ConcurrencyControlOptions.<Integer>builder()
  .initialConcurrency(1)
  .maxConcurrency(10)
  .minConcurrency(1)
  .strategy(value -> switch (value) {
     case 0 -> ConcurrencyControlOperation.ScaleUp;
     case 1 -> ConcurrencyControlOperation.ScaleDown;
     default -> ConcurrencyControlOperation.Noop;
  })
  .scaleUpFn(ConcurrencyControlFunctions.max())
  .scaleDownFn(ConcurrencyControlFunctions.max())
  .build();

PollerFlux.adaptative(poller, options)
  .subscribe();
```

### SQS API usage sample

```java

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
  .scaleUpFn(ConcurrencyControlFunctions.max())
  .scaleDownFn(ConcurrencyControlFunctions.max())
  .build();

PollerFlux.adaptative(new SqsPoller(sqsClient, receiveRequest), options).subscribe();
```