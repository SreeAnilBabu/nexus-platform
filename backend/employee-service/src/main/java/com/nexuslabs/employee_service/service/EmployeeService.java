package com.nexuslabs.employee_service.service;

import com.nexuslabs.employee_service.dto.CreateEmployeeRequest;
import com.nexuslabs.employee_service.dto.EmployeeResponse;
import com.nexuslabs.employee_service.dto.UpdateEmployeeRequest;
import com.nexuslabs.employee_service.entity.Employee;
import com.nexuslabs.employee_service.exception.DuplicateEmployeeEmailException;
import com.nexuslabs.employee_service.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import com.nexuslabs.employee_service.exception.EmployeeNotFoundException;

import java.util.List;

@Service
public class EmployeeService{

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public EmployeeResponse createEmployee(
            CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmployeeEmailException(request.getEmail());
        }
        Employee employee = new Employee(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail()
        );
        Employee savedEmployee =
                employeeRepository.save(employee);

        return mapToResponse(savedEmployee);
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(employee -> mapToResponse(employee))
                .toList();
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(id));
        return mapToResponse(employee);
   }

   public EmployeeResponse updateEmployee(
           long id,
           UpdateEmployeeRequest request) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        if(!employee.getEmail().equals(request.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmployeeEmailException(request.getEmail());
        }
       employee.setFirstName(request.getFirstName());
       employee.setLastName(request.getLastName());
       employee.setEmail(request.getEmail());

        Employee updatedEmployee = employeeRepository.save(employee);
        return mapToResponse(updatedEmployee);
   }

   public void deleteEmployee(long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        employeeRepository.delete(employee);
   }

    private EmployeeResponse mapToResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail()
        );
    }
}