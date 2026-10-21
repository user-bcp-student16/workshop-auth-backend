package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class HealthEndpointTest {
    @AfterEach
    void clearHealthOverride() {
        TestHealthToggle.forceDown(false);
    }

    @Test
    void healthEndpointReturnsUpInHappyPath() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", notNullValue());
    }

    @Test
    void healthEndpointIgnoresUnexpectedQueryParamAsEdgeCase() {
        given()
                .queryParam("source", "integration-test")
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void healthEndpointReturns503WhenAReadinessCheckIsDown() {
        TestHealthToggle.forceDown(true);

        given()
                .when().get("/q/health")
                .then()
                .statusCode(503)
                .body("status", is("DOWN"))
                .body("checks", notNullValue());
    }
}
