package com.nexuslabs.employee_service.exception;

public class DuplicateEmployeeEmailException extends RuntimeException{
    public DuplicateEmployeeEmailException(String email){
        super("Employee already exists with email "+email);
    }
}