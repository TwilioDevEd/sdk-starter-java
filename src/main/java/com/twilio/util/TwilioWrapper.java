package com.twilio.util;

import com.google.gson.Gson;
import com.twilio.Twilio;
import com.twilio.rest.notify.v1.service.Binding;
import com.twilio.rest.notify.v1.service.BindingCreator;
import com.twilio.rest.notify.v1.service.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwilioWrapper {

    final static Logger logger = LoggerFactory.getLogger(TwilioWrapper.class);

    private final Gson gson;

    public TwilioWrapper(String apiKey, String apiSecret, String accountSid) {
        Twilio.init(apiKey, apiSecret, accountSid);
        this.gson = new Gson();
    }

    public void sendNotification(String identity, String serviceSid) {
        // Create the notification
        Notification notification = Notification
                .creator(serviceSid)
                .setBody("Hello " + identity)
                .setIdentity(identity)
                .create();
        logger.info("Notification successfully created");
        logger.debug(notification.toString());
    }

    public void createBinding(BindingRequest bindingRequest, String notificationServiceSid) {
        Binding.BindingType bindingType = Binding.BindingType.forValue(bindingRequest.getBindingType());
        BindingCreator creator = Binding.creator(notificationServiceSid,
                bindingRequest.getEndpoint(),
                bindingType,
                bindingRequest.getAddress());
        Binding binding = creator.create();
        logger.info("Binding successfully created");
        logger.debug(binding.toString());
    }
}
