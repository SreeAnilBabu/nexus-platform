package com.nexuslabs.employee_service.repository;

import com.nexuslabs.employee_service.entity.Employee;
import com.nexuslabs.employee_service.entity.EmploymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.nexuslabs.employee_service.entity.Department;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void shouldFindEmployeesByFirstNameIgnoringCase() {

        Employee employee1 =
                new Employee(
                        "Sony",
                        "One",
                        "sony.one@example.com"
                );

        Employee employee2 =
                new Employee(
                        "Sony",
                        "Two",
                        "sony.two@example.com"
                );

        Employee employee3 =
                new Employee(
                        "Rahul",
                        "Kumar",
                        "rahul.test@example.com"
                );
        assertEquals(
                EmploymentStatus.ACTIVE,
                employee1.getEmploymentStatus()
        );
        employeeRepository.save(employee1);
        employeeRepository.save(employee2);
        employeeRepository.save(employee3);

        Pageable pageable =
                PageRequest.of(
                        0,
                        10,
                        Sort.by("id").ascending()
                );

        Page<Employee> result =
                employeeRepository
                        .findByFirstNameContainingIgnoreCase(
                                "SON",
                                pageable
                        );

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void shouldPaginateEmployeeSearchResults() {

        // create and save 3 employees whose firstName contains "Test"
        Employee employee1 =
                new Employee(
                        "Test",
                        "One",
                        "Test.one@example.com"
                );

        Employee employee2 =
                new Employee(
                        "Test",
                        "Two",
                        "Test.two@example.com"
                );

        Employee employee3 =
                new Employee(
                        "Test",
                        "Three",
                        "test.three@example.com"
                );

        employeeRepository.save(employee1);
        employeeRepository.save(employee2);
        employeeRepository.save(employee3);

        Pageable pageable =
                PageRequest.of(
                        0,
                        2,
                        Sort.by("id").ascending()
                );

        Page<Employee> result =
                employeeRepository
                        .findByFirstNameContainingIgnoreCase(
                                "Test",
                                pageable
                        );

        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalPages());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());
    }

    @Test
    void shouldReturnEmptyPageWhenNoEmployeeMatches() {

        Pageable pageable =
                PageRequest.of(0, 5);

        Page<Employee> result =
                employeeRepository
                        .findByFirstNameContainingIgnoreCase(
                                "DoesNotExist",
                                pageable
                        );

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldFindEmployeesByDepartmentId() {

        Department engineering =
                departmentRepository.save(
                        new Department("Engineering")
                );

        Employee employee = new Employee(
                "Alex",
                "Thomas",
                "alex.relationship@test.com"
        );

        employee.setDepartment(engineering);
        employeeRepository.save(employee);

        Pageable pageable =
                PageRequest.of(0, 5);

        Page<Employee> result =
                employeeRepository.findByDepartmentId(
                        engineering.getId(),
                        pageable
                );

        assertEquals(1, result.getTotalElements());
        assertEquals(
                engineering.getId(),
                result.getContent()
                        .get(0)
                        .getDepartment()
                        .getId()
        );
    }
}