package com.nexuslabs.employee_service.service;

import com.nexuslabs.employee_service.dto.CreateEmployeeRequest;
import com.nexuslabs.employee_service.dto.EmployeeResponse;
import com.nexuslabs.employee_service.dto.UpdateEmployeeRequest;
import com.nexuslabs.employee_service.entity.Employee;
import com.nexuslabs.employee_service.exception.DuplicateEmployeeEmailException;
import com.nexuslabs.employee_service.exception.EmployeeNotFoundException;
import com.nexuslabs.employee_service.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeServiceTest {

    private EmployeeRepository employeeRepository;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeRepository =
                Mockito.mock(EmployeeRepository.class);

        employeeService =
                new EmployeeService(employeeRepository);
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
    void shouldDeleteEmployee() {

        Employee employee = new Employee(
                "Sony",
                "Updated",
                "sony.updated@example.com"
        );

        Mockito.when(
                employeeRepository.findById(7L)
        ).thenReturn(
                Optional.of(employee)
        );

        employeeService.deleteEmployee(7L);

        Mockito.verify(employeeRepository)
                .delete(employee);
    }
}