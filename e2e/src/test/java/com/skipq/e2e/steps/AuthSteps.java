package com.skipq.e2e.steps;

import com.skipq.e2e.context.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthSteps {

    private final TestContext ctx;

    public AuthSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @Given("a STUDENT account is logged in")
    public void loginAsStudent() {
        login(TestContext.STUDENT_EMAIL, TestContext.STUDENT_PASSWORD, "STUDENT");
    }

    @Given("a GENERAL account is logged in")
    public void loginAsGeneral() {
        login(TestContext.GENERAL_EMAIL, TestContext.GENERAL_PASSWORD, "GENERAL");
    }

    @Then("the response status is {int}")
    public void responseStatusIs(int status) {
        assertThat(ctx.getLastResponse().statusCode()).isEqualTo(status);
    }

    @Then("the response contains a valid token")
    public void responseContainsToken() {
        String token = ctx.getLastResponse().jsonPath().getString("token");
        assertThat(token).isNotBlank();
    }

    private void login(String email, String password, String role) {
        Response response = RestAssured
                .given()
                .baseUri(TestContext.BASE_URL)
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "%s", "password": "%s", "role": "%s"}
                        """.formatted(email, password, role))
                .post("/api/v1/auth/login");

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.setToken(response.jsonPath().getString("token"));
        ctx.setLastResponse(response);
    }
}
