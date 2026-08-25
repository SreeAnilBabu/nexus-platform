package com.nexuslabs.employee_service.repository;

import com.nexuslabs.employee_service.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long>{
    boolean existsByEmail(String email);
}