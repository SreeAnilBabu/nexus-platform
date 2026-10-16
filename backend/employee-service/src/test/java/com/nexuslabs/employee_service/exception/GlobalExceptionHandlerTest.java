
package com.nexuslabs.employee_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturnConflictForDatabaseIntegrityViolation() {

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Simulated database constraint violation"
                );

        ResponseEntity<Map<String, String>> response =
                handler.handleDataIntegrityViolation(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals("Conflict", response.getBody().get("error"));

        assertEquals(
                "The requested operation conflicts with existing database data.",
                response.getBody().get("message")
        );
    }
}
