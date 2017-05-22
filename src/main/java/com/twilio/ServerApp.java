package com.twilio;

import com.github.javafaker.Faker;
import com.twilio.controller.TwilioController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;


public class ServerApp {

    final static Logger logger = LoggerFactory.getLogger(ServerApp.class);

    private TwilioController twilioController;



    public static void main(String[] args) {

        // Serve static files from src/main/resources/public
        staticFileLocation("/public");

        // Create a Faker instance to generate a random username for the connecting user
        Faker faker = new Faker();

        TwilioController twilioController = new TwilioController();

        // Log all requests and responses
        afterAfter(new LoggingFilter());

        // Get the configuration for variables for the health check
        get("/config", twilioController.configRoute);

        // Create an access token using our Twilio credentials
        get("/token", "application/json", twilioController.tokenRoute);

        post("/register", twilioController.registerRoute);

        post("/send-notification", twilioController.sendNotificationRoute);
    }
}