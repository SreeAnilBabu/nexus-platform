package com.nexuslabs.employee_service.service;

import com.nexuslabs.employee_service.entity.Department;
import com.nexuslabs.employee_service.exception.DepartmentHasEmployeesException;
import com.nexuslabs.employee_service.repository.DepartmentRepository;
import com.nexuslabs.employee_service.repository.EmployeeRepository;
import com.nexuslabs.employee_service.exception.DepartmentNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DepartmentServiceTest {

    private DepartmentRepository departmentRepository;
    private EmployeeRepository employeeRepository;
    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        employeeRepository = mock(EmployeeRepository.class);

        departmentService = new DepartmentService(
                departmentRepository,
                employeeRepository
        );
    }

    @Test
    void shouldRejectDeletingDepartmentWithEmployees() {

        Department engineering = new Department("Engineering");

        when(departmentRepository.findById(1L))
                .thenReturn(Optional.of(engineering));

        when(employeeRepository.existsByDepartmentId(1L))
                .thenReturn(true);

        assertThrows(
                DepartmentHasEmployeesException.class,
                () -> departmentService.deleteDepartment(1L)
        );

        verify(departmentRepository).findById(1L);
        verify(employeeRepository).existsByDepartmentId(1L);

        verify(departmentRepository, never())
                .delete(any(Department.class));
    }

    @Test
    void shouldDeleteDepartmentWithoutEmployees() {

        Department finance = new Department("Finance");

        when(departmentRepository.findById(3L))
                .thenReturn(Optional.of(finance));

        when(employeeRepository.existsByDepartmentId(3L))
                .thenReturn(false);

        departmentService.deleteDepartment(3L);

        verify(departmentRepository).findById(3L);
        verify(employeeRepository).existsByDepartmentId(3L);
        verify(departmentRepository).delete(finance);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingDepartment() {

        when(departmentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.deleteDepartment(999L)
        );

        verify(departmentRepository).findById(999L);

        verifyNoInteractions(employeeRepository);

        verify(departmentRepository, never())
                .delete(any(Department.class));
    }
}