package com.nexuslabs.employee_service.controller;

import com.nexuslabs.employee_service.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nexuslabs.employee_service.dto.EmployeeResponse;
import com.nexuslabs.employee_service.dto.PageResponse;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                "anil.updated@example.com"
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
                "asc"
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
                        "sony.updated@example.com"
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
}