package com.querymind.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DevQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnTopEarnersByDefault() throws Exception {
        mockMvc.perform(get("/api/dev/sample-query")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns", hasSize(2)))
                .andExpect(jsonPath("$.columns[0]").value("employee_name"))
                .andExpect(jsonPath("$.columns[1]").value("salary"))
                .andExpect(jsonPath("$.rowCount").value(5))
                .andExpect(jsonPath("$.rows", hasSize(5)))
                // The highest paid employee in seed data is Ian Malcolm with 125000.00
                .andExpect(jsonPath("$.rows[0][0]").value("Ian Malcolm"));
    }

    @Test
    void shouldReturnDepartmentSalariesAggregation() throws Exception {
        mockMvc.perform(get("/api/dev/sample-query")
                        .param("type", "department-salaries")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0]").value("department_name"))
                .andExpect(jsonPath("$.columns[1]").value("employee_count"))
                .andExpect(jsonPath("$.columns[2]").value("average_salary"))
                .andExpect(jsonPath("$.rowCount").value(5));
    }

    @Test
    void shouldReturnBadRequestForUnknownQueryType() throws Exception {
        mockMvc.perform(get("/api/dev/sample-query")
                        .param("type", "unsupported-query")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
