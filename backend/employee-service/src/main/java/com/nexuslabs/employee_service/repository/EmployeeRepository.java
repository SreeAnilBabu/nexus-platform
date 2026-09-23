package com.nexuslabs.employee_service.repository;
import com.nexuslabs.employee_service.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeRepository extends JpaRepository<Employee, Long>{
    boolean existsByEmail(String email);
    Page<Employee> findByFirstNameContainingIgnoreCase(String firstName,  Pageable pageable);
}
