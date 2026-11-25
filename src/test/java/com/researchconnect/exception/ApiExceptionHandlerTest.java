package com.researchconnect.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler exceptionHandler = new ApiExceptionHandler();

    @Test
    void applicationFailuresReturnServerErrorInsteadOfConflict() {
        var response = exceptionHandler.state(new IllegalStateException("Firestore is unavailable"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                "The request could not be completed. Please try again later.",
                response.getBody().get("message"));
    }
}
