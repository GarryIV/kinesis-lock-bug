Kinesis / DynamoDB lock bug demo
---

This app reproduces a case when you have an abandoned lock in a DynamoDB table 
and trying to use `DynamoDbLock.tryLock()` to acquire it.

To reproduce it run
```
docker-compose up -d
./gradlew bootRun
```

The scenario is:
* create a lock in a DynamoDb table in one AmazonDynamoDBLockClient 
  * this client doesn't run heartbeat since it intended to be dead
* obtain the same lock from a DynamoDbLockRegistry which uses the same settings as the AmazonDynamoDBLockClient above
* call `tryLock()` in a cycle trying to acquire it  

Expected:
* Lock is acquired after lease duration (20 sec)

Actual:
* Lock is never acquired with spring-integration-aws:2.5.2 +

Before `spring-integration-aws:2.5.2` it was possible to acquire the lock due to https://github.com/spring-projects/spring-integration-aws/issues/205
But after the fix the problem was revealed.
As a consequence `spring-cloud-stream-binder-kinesis` can't acquire an abandoned lock with 
`spring-integration-aws` >= `2.5.2`

To reproduce it:
* start local stack 
```shell
docker-compose up -d
```
* start application 
```
./gradlew bootRun --args='--spring.profiles.active=kinesis,local-stack'
```
* it should create a lock and start waiting for messages from a Kinesis stream
* kill the process using Windows Task Manager or using `kill -9 "$(cat application.pid)`
* the application should stop without releasing the lock
* start application again 
```
./gradlew bootRun --args='--spring.profiles.active=kinesis,local-stack'
```
* the application will never acquire the lock printing 
```
2022-12-30 11:32:55.629 TRACE 4984 --- [s-shard-locks-1] o.s.i.aws.lock.DynamoDbLockRegistry      : The lock 'DynamoDbLock [lockKey=kinesis-lock-bug:kinesis-lock-bug:shardId-000000000000,lockedAt=2022-12-30@11:26:03.502, lockItem=null]' cannot be acqu
ired at the moment

com.amazonaws.services.dynamodbv2.model.LockCurrentlyUnavailableException: The lock being requested is being held by another client.
        at com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient.acquireLock(AmazonDynamoDBLockClient.java:457) ~[dynamodb-lock-client-1.1.0.jar:na]
        at com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient.tryAcquireLock(AmazonDynamoDBLockClient.java:781) ~[dynamodb-lock-client-1.1.0.jar:na]
        at org.springframework.integration.aws.lock.DynamoDbLockRegistry$DynamoDbLock.doLock(DynamoDbLockRegistry.java:555) ~[spring-integration-aws-2.5.3.jar:na]
        at org.springframework.integration.aws.lock.DynamoDbLockRegistry$DynamoDbLock.tryLock(DynamoDbLockRegistry.java:519) ~[spring-integration-aws-2.5.3.jar:na]
        at org.springframework.integration.aws.lock.DynamoDbLockRegistry$DynamoDbLock.tryLock(DynamoDbLockRegistry.java:488) ~[spring-integration-aws-2.5.3.jar:na]
        at org.springframework.integration.aws.inbound.kinesis.KinesisMessageDrivenChannelAdapter$ShardConsumerManager.lambda$run$0(KinesisMessageDrivenChannelAdapter.java:1492) ~[spring-integration-aws-2.5.3.jar:na]
        at java.base/java.util.concurrent.ConcurrentHashMap.removeEntryIf(ConcurrentHashMap.java:1640) ~[na:na]
        at java.base/java.util.concurrent.ConcurrentHashMap$EntrySetView.removeIf(ConcurrentHashMap.java:4836) ~[na:na]
        at org.springframework.integration.aws.inbound.kinesis.KinesisMessageDrivenChannelAdapter$ShardConsumerManager.run(KinesisMessageDrivenChannelAdapter.java:1485) ~[spring-integration-aws-2.5.3.jar:na]
        at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539) ~[na:na]
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na]
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136) ~[na:na]
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635) ~[na:na]
        at java.base/java.lang.Thread.run(Thread.java:833) ~[na:na]

```
* stop local stack and the application

NB: It also possible to reproduce the same with real AWS resources, just remove `local-stack` profile to do so.
