
package com.home.amazon.serverless;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public class DependencyFactory {

    public static final String ENV_VARIABLE_TABLE = "TABLE";
    public static final String ENV_VARIABLE_SQS_ENABLED = "SQS_ENABLED";
    public static final String ENV_VARIABLE_SQS_QUEUE_URL = "SQS_QUEUE_URL";

    private DependencyFactory() {}

    /**
     * @return an instance of DynamoDbEnhancedClient
     */
    public static DynamoDbEnhancedClient dynamoDbEnhancedClient() {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.builder()
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                        .httpClientBuilder(UrlConnectionHttpClient.builder())
                        .build())
                .build();
    }

    /**
     * @return an instance of SqsClient
     */
    public static SqsClient sqsClient() {
        return SqsClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    public static String tableName() {
        return System.getenv(ENV_VARIABLE_TABLE);
    }

    public static boolean isSqsEnabled() {
        String sqsEnabled = System.getenv(ENV_VARIABLE_SQS_ENABLED);
        return sqsEnabled != null && sqsEnabled.equalsIgnoreCase("true");
    }

    public static String sqsQueueUrl() {
        return System.getenv(ENV_VARIABLE_SQS_QUEUE_URL);
    }
}
