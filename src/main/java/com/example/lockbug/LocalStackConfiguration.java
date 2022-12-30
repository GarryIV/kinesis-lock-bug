package com.example.lockbug;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("local-stack")
@Configuration
public class LocalStackConfiguration {
    @Bean
    @Primary
    public AWSCredentialsProvider awsCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("local", "local"));
    }

    @Bean
    @Primary
    public AwsClientBuilder.EndpointConfiguration endpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration("http://localhost:4566", "us-west-2");
    }

    @Bean
    @Primary
    public AmazonKinesisAsync amazonKinesisAsync(
                AWSCredentialsProvider credentialsProvider,
                AwsClientBuilder.EndpointConfiguration endpointConfiguration
            ) {
        return AmazonKinesisAsyncClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withEndpointConfiguration(endpointConfiguration)
                .build();
    }

    @Bean
    @Primary
    public AmazonDynamoDBAsync amazonDynamoDBAsync(
            AWSCredentialsProvider credentialsProvider,
            AwsClientBuilder.EndpointConfiguration endpointConfiguration
            ) {
        return AmazonDynamoDBAsyncClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withEndpointConfiguration(endpointConfiguration)
                .build();
    }
}
