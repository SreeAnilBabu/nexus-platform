
package com.nexuslabs.employee_service.dto;

import com.nexuslabs.employee_service.entity.EmploymentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateEmploymentStatusRequest {

    @NotNull(message = "Employment status is required")
    private EmploymentStatus status;

    public UpdateEmploymentStatusRequest() {
    }

    public EmploymentStatus getStatus() {
        return status;
    }

    public void setStatus(EmploymentStatus status) {
        this.status = status;
    }
}
