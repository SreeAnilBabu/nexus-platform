package com.nexuslabs.employee_service.controller;

import com.nexuslabs.employee_service.entity.EmploymentStatus;
import com.nexuslabs.employee_service.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nexuslabs.employee_service.dto.EmployeeResponse;
import com.nexuslabs.employee_service.dto.PageResponse;
import java.util.List;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@WebMvcTest(EmployeeController.class)
public class EmployeeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void shouldReturnEmployeesWithPagnination() throws Exception {

        EmployeeResponse employee = new EmployeeResponse(
                1L,
                "Anil",
                "Updated",
                "anil.updated@example.com",
                null,
                null,
                EmploymentStatus.ACTIVE
        );

        PageResponse<EmployeeResponse> pageResponse = new PageResponse<>(
                List.of(employee),
                0,
                5,
                1,
                1,
                true,
                true
        );

        when(employeeService.getAllEmployees(
                0,
                5,
                "id",
                "asc",
                null
        )).thenReturn(pageResponse);

        mockMvc.perform(
                get("/api/employees")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "id")
                        .param("direction", "asc")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(true))
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].firstName").value("Anil"));
    }

    @Test
    void shouldSearchEmployeesByFirstName() throws Exception {

        EmployeeResponse employee =
                new EmployeeResponse(
                        7L,
                        "Sony",
                        "Updated",
                        "sony.updated@example.com",
                        null,
                        null,
                        EmploymentStatus.ACTIVE
                );

        PageResponse<EmployeeResponse> pageResponse =
                new PageResponse<>(
                        List.of(employee),
                        0,
                        1,
                        2,
                        2,
                        true,
                        false
                );

        when(employeeService.searchEmployeesByFirstName(
                "son",
                0,
                1,
                "id",
                "asc"
        )).thenReturn(pageResponse);

        mockMvc.perform(
                        get("/api/employees/search")
                                .param("name", "son")
                                .param("page", "0")
                                .param("size", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].firstName").value("Sony"));
    }

    @Test
    void shouldReturnBadRequestWhenSearchNameIsMissing() throws Exception {

        mockMvc.perform(
                        get("/api/employees/search")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetEmployeesByDepartment() throws Exception {

        PageResponse<EmployeeResponse> response =
                new PageResponse<>(
                        List.of(
                                new EmployeeResponse(
                                        12L,
                                        "Alex",
                                        "Thomas",
                                        "alex.thomas@example.com",
                                        1L,
                                        null,
                                        EmploymentStatus.ACTIVE
                                )
                        ),
                        0,
                        5,
                        1,
                        1,
                        true,
                        true
                );

        when(employeeService.getAllEmployees(
                0,
                5,
                "id",
                "asc",
                1L
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/employees")
                                .param("departmentId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(12))
                .andExpect(jsonPath("$.content[0].firstName").value("Alex"))
                .andExpect(jsonPath("$.content[0].departmentId").value(1));

        verify(employeeService).getAllEmployees(
                0,
                5,
                "id",
                "asc",
                1L
        );
    }

    @Test
    void shouldUpdateEmployeeEmploymentStatus() throws Exception {

        EmployeeResponse response = new EmployeeResponse(
                1L,
                "John",
                "Doe",
                "john@example.com",
                null,
                null,
                EmploymentStatus.RESIGNED
        );

        when(employeeService.updateEmploymentStatus(
                1L,
                EmploymentStatus.RESIGNED
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/employees/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "status": "RESIGNED"
                            }
                            """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employmentStatus").value("RESIGNED"));

        verify(employeeService).updateEmploymentStatus(
                1L,
                EmploymentStatus.RESIGNED
        );
    }

    @Test
    void shouldRejectPhysicalEmployeeDeletion() throws Exception {

        mockMvc.perform(
                        delete("/api/employees/1")
                )
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(employeeService);
    }

    @Test
    void shouldRejectMissingEmploymentStatus() throws Exception {

        mockMvc.perform(
                        patch("/api/employees/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {}
                            """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(employeeService);
    }

    @Test
    void shouldRejectUnknownEmploymentStatus() throws Exception {

        mockMvc.perform(
                        patch("/api/employees/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "status": "ON_LEAVE"
                            }
                            """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(employeeService);
    }

    @Test
    void shouldRejectEmployeeDeletion() throws Exception {

        mockMvc.perform(delete("/api/employees/7"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error")
                        .value("Method Not Allowed"))
                .andExpect(jsonPath("$.message")
                        .value("Physical deletion of employee records is not permitted"));

        verifyNoInteractions(employeeService);
    }

}