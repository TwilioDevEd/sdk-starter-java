package com.twilio;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;
import static spark.Spark.afterAfter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javafaker.Faker;
import com.google.common.base.CaseFormat;
import com.google.gson.Gson;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.twilio.jwt.accesstoken.*;
import com.twilio.rest.notify.v1.service.BindingCreator;
import com.twilio.rest.notify.v1.service.Binding;
import com.twilio.rest.notify.v1.service.Notification;

import com.twilio.rest.notify.v1.service.NotificationCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApp {

    final static Logger logger = LoggerFactory.getLogger(ServerApp.class);

    private static class Response {
        String message;
        String error;
    }

    public static void main(String[] args) {

        // Serve static files from src/main/resources/public
        staticFileLocation("/public");


        // Set up configuration from environment variables
        Map<String, String> configuration = new HashMap<>();
        configuration.put("TWILIO_ACCOUNT_SID", System.getenv("TWILIO_ACCOUNT_SID"));
        configuration.put("TWILIO_API_KEY", System.getenv("TWILIO_API_KEY"));
        configuration.put("TWILIO_API_SECRET", System.getenv("TWILIO_API_SECRET"));
        configuration.put("TWILIO_NOTIFICATION_SERVICE_SID", System.getenv("TWILIO_NOTIFICATION_SERVICE_SID"));
        configuration.put("TWILIO_CHAT_SERVICE_SID",System.getenv("TWILIO_CHAT_SERVICE_SID"));
        String syncServiceSID = System.getenv("TWILIO_SYNC_SERVICE_SID");
        configuration.put("TWILIO_SYNC_SERVICE_SID",syncServiceSID != null ? syncServiceSID : "default");

        // Log all requests and responses
        afterAfter(new LoggingFilter());

        // Get the configuration for variables for the health check
        get("/config", (request, response) -> {

            Map<String, Object> json = new HashMap<>();
            String apiSecret = configuration.get("TWILIO_API_SECRET");
            boolean apiSecretConfigured = (apiSecret != null) && !apiSecret.isEmpty();

            json.putAll(configuration);
            json.put("TWILIO_API_SECRET", apiSecretConfigured);


            // Render JSON response
            Gson gson = new Gson();
            response.type("application/json");
            return gson.toJson(json);
        });

        // Create an access token with the provided identity using our Twilio credentials
        post("/token", (request, response) -> {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> props = gson.fromJson(request.body(), type);

            String identity = (String)props.get("identity");

            // create JSON response payload
            HashMap<String, String> json = new HashMap<>();
            json.put("identity", identity);
            json.put("token", generateToken(configuration, identity));

            // Render JSON response
            response.type("application/json");
            return gson.toJson(json);
        });

        // Create an access token using our Twilio credentials
        get("/token", "application/json", (request, response) -> {
            // Create a Faker instance to generate a random username for the connecting user
            Faker faker = new Faker();
            // Generate a random username for the connecting client
            String identity = faker.firstName() + faker.lastName() + faker.zipCode();

            // create JSON response payload
            HashMap<String, String> json = new HashMap<>();
            json.put("identity", identity);
            json.put("token", generateToken(configuration, identity));

            // Render JSON response
            Gson gson = new Gson();
            response.type("application/json");
            return gson.toJson(json);
        });

        post("/register", (request, response) -> {

            // Authenticate with Twilio
            Twilio.init(configuration.get("TWILIO_API_KEY"),configuration.get("TWILIO_API_SECRET"),configuration.get("TWILIO_ACCOUNT_SID"));

            logger.debug(request.body());

            Gson gson = new Gson();

            try {
                // Decode the JSON Body into a map
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> props = gson.fromJson(request.body(), type);
                props = camelCaseKeys(props);

                // Convert BindingType from Object to enum value
                Binding.BindingType bindingType = Binding.BindingType.forValue((String) props.get("bindingType"));
                props.put("bindingType", bindingType);

                // Add the notification service sid
                String serviceSid = configuration.get("TWILIO_NOTIFICATION_SERVICE_SID");
                props.put("pathServiceSid", serviceSid);

                // Create the binding
                JsonElement jsonElement = gson.toJsonTree(props);
                BindingCreator bindingCreator = gson.fromJson(jsonElement, BindingCreator.class);
                Binding binding = bindingCreator.create();

                logger.info("Binding successfully created");
                logger.debug(binding.toString());

                // Send a JSON response indicating success
                Response bindingResponse = new Response();
                bindingResponse.message = "Binding Created";
                response.type("application/json");
                return gson.toJson(bindingResponse);

            } catch (Exception ex) {
                logger.error("Exception creating binding: " + ex.getMessage(), ex);

                // Send a JSON response indicating an error
                Response bindingResponse = new Response();
                bindingResponse.message = "Failed to create binding: " + ex.getMessage();
                bindingResponse.error = ex.getMessage();
                response.type("application/json");
                response.status(500);
                return gson.toJson(bindingResponse);
            }
        });

        post("/send-notification", (request, response) -> {

            // Authenticate with Twilio
            Twilio.init(configuration.get("TWILIO_API_KEY"),configuration.get("TWILIO_API_SECRET"),configuration.get("TWILIO_ACCOUNT_SID"));

            logger.debug(request.body());

            Gson gson = new Gson();

            try {
                // Decode the JSON Body into a map
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> props = gson.fromJson(request.body(), type);
                props = camelCaseKeys(props);

                if (props.containsKey("priority")) {
                    // Convert Priority from Object to enum value
                    Notification.Priority priority = Notification.Priority.forValue((String) props.get("priority"));
                    props.put("priority", priority);
                }

                // Add the notification service sid
                String serviceSid = configuration.get("TWILIO_NOTIFICATION_SERVICE_SID");
                props.put("pathServiceSid", serviceSid);

                // Create the notification
                JsonElement jsonElement = gson.toJsonTree(props);
                NotificationCreator notificationCreator = gson.fromJson(jsonElement, NotificationCreator.class);
                Notification notification = notificationCreator.create();

                logger.info("Notification successfully created");
                logger.debug(notification.toString());

                // Send a JSON response indicating success
                Response sendNotificationResponse = new Response();
                sendNotificationResponse.message = "Notification Created";
                response.type("application/json");
                return new Gson().toJson(sendNotificationResponse);

            } catch (Exception ex) {
                logger.error("Exception creating notification: " + ex.getMessage(), ex);

                // Send a JSON response indicating an error
                Response sendNotificationResponse = new Response();
                sendNotificationResponse.message = "Failed to create notification: " + ex.getMessage();
                sendNotificationResponse.error = ex.getMessage();
                response.type("application/json");
                response.status(500);
                return new Gson().toJson(sendNotificationResponse);
            }
        });
    }

    private static String generateToken(Map<String, String> configuration, String identity) {

        // Create access token builder
        AccessToken.Builder builder = new AccessToken.Builder(
                configuration.get("TWILIO_ACCOUNT_SID"),
                configuration.get("TWILIO_API_KEY"),
                configuration.get("TWILIO_API_SECRET")
        ).identity(identity);

        List<Grant> grants = new ArrayList<>();

        // Add Sync grant if configured
        if (configuration.containsKey("TWILIO_SYNC_SERVICE_SID")) {
            SyncGrant grant = new SyncGrant();
            grant.setServiceSid(configuration.get("TWILIO_SYNC_SERVICE_SID"));
            grants.add(grant);
        }

        // Add Chat grant if configured
        if (configuration.containsKey("TWILIO_CHAT_SERVICE_SID")) {
            ChatGrant grant = new ChatGrant();
            grant.setServiceSid(configuration.get("TWILIO_CHAT_SERVICE_SID"));
            grants.add(grant);
        }

        // Add Video grant
        VideoGrant grant  = new VideoGrant();
        grants.add(grant);

        builder.grants(grants);

        AccessToken token = builder.build();

        return token.toJwt();
    }

    // Convert keys to camelCase to conform with the twilio-java api definition contract
    private static Map<String, Object> camelCaseKeys(Map<String, Object> map) {
        Map<String, Object> newMap = new HashMap<>();
        map.forEach((k,v) -> {
            String newKey = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, k);
            newMap.put(newKey, v);
        });
        return newMap;
    }

}
