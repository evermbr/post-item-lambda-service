package com.home.amazon.serverless.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.home.amazon.serverless.DependencyFactory;
import com.home.amazon.serverless.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostItemHandlerTest {

    private static final String TEST_TABLE_NAME = "TestTable";
    private static final String TEST_PARTITION_KEY_VALUE = "123";
    private static final String TEST_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue";
    private static final String TEST_AUTHOR = "Test Author";
    private static final String TEST_NAME = "Test Book";

    @Mock
    private DynamoDbEnhancedClient dbClient;

    @Mock
    private SqsClient sqsClient;

    @Mock
    private DynamoDbTable<Book> table;

    @Mock
    private APIGatewayProxyRequestEvent request;

    @Mock
    private Context context;

    @Mock
    private SendMessageResponse sendMessageResponse;

    private PostItemHandler handler;
    private Map<String, String> queryParams;

    @BeforeEach
    public void setUp() {
        when(dbClient.table(eq(TEST_TABLE_NAME), any(TableSchema.class))).thenReturn(table);
        
        queryParams = new HashMap<>();
        queryParams.put(Book.PARTITION_KEY, TEST_PARTITION_KEY_VALUE);
        queryParams.put("author", TEST_AUTHOR);
        queryParams.put("name", TEST_NAME);
        when(request.getQueryStringParameters()).thenReturn(queryParams);
        
        when(sendMessageResponse.messageId()).thenReturn("test-message-id");
    }

    @Test
    public void shouldSaveItemToDynamoDb() {
        try (MockedStatic<DependencyFactory> dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class)) {
            // Setup mocks
            dependencyFactoryMockedStatic.when(DependencyFactory::dynamoDbEnhancedClient).thenReturn(dbClient);
            dependencyFactoryMockedStatic.when(DependencyFactory::sqsClient).thenReturn(sqsClient);
            dependencyFactoryMockedStatic.when(DependencyFactory::tableName).thenReturn(TEST_TABLE_NAME);
            dependencyFactoryMockedStatic.when(DependencyFactory::isSqsEnabled).thenReturn(false);
            
            // Create handler and invoke
            handler = new PostItemHandler();
            APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);
            
            // Verify
            verify(table).putItem(any(Book.class));
            verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
            assertEquals(200, response.getStatusCode());
        }
    }

    @Test
    public void shouldSendToSqsWhenEnabled() {
        try (MockedStatic<DependencyFactory> dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class)) {
            // Setup mocks
            dependencyFactoryMockedStatic.when(DependencyFactory::dynamoDbEnhancedClient).thenReturn(dbClient);
            dependencyFactoryMockedStatic.when(DependencyFactory::sqsClient).thenReturn(sqsClient);
            dependencyFactoryMockedStatic.when(DependencyFactory::tableName).thenReturn(TEST_TABLE_NAME);
            dependencyFactoryMockedStatic.when(DependencyFactory::isSqsEnabled).thenReturn(true);
            dependencyFactoryMockedStatic.when(DependencyFactory::sqsQueueUrl).thenReturn(TEST_QUEUE_URL);
            
            when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResponse);
            
            // Create handler and invoke
            handler = new PostItemHandler();
            APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);
            
            // Verify
            verify(table).putItem(any(Book.class));
            verify(sqsClient).sendMessage(any(SendMessageRequest.class));
            assertEquals(200, response.getStatusCode());
        }
    }

    @Test
    public void shouldNotSendToSqsWhenQueueUrlIsNull() {
        try (MockedStatic<DependencyFactory> dependencyFactoryMockedStatic = mockStatic(DependencyFactory.class)) {
            // Setup mocks
            dependencyFactoryMockedStatic.when(DependencyFactory::dynamoDbEnhancedClient).thenReturn(dbClient);
            dependencyFactoryMockedStatic.when(DependencyFactory::sqsClient).thenReturn(sqsClient);
            dependencyFactoryMockedStatic.when(DependencyFactory::tableName).thenReturn(TEST_TABLE_NAME);
            dependencyFactoryMockedStatic.when(DependencyFactory::isSqsEnabled).thenReturn(true);
            dependencyFactoryMockedStatic.when(DependencyFactory::sqsQueueUrl).thenReturn(null);
            
            // Create handler and invoke
            handler = new PostItemHandler();
            APIGatewayProxyResponseEvent response = handler.handleRequest(request, context);
            
            // Verify
            verify(table).putItem(any(Book.class));
            verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
            assertEquals(200, response.getStatusCode());
        }
    }
}