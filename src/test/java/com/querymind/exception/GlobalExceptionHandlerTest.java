package com.querymind.exception;

import com.querymind.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void shouldHandleQueryExecutionException() {
        QueryExecutionException ex = new QueryExecutionException("QUERY_TIMEOUT", "Query took too long.");

        ResponseEntity<ApiError> response = exceptionHandler.handleQueryExecutionException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("QUERY_TIMEOUT");
        assertThat(response.getBody().message()).isEqualTo("Query took too long.");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldHandleGenericExceptionWithoutLeakingDetails() {
        Exception ex = new RuntimeException("Secret DB password in stack trace");

        ResponseEntity<ApiError> response = exceptionHandler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message())
                .isEqualTo("An unexpected error occurred while processing the request.");
        assertThat(response.getBody().message()).doesNotContain("Secret");
    }
}
