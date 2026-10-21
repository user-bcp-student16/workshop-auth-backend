package com.bcp.workshop.auth;

import org.eclipse.microprofile.config.ConfigProvider;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class LegacyTokenUtil {
    private static final String TOKEN_SECRET_KEY = "auth.token.secret";
    private static final String TOKEN_SECRET_DEFAULT = "WORKSHOP_SECRET_2026";
    private static final long TOKEN_TTL_SECONDS = 3600;

    private LegacyTokenUtil() {}

    public static String create(String username, String role) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiry = issuedAt + TOKEN_TTL_SECONDS;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"" + escapeJson(username) + "\",\"role\":\"" + escapeJson(role) + "\",\"iat\":" + issuedAt + ",\"exp\":" + expiry + "}";

        String header = base64UrlEncode(headerJson);
        String payload = base64UrlEncode(payloadJson);
        String signingInput = header + "." + payload;
        String signature = sign(signingInput, resolveTokenSecret());

        return signingInput + "." + signature;
    }

    private static String resolveTokenSecret() {
        return ConfigProvider.getConfig().getOptionalValue(TOKEN_SECRET_KEY, String.class).orElse(TOKEN_SECRET_DEFAULT);
    }

    private static String sign(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("Unable to sign token", error);
        }
    }

    private static String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
