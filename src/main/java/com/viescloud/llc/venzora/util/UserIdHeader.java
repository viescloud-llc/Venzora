package com.viescloud.llc.venzora.util;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Helper for custom controllers (orchestrator, self-service, etc.) that need to read
 * the {@code user_id} header and validate it as a UUID. Centralizes the "missing /
 * malformed" error responses so every endpoint behaves the same way.
 */
public final class UserIdHeader {

    private UserIdHeader() {}

    /** Returns the parsed UUID, or throws 401 (missing / unauthenticated sentinel) / 400 (malformed). */
    public static UUID require(String header) {
        if (header == null || header.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user_id header required");
        }
        // "0" and "-1" are the framework's no-authenticated-user sentinels (the
        // controller default and the JWT filter's fallback). An expired/invalid
        // token surfaces as one of these — that's an auth failure, not a
        // malformed request.
        if ("0".equals(header) || "-1".equals(header)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated (token missing or expired)");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id must be a UUID");
        }
    }
}
