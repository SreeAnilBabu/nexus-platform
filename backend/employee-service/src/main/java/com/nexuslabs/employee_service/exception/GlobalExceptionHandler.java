package com.nexuslabs.employee_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.HashMap;

@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String,String>> handleEmployeeNotFound(EmployeeNotFoundException ex){
        Map<String,String> response = Map.of("error",ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>>handleValidationErrors(MethodArgumentNotValidException ex){

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    @ExceptionHandler(DuplicateEmployeeEmailException.class)
    public  ResponseEntity<Map<String,String>> handleDuplicateEmployeeEmail(DuplicateEmployeeEmailException ex){

        Map<String,String> response = Map.of("error",ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    @ExceptionHandler(InvalidRequestParameterException.class)
    public ResponseEntity<Map<String,String>> handleInvalidRequestParameter(InvalidRequestParameterException ex){
        Map<String,String> error = new HashMap<>();
        error.put("error","Bad Request");
        error.put("message",ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}