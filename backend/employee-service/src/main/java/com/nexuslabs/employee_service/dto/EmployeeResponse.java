package com.nexuslabs.employee_service.dto;
import java.time.LocalDate;
import com.nexuslabs.employee_service.entity.EmploymentStatus;

public class EmployeeResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;

    private Long departmentId;

    private LocalDate joiningDate;
    private EmploymentStatus employmentStatus;

    public EmployeeResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            Long departmentId,
            LocalDate joiningDate,
            EmploymentStatus employmentStatus) {

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.departmentId = departmentId;
        this.joiningDate = joiningDate;
        this.employmentStatus = employmentStatus;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }
}