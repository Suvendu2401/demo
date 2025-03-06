package com.task05;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

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
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private final DynamoDbClient dynamoDb = DynamoDbClient.create();
	private static final String TABLE_NAME = "cmtr-57a5b864-Events-3tt3";

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
		context.getLogger().log("Received request: " + request.toString());

		// Generate UUID and timestamp
		String id = UUID.randomUUID().toString();
		String timestamp = Instant.now().toString();

		// Extract request parameters
		Integer principalId = (Integer) request.get("principalId");
		Map<String, Object> content = (Map<String, Object>) request.get("content");

		// Prepare item for DynamoDB
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("id", AttributeValue.builder().s(id).build());
		item.put("principalId", AttributeValue.builder().n(String.valueOf(principalId)).build());
		item.put("createdAt", AttributeValue.builder().s(timestamp).build());
		item.put("body", AttributeValue.builder().s(content.toString()).build());

		// Store in DynamoDB
		dynamoDb.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());

		// Prepare response
		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 201);
		response.put("event", Map.of(
				"id", id,
				"principalId", principalId,
				"createdAt", timestamp,
				"body", content
		));

		return response;
	}
}
