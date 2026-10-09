package com.querymind.dto;

import java.time.Instant;

/**
 * Standard error response envelope.
 * Exposes a clean application error code and safe message without revealing internal details or stack traces.
 */
public record ApiError(
        String code,
        String message,
        Instant timestamp
) {
    public ApiError(String code, String message) {
        this(code, message, Instant.now());
    }
}
