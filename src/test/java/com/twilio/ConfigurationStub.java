package com.twilio;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class ConfigurationStub extends Configuration {
    public String getAccountSid() {
        return "accountSid";
    }

    public String getApiKey() {
        return "apiKey";
    }

    public String getApiSecret() {
        return "apiSecret";
    }

    public String getNotificationServiceSid() {
        return "notificationServiceSid";
    }

    public String getChatServiceSid() {
        return "chatServiceSid";
    }

    public String getSyncServiceSid() {
        return "syncServiceSid";
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
