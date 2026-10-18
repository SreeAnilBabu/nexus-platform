package com.nexuslabs.employee_service.service;

import com.nexuslabs.employee_service.dto.CreateEmployeeRequest;
import com.nexuslabs.employee_service.dto.EmployeeResponse;
import com.nexuslabs.employee_service.dto.PageResponse;
import com.nexuslabs.employee_service.dto.UpdateEmployeeRequest;
import com.nexuslabs.employee_service.entity.Employee;
import com.nexuslabs.employee_service.exception.DuplicateEmployeeEmailException;
import com.nexuslabs.employee_service.repository.DepartmentRepository;
import com.nexuslabs.employee_service.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import com.nexuslabs.employee_service.exception.EmployeeNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.nexuslabs.employee_service.exception.InvalidRequestParameterException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nexuslabs.employee_service.entity.Department;
import com.nexuslabs.employee_service.exception.DepartmentNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import com.nexuslabs.employee_service.entity.EmploymentStatus;
import com.nexuslabs.employee_service.exception.InvalidEmploymentStatusTransitionException;

@Service
public class EmployeeService{

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public EmployeeResponse createEmployee(
            CreateEmployeeRequest request) {

        if (employeeRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmployeeEmailException(request.getEmail());
        }
        Department department =
                resolveDepartment(request.getDepartmentId());
        Employee employee = new Employee(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail()
        );
        employee.setDepartment(department);
        employee.setJoiningDate(request.getJoiningDate());
        Employee savedEmployee =
                employeeRepository.save(employee);

        logger.info(
                "Employee created successfully with id={}",
                savedEmployee.getId()
        );

        return mapToResponse(savedEmployee);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getAllEmployees(
            int page,
            int size,
            String sortBy,
            String direction,
            Long departmentId) {

        validatePaginationAndSorting(page, size, sortBy, direction);

        logger.debug(
                "Fetching employees: page={}, size={}, sortBy={}, direction={}",
                page, size, sortBy, direction
        );

        Sort sort = direction.equalsIgnoreCase("desc")
                ?  Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size,sort);

        Page<Employee> employeePage;

        if(departmentId == null){
            employeePage = employeeRepository.findAll(pageable);
        }else{
            if(!departmentRepository.existsById(departmentId)){
                throw new DepartmentNotFoundException(departmentId);
            }
            employeePage = employeeRepository.findByDepartmentId(
                    departmentId, pageable
            );
        }
        //= employeeRepository.findAll(pageable);

        return mapToPageResponse(employeePage);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(id));
        return mapToResponse(employee);
   }

   @Transactional
   public EmployeeResponse updateEmployee(
           long id,
           UpdateEmployeeRequest request) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        if(!employee.getEmail().equals(request.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())){
            throw new DuplicateEmployeeEmailException(request.getEmail());
        }

       Department department =
               resolveDepartment(request.getDepartmentId());

       employee.setFirstName(request.getFirstName());
       employee.setLastName(request.getLastName());
       employee.setEmail(request.getEmail());
       employee.setDepartment(department);
       employee.setJoiningDate(request.getJoiningDate());

        Employee updatedEmployee = employeeRepository.save(employee);
        return mapToResponse(updatedEmployee);
   }

    @Transactional
    public EmployeeResponse updateEmploymentStatus(
            Long id,
            EmploymentStatus newStatus) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        EmploymentStatus currentStatus = employee.getEmploymentStatus();

        if (newStatus == null) {
            throw new InvalidRequestParameterException(
                    "Employment status is required"
            );
        }

        if (currentStatus == newStatus) {
            throw new InvalidEmploymentStatusTransitionException(
                    "Employee is already in status " + currentStatus
            );
        }

        if (currentStatus != EmploymentStatus.ACTIVE) {
            throw new InvalidEmploymentStatusTransitionException(
                    "Cannot change employment status from "
                            + currentStatus + " to " + newStatus
            );
        }

        if (newStatus != EmploymentStatus.RESIGNED
                && newStatus != EmploymentStatus.TERMINATED) {
            throw new InvalidEmploymentStatusTransitionException(
                    "Invalid transition from ACTIVE to " + newStatus
            );
        }

        employee.setEmploymentStatus(newStatus);

        Employee updatedEmployee = employeeRepository.save(employee);

        logger.info(
                "Employee lifecycle updated: id={}, previousStatus={}, newStatus={}",
                id, currentStatus, newStatus
        );

        return mapToResponse(updatedEmployee);
    }


    private EmployeeResponse mapToResponse(Employee employee) {

        Long departmentId = employee.getDepartment() != null
                ? employee.getDepartment().getId()
                : null;

        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                departmentId,
                employee.getJoiningDate(),
                employee.getEmploymentStatus()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> searchEmployeesByFirstName(
            String name,
            int page,
            int size,
            String sortBy,
            String direction){

        validatePaginationAndSorting(page,size, sortBy, direction);

        logger.debug(
                "Searching employees : name={}, page={}, size={}, sortby={}, direction={}",
                name, page, size, sortBy, direction
        );

        if(name == null || name.isBlank()){
            logger.warn("Blank employee search name requested");
            throw new InvalidRequestParameterException("Search name must not be blank");
        }

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size,sort);

        Page<Employee> employeePage =
                employeeRepository.findByFirstNameContainingIgnoreCase(
                        name,
                        pageable
                );
        return mapToPageResponse(employeePage);
    }

    private void validatePaginationAndSorting(int page, int size, String sortBy, String direction) {
        if(page<0){

            logger.warn("Invalid page number Requested: {}", page);
            throw new InvalidRequestParameterException("Page number must not be less than 0");
        }

        if(size < 1 || size > 100){
            logger.warn("Invalid size requested: {}", size);
            throw new InvalidRequestParameterException("Page size must be between 1 and 100");
        }

        if(!sortBy.equals("id")
                && !sortBy.equals("firstName")
                && !sortBy.equals("lastName")
                && !sortBy.equals("email")
        ){
            logger.warn("Invalid sort field requested: {}", sortBy);
            throw new InvalidRequestParameterException("Invalid sort field: "+sortBy);
        }

        if(!direction.equalsIgnoreCase("asc") &&  !direction.equalsIgnoreCase("desc")){
            logger.warn("Invalid sort direction requested: {}", direction);
            throw new InvalidRequestParameterException("Sort direction must be 'asc' or 'desc'");
        }
    }

    private PageResponse<EmployeeResponse> mapToPageResponse(
            Page<Employee> employeePage){

        List<EmployeeResponse> content =
                employeePage.getContent()
                        .stream()
                        .map(employee -> mapToResponse(employee))
                        .toList();

        return new PageResponse<>(
                content,
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements(),
                employeePage.getTotalPages(),
                employeePage.isFirst(),
                employeePage.isLast()
        );
    }

    private Department resolveDepartment(Long departmentId) {

        if (departmentId == null) {
            return null;
        }

        return departmentRepository.findById(departmentId)
                .orElseThrow(
                        () -> new DepartmentNotFoundException(departmentId)
                );
    }
}