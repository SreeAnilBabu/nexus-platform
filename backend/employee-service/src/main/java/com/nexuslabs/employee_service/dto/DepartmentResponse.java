package com.nexuslabs.employee_service.dto;

public class DepartmentResponse {
    private Long id;
    private String name;

    public DepartmentResponse(Long id, String name){
        this.id = id;
        this.name = name;
    }
    public Long getId() {
        return id;
    }
    public String getName(){
        return name;
    }
}