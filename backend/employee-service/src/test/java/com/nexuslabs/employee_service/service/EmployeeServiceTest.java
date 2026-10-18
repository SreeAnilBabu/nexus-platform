package com.nexuslabs.employee_service.service;

import com.nexuslabs.employee_service.dto.CreateEmployeeRequest;
import com.nexuslabs.employee_service.dto.EmployeeResponse;
import com.nexuslabs.employee_service.dto.UpdateEmployeeRequest;
import com.nexuslabs.employee_service.entity.Employee;
import com.nexuslabs.employee_service.exception.DuplicateEmployeeEmailException;
import com.nexuslabs.employee_service.exception.EmployeeNotFoundException;
import com.nexuslabs.employee_service.repository.DepartmentRepository;
import com.nexuslabs.employee_service.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.nexuslabs.employee_service.entity.Department;
import com.nexuslabs.employee_service.dto.PageResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.nexuslabs.employee_service.exception.DepartmentNotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.Optional;
import static org.mockito.Mockito.*;
import com.nexuslabs.employee_service.entity.EmploymentStatus;
import com.nexuslabs.employee_service.exception.InvalidEmploymentStatusTransitionException;

class EmployeeServiceTest {

    private EmployeeService employeeService;

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;



    @BeforeEach
    void setUp() {
        employeeRepository =
                mock(EmployeeRepository.class);
        departmentRepository = mock(DepartmentRepository.class);

        employeeService =
                new EmployeeService(
                        employeeRepository,
                        departmentRepository
                );
    }

    @Test
    void shouldGetEmployeeById() {

        Employee employee = new Employee(
                "John",
                "Doe",
                "john@example.com"
        );

        Mockito.when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        EmployeeResponse response =
                employeeService.getEmployeeById(1L);

        assertEquals(
                "John",
                response.getFirstName()
        );

        assertEquals(
                "Doe",
                response.getLastName()
        );

        assertEquals(
                "john@example.com",
                response.getEmail()
        );

        Mockito.verify(employeeRepository)
                .findById(1L);
    }

    @Test
    void shouldThrowEmployeeNotFoundExceptionWhenEmployeeDoesNotExist() {

        Mockito.when(
                employeeRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                EmployeeNotFoundException.class,
                () ->
                        employeeService
                                .getEmployeeById(999L)
        );
    }

    @Test
    void shouldCreateEmployee() {

        CreateEmployeeRequest request =
                new CreateEmployeeRequest();

        request.setFirstName("Rahul");
        request.setLastName("Kumar");
        request.setEmail("rahul@example.com");

        Mockito.when(
                employeeRepository.existsByEmail(
                        "rahul@example.com"
                )
        ).thenReturn(false);

        Employee savedEmployee = new Employee(
                "Rahul",
                "Kumar",
                "rahul@example.com"
        );

        Mockito.when(
                employeeRepository.save(
                        Mockito.any(Employee.class)
                )
        ).thenReturn(savedEmployee);

        EmployeeResponse response =
                employeeService.createEmployee(request);

        assertEquals(
                "Rahul",
                response.getFirstName()
        );

        assertEquals(
                "Kumar",
                response.getLastName()
        );

        assertEquals(
                "rahul@example.com",
                response.getEmail()
        );

        assertEquals(
                EmploymentStatus.ACTIVE,
                response.getEmploymentStatus()
        );

        Mockito.verify(employeeRepository)
                .existsByEmail(
                        "rahul@example.com"
                );

        Mockito.verify(employeeRepository)
                .save(
                        Mockito.any(Employee.class)
                );
    }

    @Test
    void shouldThrowDuplicateEmployeeEmailExceptionWhenCreatingDuplicateEmail() {

        CreateEmployeeRequest request =
                new CreateEmployeeRequest();

        request.setFirstName("Rahul");
        request.setLastName("Kumar");
        request.setEmail("rahul@example.com");

        Mockito.when(
                employeeRepository.existsByEmail(
                        "rahul@example.com"
                )
        ).thenReturn(true);

        assertThrows(
                DuplicateEmployeeEmailException.class,
                () ->
                        employeeService
                                .createEmployee(request)
        );

        Mockito.verify(
                employeeRepository,
                Mockito.never()
        ).save(
                Mockito.any(Employee.class)
        );
    }

    @Test
    void shouldUpdateEmployee() {

        Employee existingEmployee = new Employee(
                "Sony",
                "Kuttan",
                "sony@example.com"
        );

        UpdateEmployeeRequest request =
                new UpdateEmployeeRequest();

        request.setFirstName("Sony");
        request.setLastName("Updated");
        request.setEmail(
                "sony.updated@example.com"
        );

        Mockito.when(
                employeeRepository.findById(7L)
        ).thenReturn(
                Optional.of(existingEmployee)
        );

        Mockito.when(
                employeeRepository.existsByEmail(
                        "sony.updated@example.com"
                )
        ).thenReturn(false);

        Mockito.when(
                employeeRepository.save(existingEmployee)
        ).thenReturn(existingEmployee);

        EmployeeResponse response =
                employeeService.updateEmployee(
                        7L,
                        request
                );

        assertEquals(
                "Sony",
                response.getFirstName()
        );

        assertEquals(
                "Updated",
                response.getLastName()
        );

        assertEquals(
                "sony.updated@example.com",
                response.getEmail()
        );

        Mockito.verify(employeeRepository)
                .save(existingEmployee);
    }

