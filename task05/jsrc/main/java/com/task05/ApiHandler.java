package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(
		lambdaName = "api_handler",
		roleName = "api_handler-role",
		isPublishVersion = true,
		aliasName = "learn",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
	private static final String TABLE_NAME = "cmtr-57a5b864-Events"; // Ensure this matches your DynamoDB table name

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
		context.getLogger().log("Received request: " + request.toString());

		// Generate UUID and timestamp
		String id = UUID.randomUUID().toString();
		String timestamp = Instant.now().toString();

		// Extract request parameters
		Integer principalId = (Integer) request.get("principalId");
		Map<String, Object> content = (Map<String, Object>) request.get("content");

		if (principalId == null || content == null) {
			return Map.of("statusCode", 400, "message", "Invalid input: Missing principalId or content");
		}

		// Prepare item for DynamoDB
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", new AttributeValue(id));
		item.put("principalId", new AttributeValue().withN(String.valueOf(principalId)));
		item.put("createdAt", new AttributeValue(timestamp));
		item.put("body", new AttributeValue().withM(convertToDynamoDBMap(content)));

		try {
			// Insert into DynamoDB
			PutItemRequest putItemRequest = new PutItemRequest(TABLE_NAME, item);
			dynamoDB.putItem(putItemRequest);
			context.getLogger().log("Successfully stored event in DynamoDB");

			// Prepare response
			return Map.of(
					"statusCode", 201,
					"event", Map.of(
							"id", id,
							"principalId", principalId,
							"createdAt", timestamp,
							"body", content
					)
			);
		} catch (Exception e) {
			context.getLogger().log("Error saving to DynamoDB: " + e.getMessage());
			return Map.of("statusCode", 500, "message", "Error saving event to database");
		}
	}

	private Map<String, AttributeValue> convertToDynamoDBMap(Map<String, Object> content) {
		Map<String, AttributeValue> dynamoDBMap = new HashMap<>();
		for (Map.Entry<String, Object> entry : content.entrySet()) {
			dynamoDBMap.put(entry.getKey(), new AttributeValue(entry.getValue().toString()));
		}
		return dynamoDBMap;
	}
}
