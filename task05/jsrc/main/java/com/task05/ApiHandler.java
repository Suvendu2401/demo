package com.task05;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(
		lambdaName = "api_handler",
		roleName = "api_handler-role",
		isPublishVersion = true,
		aliasName = "${lambdas_alias_name}",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class ApiHandler implements RequestHandler<Object, Map<String, Object>> {

	// Get the table name dynamically from environment variables
	private static final String TABLE_NAME = System.getenv("DYNAMODB_TABLE") != null
			? System.getenv("DYNAMODB_TABLE")
			: "DefaultTable";  // Fallback table name

	private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

	public Map<String, Object> handleRequest(Object request, Context context) {
		System.out.println("Received request: " + request);
		System.out.println("Using DynamoDB table: " + TABLE_NAME);

		// Generate UUID for event ID
		String eventId = UUID.randomUUID().toString();
		String createdAt = Instant.now().toString();

		// Convert request object to a Map
		Map<String, Object> requestMap = (request instanceof Map) ? (Map<String, Object>) request : new HashMap<>();
		Map<String, Object> body = (Map<String, Object>) requestMap.getOrDefault("body", new HashMap<>());

		// Construct DynamoDB item
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", AttributeValue.builder().s(eventId).build());
		item.put("principalId", AttributeValue.builder().n("10").build());  // Hardcoded, modify as needed
		item.put("createdAt", AttributeValue.builder().s(createdAt).build());
		item.put("body", AttributeValue.builder().s(body.toString()).build());

		// Save to DynamoDB
		try {
			PutItemRequest putItemRequest = PutItemRequest.builder()
					.tableName(TABLE_NAME)
					.item(item)
					.build();
			dynamoDbClient.putItem(putItemRequest);
			System.out.println("Saved event to DynamoDB: " + item);
		} catch (Exception e) {
			System.err.println("Error saving to DynamoDB: " + e.getMessage());
		}

		// Prepare response
		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 201);  // Set status code to 201
		response.put("event", Map.of(
				"id", eventId,
				"principalId", 10,
				"createdAt", createdAt,
				"body", body
		));

		return response;
	}
}
