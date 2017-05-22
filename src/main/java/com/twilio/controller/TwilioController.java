package com.twilio.controller;

import com.github.javafaker.Faker;
import com.google.gson.Gson;
import com.twilio.Configuration;
import com.twilio.jwt.accesstoken.*;
import com.twilio.util.BindingRequest;
import com.twilio.util.TwilioWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class TwilioController {

    final static Logger logger = LoggerFactory.getLogger(TwilioController.class);

    private Configuration configuration;
    private Faker faker;
    private TwilioWrapper twilioWrapper;

    private static class BindingResponse {
        String message;
        String error;
    }

    private static class SendNotificationResponse {
        String message;
        String error;
    }

    public TwilioController(Configuration configuration, Faker faker, TwilioWrapper twilioWrapper) {
        this.configuration = configuration;
        this.faker = faker;
        this.twilioWrapper = twilioWrapper;
    }

    public TwilioController() {
        configuration = new Configuration();
        twilioWrapper = new TwilioWrapper(configuration.getApiKey(),
                configuration.getApiSecret(),
                configuration.getAccountSid());

        // Create a Faker instance to generate a random username for the connecting user
        Faker faker = new Faker();
    }

    public Route configRoute = (request, response) -> {
        // Render JSON response
        Gson gson = new Gson();
        response.type("application/json");
        return gson.toJson(configuration.toMap());
    };

    public Route tokenRoute = (request, response) -> {
        // Generate a random username for the connecting client
        String identity = faker.firstName() + faker.lastName() + faker.zipCode();

        // Create access token builder
        AccessToken.Builder builder = new AccessToken.Builder(
                configuration.getAccountSid(),
                configuration.getApiKey(),
                configuration.getApiSecret()
        ).identity(identity);

        List<Grant> grants = new ArrayList<>();

        // Add Sync grant if configured
        if (isNotBlank(configuration.getSyncServiceSid())) {
            SyncGrant grant = new SyncGrant();
            grant.setServiceSid(configuration.getSyncServiceSid());
            grants.add(grant);
        }

        // Add Chat grant if configured
        if (isNotBlank(configuration.getChatServiceSid())) {
            IpMessagingGrant grant = new IpMessagingGrant();
            grant.setServiceSid(configuration.getChatServiceSid());
            grants.add(grant);
        }

        // Add Video grant
        VideoGrant grant = new VideoGrant();
        grants.add(grant);

        builder.grants(grants);

        AccessToken token = builder.build();

        // create JSON response payload
        HashMap<String, String> json = new HashMap<>();
        json.put("identity", identity);
        json.put("token", token.toJwt());

        // Render JSON response
        Gson gson = new Gson();
        response.type("application/json");
        return gson.toJson(json);
    };

    public Route registerRoute = (request, response) -> {

        logger.debug(request.body());

        // Decode the JSON Body
        Gson gson = new Gson();
        BindingRequest bindingRequest = gson.fromJson(request.body(), BindingRequest.class);

        try {
            // Create a binding
            twilioWrapper.createBinding(bindingRequest, configuration.getNotificationServiceSid());

            // Send a JSON response indicating success
            TwilioController.BindingResponse bindingResponse = new TwilioController.BindingResponse();
            bindingResponse.message = "Binding Created";
            response.type("application/json");
            return gson.toJson(bindingResponse);

        } catch (Exception ex) {
            logger.error("Exception creating binding: " + ex.getMessage(), ex);

            // Send a JSON response indicating an error
            TwilioController.BindingResponse bindingResponse = new TwilioController.BindingResponse();
            bindingResponse.message = "Failed to create binding: " + ex.getMessage();
            bindingResponse.error = ex.getMessage();
            response.type("application/json");
            response.status(500);
            return gson.toJson(bindingResponse);
        }
    };

    public Route sendNotificationRoute = (request, response) -> {
        try {
            // Get the identity
            String identity = request.raw().getParameter("identity");
            logger.info("Identity: " + identity);

            String serviceSid = configuration.getNotificationServiceSid();
            twilioWrapper.sendNotification(identity, serviceSid);

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
    };
}
