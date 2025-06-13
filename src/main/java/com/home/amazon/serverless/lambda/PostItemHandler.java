package com.home.amazon.serverless.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.home.amazon.serverless.DependencyFactory;
import com.home.amazon.serverless.model.Book;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Collections;
import java.util.Map;

public class PostItemHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbEnhancedClient dbClient;
    private final SqsClient sqsClient;
    private final String tableName;
    private final TableSchema<Book> bookTableSchema;
    private final boolean sqsEnabled;
    private final String sqsQueueUrl;
    private final Gson gson;

    public PostItemHandler() {
        dbClient = DependencyFactory.dynamoDbEnhancedClient();
        sqsClient = DependencyFactory.sqsClient();
        tableName = DependencyFactory.tableName();
        bookTableSchema = TableSchema.fromBean(Book.class);
        sqsEnabled = DependencyFactory.isSqsEnabled();
        sqsQueueUrl = DependencyFactory.sqsQueueUrl();
        gson = new Gson();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String response = "";
        DynamoDbTable<Book> booksTable = dbClient.table(tableName, bookTableSchema);
        Map<String, String> queryStringParameters = input.getQueryStringParameters();
        if (queryStringParameters != null) {
            Book item = new Book();
            item.setIsbn(queryStringParameters.get(Book.PARTITION_KEY));
            item.setAuthor(queryStringParameters.get("author"));
            item.setName(queryStringParameters.get("name"));
            booksTable.putItem(item);
            
            // Optionally send to SQS if enabled
            if (sqsEnabled && sqsQueueUrl != null && !sqsQueueUrl.isEmpty()) {
                try {
                    String messageBody = gson.toJson(item);
                    SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                            .queueUrl(sqsQueueUrl)
                            .messageBody(messageBody)
                            .build();
                    
                    SendMessageResponse sendMessageResponse = sqsClient.sendMessage(sendMessageRequest);
                    context.getLogger().log("Message sent to SQS. MessageId: " + sendMessageResponse.messageId());
                } catch (Exception e) {
                    context.getLogger().log("Error sending message to SQS: " + e.getMessage());
                }
            }
        }

        return new APIGatewayProxyResponseEvent().withStatusCode(200)
                .withIsBase64Encoded(Boolean.FALSE)
                .withHeaders(Collections.emptyMap())
                .withBody("Success");
    }
}
