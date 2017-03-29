package com.twilio;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;
import static spark.Spark.afterAfter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javafaker.Faker;
import com.google.gson.Gson;
import com.google.gson.JsonObject;


import com.twilio.jwt.accesstoken.*;
import com.twilio.rest.notify.v1.service.BindingCreator;
import com.twilio.rest.notify.v1.service.Binding;
import com.twilio.rest.notify.v1.service.Notification;
import com.twilio.rest.notify.v1.service.NotificationCreator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.ModelAndView;

public class ServerApp {

    final static Logger logger = LoggerFactory.getLogger(ServerApp.class);

    private class BindingRequest {
        String endpoint;
        String identity;
        String BindingType;
        String Address;
    }

    private static class BindingResponse {
        String message;
        String error;
    }

    private static class SendNotificationResponse {
        String message;
        String error;
    }


    public static void main(String[] args) {

        // Serve static files from src/main/resources/public
        staticFileLocation("/public");

        // Create a Faker instance to generate a random username for the connecting user
        Faker faker = new Faker();


        // Set up configuration from environment variables
        Map<String, String> configuration = new HashMap<>();
        configuration.put("TWILIO_ACCOUNT_SID", System.getenv("TWILIO_ACCOUNT_SID"));
        configuration.put("TWILIO_API_KEY", System.getenv("TWILIO_API_KEY"));
        configuration.put("TWILIO_API_SECRET", System.getenv("TWILIO_API_SECRET"));
        configuration.put("TWILIO_NOTIFICATION_SERVICE_SID", System.getenv("TWILIO_NOTIFICATION_SERVICE_SID"));
        configuration.put("TWILIO_CONFIGURATION_SID",System.getenv("TWILIO_CONFIGURATION_SID"));
        configuration.put("TWILIO_CHAT_SERVICE_SID",System.getenv("TWILIO_CHAT_SERVICE_SID"));
        configuration.put("TWILIO_SYNC_SERVICE_SID",System.getenv("TWILIO_SYNC_SERVICE_SID"));

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

        // Create an access token using our Twilio credentials
        get("/token", "application/json", (request, response) -> {
            // Generate a random username for the connecting client
            String identity = faker.firstName() + faker.lastName() + faker.zipCode();

            // Create an endpoint ID which uniquely identifies the user on their current device
            String appName = "TwilioAppDemo";

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
                IpMessagingGrant grant = new IpMessagingGrant();
                grant.setServiceSid(configuration.get("TWILIO_CHAT_SERVICE_SID"));
                grants.add(grant);
            }

            // Add Video grant if configured
            if (configuration.containsKey("TWILIO_CONFIGURATION_SID")) {
                VideoGrant grant  = new VideoGrant();
                grant.setConfigurationProfileSid(configuration.get("TWILIO_CONFIGURATION_SID"));
                grants.add(grant);
            }

            builder.grants(grants);

            AccessToken token = builder.build();


            // create JSON response payload
            HashMap<String, String> json = new HashMap<String, String>();
            json.put("identity", identity);
            json.put("token", token.toJwt());

            // Render JSON response
            Gson gson = new Gson();
            response.type("application/json");
            return gson.toJson(json);
        });


        post("/register", (request, response) -> {

            // Authenticate with Twilio
            Twilio.init(configuration.get("TWILIO_API_KEY"),configuration.get("TWILIO_API_SECRET"),configuration.get("TWILIO_ACCOUNT_SID"));

            logger.debug(request.body());

            // Decode the JSON Body
            Gson gson = new Gson();
            BindingRequest bindingRequest = gson.fromJson(request.body(), BindingRequest.class);


            // Create a binding
            Binding.BindingType bindingType = Binding.BindingType.forValue(bindingRequest.BindingType);
            BindingCreator creator = Binding.creator(configuration.get("TWILIO_NOTIFICATION_SERVICE_SID"),
                    bindingRequest.endpoint, bindingRequest.identity, bindingType, bindingRequest.Address);

            try {
                Binding binding = creator.create();
                logger.info("Binding successfully created");
                logger.debug(binding.toString());

                // Send a JSON response indicating success
                BindingResponse bindingResponse = new BindingResponse();
                bindingResponse.message = "Binding Created";
                response.type("application/json");
                return gson.toJson(bindingResponse);

            } catch (Exception ex) {
                logger.error("Exception creating binding: " + ex.getMessage(), ex);

                // Send a JSON response indicating an error
                BindingResponse bindingResponse = new BindingResponse();
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

            try {
                // Get the identity
                String identity = request.raw().getParameter("identity");
                logger.info("Identity: " + identity);

                // Create the notification
                String serviceSid = configuration.get("TWILIO_NOTIFICATION_SERVICE_SID");
                Notification notification = Notification
                    .creator(serviceSid)
                    .setBody("Hello " + identity)
                    .setIdentity(identity)
                    .create();
                logger.info("Notification successfully created");
                logger.debug(notification.toString());

                // Send a JSON response indicating success
                SendNotificationResponse sendNotificationResponse = new SendNotificationResponse();
                sendNotificationResponse.message = "Notification Created";
                response.type("application/json");
                return new Gson().toJson(sendNotificationResponse);

            } catch (Exception ex) {
                logger.error("Exception creating notification: " + ex.getMessage(), ex);

                // Send a JSON response indicating an error
                SendNotificationResponse sendNotificationResponse = new SendNotificationResponse();
                sendNotificationResponse.message = "Failed to create notification: " + ex.getMessage();
                sendNotificationResponse.error = ex.getMessage();
                response.type("application/json");
                response.status(500);
                return new Gson().toJson(sendNotificationResponse);
            }
        });
    }
}