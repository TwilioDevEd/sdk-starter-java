package com.twilio;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.*;

public class Configuration {

    private String accountSid;
    private String apiKey;
    private String apiSecret;
    private String notificationServiceSid;
    private String chatServiceSid;
    private String syncServiceSid;

    public Configuration() {
        accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        apiKey = System.getenv("TWILIO_API_KEY");
        apiSecret = System.getenv("TWILIO_API_SECRET");
        notificationServiceSid = System.getenv("TWILIO_NOTIFICATION_SERVICE_SID");
        chatServiceSid = System.getenv("TWILIO_CHAT_SERVICE_SID");
        syncServiceSid = System.getenv("TWILIO_SYNC_SERVICE_SID");
    }

    public String getAccountSid() {
        return accountSid;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getNotificationServiceSid() {
        return notificationServiceSid;
    }

    public String getChatServiceSid() {
        return chatServiceSid;
    }

    public String getSyncServiceSid() {
        return syncServiceSid;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("TWILIO_ACCOUNT_SID", getAccountSid());
        map.put("TWILIO_API_KEY", getApiKey());
        map.put("TWILIO_API_SECRET", !isEmpty(getApiKey()));
        map.put("TWILIO_NOTIFICATION_SERVICE_SID", getNotificationServiceSid());
        map.put("TWILIO_CHAT_SERVICE_SID", getChatServiceSid());
        map.put("TWILIO_SYNC_SERVICE_SID", getSyncServiceSid());
        return map;
    }
}