    @Test
    void shouldThrowDuplicateEmployeeEmailExceptionWhenUpdatingToExistingEmail() {

        Employee existingEmployee = new Employee(
                "Sony",
                "Kuttan",
                "sony@example.com"
        );

        UpdateEmployeeRequest request =
                new UpdateEmployeeRequest();

        request.setFirstName("Sony");
        request.setLastName("Updated");
        request.setEmail(
                "john@example.com"
        );

        Mockito.when(
                employeeRepository.findById(7L)
        ).thenReturn(
                Optional.of(existingEmployee)
        );

        Mockito.when(
                employeeRepository.existsByEmail(
                        "john@example.com"
                )
        ).thenReturn(true);

        assertThrows(
                DuplicateEmployeeEmailException.class,
                () ->
                        employeeService.updateEmployee(
                                7L,
                                request
                        )
        );

        Mockito.verify(
                employeeRepository,
                Mockito.never()
        ).save(
                Mockito.any(Employee.class)
        );
    }

    @Test
    void shouldGetEmployeesByDepartment() {

        Employee employee = new Employee(
                "Alex",
                "Thomas",
                "alex.department@test.com"
        );

        Department department = new Department("Engineering");
        employee.setDepartment(department);

        Page<Employee> employeePage =
                new PageImpl<>(List.of(employee));

        when(departmentRepository.existsById(1L))
                .thenReturn(true);

        when(employeeRepository.findByDepartmentId(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(employeePage);

        PageResponse<EmployeeResponse> result =
                employeeService.getAllEmployees(
                        0,
                        5,
                        "id",
                        "asc",
                        1L
                );

        assertEquals(1, result.getContent().size());

        verify(departmentRepository)
                .existsById(1L);

        verify(employeeRepository)
                .findByDepartmentId(
                        eq(1L),
                        any(Pageable.class)
                );
    }

    @Test
    void shouldThrowExceptionWhenFilteringByNonExistingDepartment() {

        when(departmentRepository.existsById(999L))
                .thenReturn(false);

        assertThrows(
                DepartmentNotFoundException.class,
                () -> employeeService.getAllEmployees(
                        0,
                        5,
                        "id",
                        "asc",
                        999L
                )
        );

        verify(departmentRepository)
                .existsById(999L);

        verify(employeeRepository, never())
                .findByDepartmentId(
                        anyLong(),
                        any(Pageable.class)
                );
    }

    @Test
    void shouldResignActiveEmployee() {
        Employee employee = new Employee(
                "John", "Doe", "john@example.com"
        );

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.save(employee))
                .thenReturn(employee);

        EmployeeResponse response =
                employeeService.updateEmploymentStatus(
                        1L, EmploymentStatus.RESIGNED
                );

        assertEquals(
                EmploymentStatus.RESIGNED,
                response.getEmploymentStatus()
        );

        verify(employeeRepository).save(employee);
    }

    @Test
    void shouldTerminateActiveEmployee() {
        Employee employee = new Employee(
                "Jane", "Doe", "jane@example.com"
        );

        when(employeeRepository.findById(2L))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.save(employee))
                .thenReturn(employee);

        EmployeeResponse response =
                employeeService.updateEmploymentStatus(
                        2L, EmploymentStatus.TERMINATED
                );

        assertEquals(
                EmploymentStatus.TERMINATED,
                response.getEmploymentStatus()
        );
    }

    @Test
    void shouldRejectResignedEmployeeReturningToActive() {
        Employee employee = new Employee(
                "John", "Doe", "john@example.com"
        );
        employee.setEmploymentStatus(EmploymentStatus.RESIGNED);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                InvalidEmploymentStatusTransitionException.class,
                () -> employeeService.updateEmploymentStatus(
                        1L, EmploymentStatus.ACTIVE
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldRejectSameStatusTransition() {
        Employee employee = new Employee(
                "John", "Doe", "john@example.com"
        );

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                InvalidEmploymentStatusTransitionException.class,
                () -> employeeService.updateEmploymentStatus(
                        1L, EmploymentStatus.ACTIVE
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldRejectTerminatedEmployeeReturningToActive() {

        Employee employee = new Employee(
                "John", "Doe", "john@example.com"
        );

        employee.setEmploymentStatus(EmploymentStatus.TERMINATED);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                InvalidEmploymentStatusTransitionException.class,
                () -> employeeService.updateEmploymentStatus(
                        1L,
                        EmploymentStatus.ACTIVE
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldRejectResignedEmployeeBeingTerminated() {

        Employee employee = new Employee(
                "John", "Doe", "john@example.com"
        );

        employee.setEmploymentStatus(EmploymentStatus.RESIGNED);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                InvalidEmploymentStatusTransitionException.class,
                () -> employeeService.updateEmploymentStatus(
                        1L,
                        EmploymentStatus.TERMINATED
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldRejectTerminatedEmployeeResigning() {

        Employee employee = new Employee(
                "John", "Doe", "john@example.com"
        );

        employee.setEmploymentStatus(EmploymentStatus.TERMINATED);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        assertThrows(
                InvalidEmploymentStatusTransitionException.class,
                () -> employeeService.updateEmploymentStatus(
                        1L,
                        EmploymentStatus.RESIGNED
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingMissingEmployeeStatus() {

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                EmployeeNotFoundException.class,
                () -> employeeService.updateEmploymentStatus(
                        999L,
                        EmploymentStatus.RESIGNED
                )
        );

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void shouldPreserveEmploymentStatusDuringNormalUpdate() {

        Employee employee = new Employee(
                "John", "Doe", "john@example.com"
        );
        employee.setEmploymentStatus(EmploymentStatus.RESIGNED);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setFirstName("John");
        request.setLastName("Updated");
        request.setEmail("john@example.com");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.save(employee))
                .thenReturn(employee);

        EmployeeResponse response =
                employeeService.updateEmployee(1L, request);

        assertEquals(
                EmploymentStatus.RESIGNED,
                response.getEmploymentStatus()
        );
    }

}