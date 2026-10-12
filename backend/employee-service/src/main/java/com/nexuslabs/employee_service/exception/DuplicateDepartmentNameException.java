package com.nexuslabs.employee_service.exception;

public class DuplicateDepartmentNameException extends RuntimeException{
    public DuplicateDepartmentNameException(String name){
        super("Department already exists: " + name);
    }
}