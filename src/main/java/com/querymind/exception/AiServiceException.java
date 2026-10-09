package com.querymind.exception;

/**
 * Exception thrown when the AI model provider fails, times out, or returns an unprocessable response.
 */
public class AiServiceException extends RuntimeException {

    private final String errorCode;

    public AiServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
