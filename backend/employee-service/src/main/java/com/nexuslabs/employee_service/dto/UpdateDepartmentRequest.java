package com.nexuslabs.employee_service.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateDepartmentRequest
{
    @NotBlank(message = "Department name is required")
    private String name;

    public UpdateDepartmentRequest(){}

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

}