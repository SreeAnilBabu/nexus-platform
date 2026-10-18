package com.nexuslabs.employee_service.exception;

public class InvalidEmploymentStatusTransitionException
        extends RuntimeException {

    public InvalidEmploymentStatusTransitionException(String message) {
        super(message);
    }
}
