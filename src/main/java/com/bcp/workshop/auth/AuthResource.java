package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOGGER = Logger.getLogger(AuthResource.class);

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            String validationError = validateRequest(request);
            if (validationError != null) {
                return Response.status(400).entity(Map.of("message", validationError)).build();
            }

            LoginResponse response = authService.login(request.username, request.password);
            if (response == null) {
                return Response.status(401).entity(Map.of("message", "Invalid user " + request.username + " or password")).build();
            }

            return Response.ok(response).build();
        } catch (Exception error) {
            LOGGER.error("Unexpected error during login", error);
            return Response.status(500).entity(Map.of("message", "Internal server error")).build();
        }
    }

    private String validateRequest(LoginRequest request) {
        if (request == null) {
            return "Request is null";
        }
        if (request.username == null || request.username.trim().isEmpty()) {
            return "Username is required";
        }
        if (request.password == null || request.password.trim().isEmpty()) {
            return "Password is required";
        }
        return null;
    }
}
