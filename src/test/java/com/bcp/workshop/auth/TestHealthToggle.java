package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import java.util.concurrent.atomic.AtomicBoolean;

@Readiness
@ApplicationScoped
public class TestHealthToggle implements HealthCheck {
    private static final AtomicBoolean FORCE_DOWN = new AtomicBoolean(false);

    static void forceDown(boolean value) {
        FORCE_DOWN.set(value);
    }

    @Override
    public HealthCheckResponse call() {
        if (FORCE_DOWN.get()) {
            return HealthCheckResponse.down("test-readiness-toggle");
        }
        return HealthCheckResponse.up("test-readiness-toggle");
    }
}
