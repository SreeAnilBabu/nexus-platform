
package com.nexuslabs.employee_service.service;

import com.nexuslabs.employee_service.entity.Department;
import com.nexuslabs.employee_service.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DepartmentTransactionTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldRollbackDepartmentCreationWhenExceptionOccurs() {

        String departmentName = "Temporary Rollback Department";

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> {

            transactionTemplate.executeWithoutResult(status -> {

                Department department =
                        new Department(departmentName);

                departmentRepository.saveAndFlush(department);

                assertTrue(
                        departmentRepository.existsByNameIgnoreCase(
                                departmentName)
                );

                throw new IllegalStateException(
                        "Simulated failure after department creation"
                );
            });
        });

        assertFalse(
                departmentRepository.existsByNameIgnoreCase(
                        departmentName)
        );
    }
}
