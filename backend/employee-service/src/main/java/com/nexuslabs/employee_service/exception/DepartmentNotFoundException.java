package com.nexuslabs.employee_service.exception;

public class DepartmentNotFoundException extends RuntimeException{
    public DepartmentNotFoundException(long id) {
        super("Department not found with id: "+id);
    }
}