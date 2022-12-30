package com.example.lockbug;

import java.util.concurrent.TimeUnit;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions.AmazonDynamoDBLockClientOptionsBuilder;
import com.amazonaws.services.dynamodbv2.CreateDynamoDBTableOptions;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.aws.lock.DynamoDbLockRegistry;

@Profile("dynamo-db")
@Configuration
public class DynamoDbLockBugConfiguration {

	private static final Log logger = LogFactory.getLog(DynamoDbLockRegistry.class);

	private static final String TABLE_NAME = DynamoDbLockRegistry.DEFAULT_TABLE_NAME;
	private static final String PARTITION_KEY_NAME = DynamoDbLockRegistry.DEFAULT_PARTITION_KEY_NAME;
	private static final String SORT_KEY_NAME = DynamoDbLockRegistry.DEFAULT_SORT_KEY_NAME;
	private static final String PARTITION_KEY = "test-key";

	private AmazonDynamoDBLockClientOptionsBuilder dynamoDBLockClientOptionsBuilder(AmazonDynamoDB amazonDynamoDb) {
		return AmazonDynamoDBLockClientOptions.builder(amazonDynamoDb, TABLE_NAME)
				.withPartitionKeyName(PARTITION_KEY_NAME)
				.withSortKeyName(SORT_KEY_NAME);
	}

	@Bean
	public AmazonDynamoDB amazonDynamoDB() {
		return AmazonDynamoDBAsyncClient.builder().build();
	}

	@Bean
	public DynamoDbLockRegistry dynamoDbLockRegistry(AmazonDynamoDB amazonDynamoDb) {
		var lockClientOptions = dynamoDBLockClientOptionsBuilder(amazonDynamoDb).build();
		return new DynamoDbLockRegistry(new AmazonDynamoDBLockClient(lockClientOptions));
	}

	@Bean
	public CommandLineRunner runner(AmazonDynamoDB amazonDynamoDb, DynamoDbLockRegistry dynamoDbLockRegistry) {
		return args -> {
			createAbandonedLock(amazonDynamoDb);

			logger.info("Trying to acquire the abandoned lock");
			var lock = dynamoDbLockRegistry.obtain(PARTITION_KEY);
			while (!lock.tryLock()) {
				logger.info("Failed to lock");
				Thread.sleep(1000);
			}
			logger.info("Lock is acquired");
			lock.unlock();
			logger.info("Done");
		};
	}

	private void createAbandonedLock(AmazonDynamoDB amazonDynamoDb) throws InterruptedException {
		var options = dynamoDBLockClientOptionsBuilder(amazonDynamoDb)
				// do not update lock to allow another registry acquire it
				.withCreateHeartbeatBackgroundThread(false)
				.build();

		var lockClient = new AmazonDynamoDBLockClient(options);
		if (!lockClient.lockTableExists()) {
			var tableOptions = CreateDynamoDBTableOptions.builder(
							amazonDynamoDb,
							new ProvisionedThroughput(1L, 1L),
							TABLE_NAME
					)
					.withPartitionKeyName(PARTITION_KEY_NAME)
					.withSortKeyName(SORT_KEY_NAME).build();
			AmazonDynamoDBLockClient.createLockTableInDynamoDB(tableOptions);
		}
		lockClient.acquireLock(
				AcquireLockOptions.builder(PARTITION_KEY)
						.withSortKey(DynamoDbLockRegistry.DEFAULT_SORT_KEY)
						.withAdditionalTimeToWaitForLock(30L) // more than lease duration we use
						.withTimeUnit(TimeUnit.SECONDS)
						.build()
		);
	}
}
