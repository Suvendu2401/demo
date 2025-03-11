const AWS = require("aws-sdk");
const { v4: uuidv4 } = require("uuid");

// Load environment variables
const USER_POOL_ID = process.env.cup_id;
const CLIENT_ID = process.env.cup_client_id;
const TABLES_TABLE = process.env.tables_table;
const RESERVATIONS_TABLE = process.env.reservations_table;

const cognito = new AWS.CognitoIdentityServiceProvider();
const dynamoDb = new AWS.DynamoDB.DocumentClient();

/**
 * Main Lambda handler
 */
exports.handler = async (event) => {
    console.log("Incoming event:", JSON.stringify(event, null, 2));

    try {
        const path = event.resource;
        const method = event.httpMethod;
        const body = event.body ? JSON.parse(event.body) : {};
        const headers = event.headers || {};

        switch (path) {
            case "/signup":
                if (method === "POST") return await handleSignup(body);
                break;
            case "/signin":
                if (method === "POST") return await handleSignin(body);
                break;
            case "/tables":
                if (method === "GET") return await getTables(event);
                if (method === "POST") return await createTable(body);
                break;
            case `/tables/{tableId}`:
                if (method === "GET") return await getTable(event.pathParameters.tableId);
                break;
            case "/reservations":
                if (method === "GET") return await getReservations(event);
                if (method === "POST") return await createReservation(body);
                break;
            default:
                return errorResponse(404, "Route not found.");
        }
    } catch (error) {
        console.error("Error:", error);
        return errorResponse(500, "Internal Server Error.");
    }
};

/**
 * Handle user signup
 */
const handleSignup = async (body) => {
    const { username, password, email } = body;
    if (!username || !password || !email) return errorResponse(400, "Missing required fields.");

    const params = {
        UserPoolId: USER_POOL_ID,
        Username: username,
        TemporaryPassword: password,
        MessageAction: "SUPPRESS",
        UserAttributes: [{ Name: "email", Value: email }],
    };

    try {
        await cognito.adminCreateUser(params).promise();
        return successResponse(201, "User created successfully.");
    } catch (error) {
        return errorResponse(500, "Signup failed.", error);
    }
};

/**
 * Handle user signin
 */
const handleSignin = async (body) => {
    const { username, password } = body;
    if (!username || !password) return errorResponse(400, "Missing credentials.");

    const params = {
        AuthFlow: "ADMIN_USER_PASSWORD_AUTH",
        UserPoolId: USER_POOL_ID,
        ClientId: CLIENT_ID,
        AuthParameters: {
            USERNAME: username,
            PASSWORD: password,
        },
    };

    try {
        const authResponse = await cognito.adminInitiateAuth(params).promise();
        return successResponse(200, "Signin successful.", authResponse.AuthenticationResult);
    } catch (error) {
        return errorResponse(401, "Authentication failed.", error);
    }
};

/**
 * Get all tables
 */
const getTables = async () => {
    try {
        const data = await dynamoDb.scan({ TableName: TABLES_TABLE }).promise();
        return successResponse(200, "Tables fetched successfully.", data.Items);
    } catch (error) {
        return errorResponse(500, "Failed to fetch tables.", error);
    }
};

/**
 * Get a specific table by ID
 */
const getTable = async (tableId) => {
    const params = {
        TableName: TABLES_TABLE,
        Key: { id: tableId },
    };

    try {
        const data = await dynamoDb.get(params).promise();
        if (!data.Item) return errorResponse(404, "Table not found.");
        return successResponse(200, "Table fetched successfully.", data.Item);
    } catch (error) {
        return errorResponse(500, "Failed to fetch table.", error);
    }
};

/**
 * Create a new table
 */
const createTable = async (body) => {
    const { name, capacity } = body;
    if (!name || !capacity) return errorResponse(400, "Missing required fields.");

    const newItem = {
        id: uuidv4(),
        name,
        capacity,
        createdAt: new Date().toISOString(),
    };

    const params = {
        TableName: TABLES_TABLE,
        Item: newItem,
    };

    try {
        await dynamoDb.put(params).promise();
        return successResponse(201, "Table created successfully.", newItem);
    } catch (error) {
        return errorResponse(500, "Failed to create table.", error);
    }
};

/**
 * Get all reservations
 */
const getReservations = async () => {
    try {
        const data = await dynamoDb.scan({ TableName: RESERVATIONS_TABLE }).promise();
        return successResponse(200, "Reservations fetched successfully.", data.Items);
    } catch (error) {
        return errorResponse(500, "Failed to fetch reservations.", error);
    }
};

/**
 * Create a new reservation
 */
const createReservation = async (body) => {
    const { tableId, userId, time } = body;
    if (!tableId || !userId || !time) return errorResponse(400, "Missing required fields.");

    const newItem = {
        id: uuidv4(),
        tableId,
        userId,
        time,
        createdAt: new Date().toISOString(),
    };

    const params = {
        TableName: RESERVATIONS_TABLE,
        Item: newItem,
    };

    try {
        await dynamoDb.put(params).promise();
        return successResponse(201, "Reservation created successfully.", newItem);
    } catch (error) {
        return errorResponse(500, "Failed to create reservation.", error);
    }
};

/**
 * Helper function to return success responses
 */
const successResponse = (statusCode, message, data = null) => {
    return {
        statusCode,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message, data }),
    };
};

/**
 * Helper function to return error responses
 */
const errorResponse = (statusCode, message, error = null) => {
    console.error("Error:", error);
    return {
        statusCode,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ error: message, details: error ? error.message : null }),
    };
};
