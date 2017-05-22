package com.twilio.controller;

import com.github.javafaker.Faker;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twilio.ConfigurationStub;
import com.twilio.util.BindingRequest;
import com.twilio.util.TwilioWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import spark.Request;
import spark.Response;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TwilioControllerTest {

    private TwilioController subject;

    @Mock
    private Request mockRequest;
    @Mock
    private Response mockResponse;
    @Mock
    private TwilioWrapper mockTwilioWrapper;
    @Mock
    private Faker mockFaker;

    JsonParser jsonParser = new JsonParser();

    @Before
    public void setup(){
        subject = new TwilioController(new ConfigurationStub(), mockFaker, mockTwilioWrapper);
    }

    @Test
    public void config_returnsTrueForApiSecretEnvironmentVariablePresent() throws Exception {
        // when
        String response = (String) subject.configRoute.handle(mockRequest, mockResponse);

        // then
        assertThat(response, containsString("\"TWILIO_API_SECRET\":true"));
    }

    @Test
    public void token_returnsATokenAndIdentity() throws Exception {
        // then
        when(mockFaker.firstName()).thenReturn("firstName");
        when(mockFaker.lastName()).thenReturn("lastName");
        when(mockFaker.zipCode()).thenReturn("zipCode");

        // when
        String response = (String) subject.tokenRoute.handle(mockRequest, mockResponse);

        // then
        JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
        assertThat(jsonResponse.get("identity").getAsString(), equalTo("firstNamelastNamezipCode"));
        assertThat(jsonResponse.get("token").getAsString(), is(notNullValue()));
    }

    @Test
    public void register_createsBinding() throws Exception {
        // then
        BindingRequest bindingRequest = new BindingRequest("endpoint", "bindingType", "address");
        Gson gson = new Gson();
        String bindingRequestJson = gson.toJson(bindingRequest);
        when(mockRequest.body()).thenReturn(bindingRequestJson);

        // when
        String response = (String) subject.registerRoute.handle(mockRequest, mockResponse);

        // then
        JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
        assertThat(jsonResponse.get("message").getAsString(), is("Binding Created"));
        verify(mockTwilioWrapper, atLeastOnce()).createBinding(bindingRequest, "notificationServiceSid");
    }

    @Test
    public void register_returnsErrorMessageWhenExceptionIsThrown() throws Exception {
        // then
        BindingRequest bindingRequest = new BindingRequest("endpoint", "bindingType", "address");
        Gson gson = new Gson();
        String bindingRequestJson = gson.toJson(bindingRequest);
        when(mockRequest.body()).thenReturn(bindingRequestJson);
        doThrow(RuntimeException.class).when(mockTwilioWrapper).createBinding(any(),any());

        // when
        String response = (String) subject.registerRoute.handle(mockRequest, mockResponse);

        // then
        JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
        assertThat(jsonResponse.get("message").getAsString(), containsString("Failed to create binding:"));
    }

    @Test
    public void sendNotification_returnsSuccessMessage() throws Exception {
        // then
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getParameter("identity")).thenReturn("myIdentity");
        when(mockRequest.raw()).thenReturn(httpServletRequest);

        // when
        String response = (String) subject.sendNotificationRoute.handle(mockRequest, mockResponse);

        // then
        JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
        assertThat(jsonResponse.get("message").getAsString(), is("Notification Created"));
        verify(mockTwilioWrapper, atLeastOnce()).sendNotification("myIdentity", "notificationServiceSid");
    }

    @Test
    public void sendNotification_returnsErrorMessageWhenExceptionIsThrown() throws Exception {
        // then
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getParameter("identity")).thenReturn("myIdentity");
        when(mockRequest.raw()).thenReturn(httpServletRequest);
        doThrow(RuntimeException.class).when(mockTwilioWrapper).sendNotification(any(),any());

        // when
        String response = (String) subject.sendNotificationRoute.handle(mockRequest, mockResponse);

        // then
        JsonObject jsonResponse = jsonParser.parse(response).getAsJsonObject();
        assertThat(jsonResponse.get("message").getAsString(), containsString("Failed to create notification:"));
    }
}