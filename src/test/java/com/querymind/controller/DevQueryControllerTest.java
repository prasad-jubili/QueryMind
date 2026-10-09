package com.querymind.controller;

import com.querymind.dto.QueryResult;
import com.querymind.service.SqlExecutionService;
import com.querymind.service.SqlGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevQueryControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SqlExecutionService stubExecutionService = new StubSqlExecutionService();
        SqlGenerationService stubGenerationService = new StubSqlGenerationService();
        DevQueryController controller = new DevQueryController(stubExecutionService, stubGenerationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnTopEarnersByDefault() throws Exception {
        mockMvc.perform(get("/api/dev/sample-query")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns", hasSize(2)))
                .andExpect(jsonPath("$.columns[0]").value("employee_name"))
                .andExpect(jsonPath("$.columns[1]").value("salary"))
                .andExpect(jsonPath("$.rowCount").value(1))
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
                .andExpect(jsonPath("$.rowCount").value(1));
    }

    @Test
    void shouldReturnBadRequestForUnknownQueryType() throws Exception {
        mockMvc.perform(get("/api/dev/sample-query")
                        .param("type", "unsupported-query")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGenerateSqlWithoutExecutingIt() throws Exception {
        mockMvc.perform(get("/api/dev/generate-sql")
                        .param("question", "Show the 5 employees with highest salaries")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("Show the 5 employees with highest salaries"))
                .andExpect(jsonPath("$.generatedSql")
                        .value("SELECT employee_name, salary FROM employees ORDER BY salary DESC LIMIT 5"));
    }

    private static class StubSqlExecutionService extends SqlExecutionService {
        @Override
        public QueryResult executeQuery(String sql) {
            if (sql.contains("ORDER BY salary DESC LIMIT 5")) {
                return new QueryResult(
                        List.of("employee_name", "salary"),
                        List.of(List.of("Ian Malcolm", 125000.00)),
                        1
                );
            }
            return new QueryResult(
                    List.of("department_name", "employee_count", "average_salary"),
                    List.of(List.of("Engineering", 3, 95666.67)),
                    1
            );
        }
    }

    private static class StubSqlGenerationService extends SqlGenerationService {
        @Override
        public String generateSql(String question) {
            return "SELECT employee_name, salary FROM employees ORDER BY salary DESC LIMIT 5";
        }
    }
}
