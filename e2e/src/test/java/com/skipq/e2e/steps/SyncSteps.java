package com.skipq.e2e.steps;

import com.skipq.e2e.context.TestContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SyncSteps {

    private final TestContext ctx;

    public SyncSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @When("the student syncs")
    public void sync() {
        Response response = RestAssured
                .given()
                .baseUri(TestContext.BASE_URL)
                .header("Authorization", "Bearer " + ctx.getToken())
                .get("/api/v1/student/sync");

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.setLastResponse(response);
    }

    @Then("the sync returns only campus vendors")
    public void syncReturnsCampusVendors() {
        List<Map<String, Object>> vendors = ctx.getLastResponse().jsonPath().getList("vendors");
        assertThat(vendors).isNotEmpty();
        vendors.forEach(v ->
                assertThat(v.get("campusId"))
                        .as("Expected campus vendor but got a general vendor: %s", v.get("name"))
                        .isNotNull()
        );
    }

    @Then("the sync returns only general vendors")
    public void syncReturnsGeneralVendors() {
        List<Map<String, Object>> vendors = ctx.getLastResponse().jsonPath().getList("vendors");
        assertThat(vendors).isNotEmpty();
        vendors.forEach(v ->
                assertThat(v.get("campusId"))
                        .as("Expected general vendor but got a campus vendor: %s", v.get("name"))
                        .isNull()
        );
    }

    @Then("the sync response includes a valid profile")
    public void syncIncludesProfile() {
        String email = ctx.getLastResponse().jsonPath().getString("profile.email");
        assertThat(email).isNotBlank();
    }
}
