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

    /** Returns the parsed UUID, or throws 401 (missing) / 400 (malformed). */
    public static UUID require(String header) {
        if (header == null || header.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user_id header required");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id must be a UUID");
        }
    }
}
