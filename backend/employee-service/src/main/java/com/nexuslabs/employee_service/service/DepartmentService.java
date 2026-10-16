package com.nexuslabs.employee_service.service;

import com.nexuslabs.employee_service.dto.CreateDepartmentRequest;
import com.nexuslabs.employee_service.dto.DepartmentResponse;
import com.nexuslabs.employee_service.dto.UpdateDepartmentRequest;
import com.nexuslabs.employee_service.entity.Department;
import com.nexuslabs.employee_service.exception.DepartmentNotFoundException;
import com.nexuslabs.employee_service.exception.DuplicateDepartmentNameException;
import com.nexuslabs.employee_service.repository.DepartmentRepository;
import com.nexuslabs.employee_service.repository.EmployeeRepository;
import org.springframework.transaction.annotation.Transactional;
import com.nexuslabs.employee_service.exception.DepartmentHasEmployeesException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@Service
public class DepartmentService{

    private static final Logger logger = LoggerFactory.getLogger(DepartmentService.class);
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository) {

        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request){

        String departmentName = request.getName().trim();
        if (departmentRepository.existsByNameIgnoreCase(departmentName)){
            throw new DuplicateDepartmentNameException(departmentName);
        }
        Department department = new Department(departmentName);
        Department savedDepartment = departmentRepository.save(department);

        logger.info("Department created successfully with id={}",savedDepartment.getId());

        return mapToResponse(savedDepartment);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(long id){
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
        return mapToResponse(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {

        return departmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse updateDepartment(
            Long id,
            UpdateDepartmentRequest request) {

        String departmentName = request.getName().trim();

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(
                departmentName, id)) {
            throw new DuplicateDepartmentNameException(departmentName);
        }

        department.setName(departmentName);

        Department updatedDepartment =
                departmentRepository.save(department);

        logger.info(
                "Department updated successfully with id={}",
                updatedDepartment.getId()
        );

        return mapToResponse(updatedDepartment);
    }

    private DepartmentResponse mapToResponse(Department department){
        return new DepartmentResponse(
                department.getId(),
                department.getName()
        );
    }

    @Transactional
    public void deleteDepartment(Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        if (employeeRepository.existsByDepartmentId(id)) {
            throw new DepartmentHasEmployeesException(id);
        }

        departmentRepository.delete(department);

        logger.info("Department deletion requested for id={}", id);
    }

}