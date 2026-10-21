package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void returns400WhenRequestIsNull() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void returns400WhenUsernameIsMissing() {
        given()
                .contentType("application/json")
                .body("{\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void returns400WhenUsernameIsBlank() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"   \",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void returns400WhenPasswordIsMissing() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void returns400WhenPasswordIsBlank() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\" \",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void returns401WhenCredentialsAreInvalid() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"wrong\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student or password"));
    }

    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("token", notNullValue());
    }

    @Test
    void logsInWithoutOptionalClientField() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("token", notNullValue());
    }

    @Test
    void rejectsSqlInjectionAttempt() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"' OR '1'='1' --\",\"password\":\"irrelevant\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void storesSeedPasswordAsBcryptHash() throws Exception {
        String storedPassword = readStoredPassword("student");
        assertNotEquals("Workshop123!", storedPassword);
        assertTrue(storedPassword.startsWith("$2"));
    }

    @Test
    void tokenIsSignedWithClaimsAndNoEmbeddedSecret() throws Exception {
        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertTrue(payloadJson.contains("\"sub\":\"student\""));
        assertTrue(payloadJson.contains("\"role\":\"STUDENT\""));
        assertTrue(payloadJson.contains("\"exp\":"));
        assertTrue(!payloadJson.contains("WORKSHOP_SECRET_2026"));

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput, "WORKSHOP_SECRET_2026");
        assertEquals(expectedSignature, parts[2]);
    }

    @Test
    void tokenUsesSecretFromConfiguration() throws Exception {
        String previous = System.getProperty("auth.token.secret");
        System.setProperty("auth.token.secret", "TEST_SECRET_123");
        try {
            String token = given()
                    .contentType("application/json")
                    .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                    .when().post("/auth/login")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("token");

            String[] parts = token.split("\\.");
            String signingInput = parts[0] + "." + parts[1];
            assertEquals(sign(signingInput, "TEST_SECRET_123"), parts[2]);
        } finally {
            if (previous == null) {
                System.clearProperty("auth.token.secret");
            } else {
                System.setProperty("auth.token.secret", previous);
            }
        }
    }

    @Test
    void corsOriginsIsRestrictedByConfigProperty() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));
        assertTrue(properties.contains("quarkus.http.cors.origins=${AUTH_CORS_ORIGINS:http://localhost:4200}"));
    }

    @Test
    void errorResponseIsGenericAndDoesNotExposeInternalDetails() {
        AuthResource resource = new AuthResource();
        resource.authService = new AuthService() {
            @Override
            public LoginResponse login(String username, String password) {
                throw new RuntimeException("boom");
            }
        };

        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";

        Response response = resource.login(request);
        assertEquals(500, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertEquals("Internal server error", body.get("message"));
        assertTrue(!body.containsKey("exception"));
    }

    @Test
    void pomUsesUpdatedDependencyVersions() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        assertTrue(pom.contains("<quarkus.platform.version>3.29.2</quarkus.platform.version>"));
        assertTrue(pom.contains("<artifactId>commons-text</artifactId>"));
        assertTrue(pom.contains("<version>1.13.0</version>"));
    }

    private String sign(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String readStoredPassword(String username) throws Exception {
        String dbUrl = System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
        String sql = "SELECT password FROM legacy_users WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(dbUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getString("password");
                }
            }
        }
        return null;
    }
}
