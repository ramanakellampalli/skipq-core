package com.skipq.e2e.context;

import io.restassured.response.Response;

public class TestContext {

    public static final String BASE_URL = System.getenv().getOrDefault("LDT_BASE_URL", "https://api-dev.ohyeahsaas.com");

    public static final String STUDENT_EMAIL    = System.getenv("LDT_STUDENT_EMAIL");
    public static final String STUDENT_PASSWORD = System.getenv("LDT_STUDENT_PASSWORD");
    public static final String GENERAL_EMAIL    = System.getenv("LDT_GENERAL_EMAIL");
    public static final String GENERAL_PASSWORD = System.getenv("LDT_GENERAL_PASSWORD");

    // shared state across steps within a scenario
    private String token;
    private Response lastResponse;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Response getLastResponse() { return lastResponse; }
    public void setLastResponse(Response lastResponse) { this.lastResponse = lastResponse; }
}
