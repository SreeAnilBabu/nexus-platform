package com.nexuslabs.employee_service.exception;

public class DepartmentHasEmployeesException extends RuntimeException {

    public DepartmentHasEmployeesException(Long departmentId) {
        super(
                "Cannot delete department with id: "
                        + departmentId
                        + " because employees are assigned to it"
        );
    }
}