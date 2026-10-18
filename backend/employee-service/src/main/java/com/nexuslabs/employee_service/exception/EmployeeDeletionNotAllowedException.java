package com.nexuslabs.employee_service.exception;

public class EmployeeDeletionNotAllowedException extends RuntimeException {

    public EmployeeDeletionNotAllowedException(String message) {
        super(message);
    }
}