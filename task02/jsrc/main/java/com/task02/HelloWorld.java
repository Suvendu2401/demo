package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
    lambdaName = "hello_world",
	roleName = "hello_world-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class HelloWorld implements RequestHandler<Object, Map<String, Object>> {

	public Map<String, Object> handleRequest(Object event, Context context) {
		System.out.println("Hello from lambda" + event);
		if (!(event instanceof Map)) {
			System.out.println("Invalid event format");
			return createErrorResponse(400, "Bad Request");
		}

		Map<String, Object> eventMap = (Map<String, Object>) event;
		String path = (String) eventMap.get("path");

		if ("/hello".equals(path)) {
			return createSuccessResponse();
		} else {
			return createErrorResponse(404, "Not Found");
		}
	}

	private Map<String, Object> createSuccessResponse() {
		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 200);
		response.put("message", "Hello from Lambda");
		return response;
	}

	private Map<String, Object> createErrorResponse(int statusCode, String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", statusCode);
		response.put("message", message);
		return response;
	}
}
