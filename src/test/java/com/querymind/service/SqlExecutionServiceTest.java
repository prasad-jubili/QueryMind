package com.querymind.service;

import com.querymind.config.ExecutionProperties;
import com.querymind.dto.QueryResult;
import com.querymind.exception.QueryExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SqlExecutionServiceTest {

    @Autowired
    private SqlExecutionService sqlExecutionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExecuteSelectQueryAndMapColumnsAndRows() {
        String sql = "SELECT employee_id, employee_name, salary FROM employees WHERE employee_id = 1";

        QueryResult result = sqlExecutionService.executeQuery(sql);

        assertThat(result.columns()).containsExactly("employee_id", "employee_name", "salary");
        assertThat(result.rowCount()).isEqualTo(1);
        assertThat(result.rows()).hasSize(1);

        List<Object> firstRow = result.rows().get(0);
        assertThat(firstRow.get(0)).isEqualTo(1L);
        assertThat(firstRow.get(1)).isEqualTo("Alice Smith");
        assertThat(((Number) firstRow.get(2)).doubleValue()).isEqualTo(95000.00);
    }

    @Test
    void shouldRespectColumnAliases() {
        String sql = "SELECT employee_name AS full_name, salary AS annual_pay FROM employees WHERE employee_id = 1";

        QueryResult result = sqlExecutionService.executeQuery(sql);

        assertThat(result.columns()).containsExactly("full_name", "annual_pay");
        assertThat(result.rowCount()).isEqualTo(1);
    }

    @Test
    void shouldCapResultsAtMaxRows() {
        ExecutionProperties customProperties = new ExecutionProperties();
        customProperties.setMaxRows(3);
        customProperties.setQueryTimeoutSeconds(5);

        SqlExecutionService customService = new SqlExecutionService(jdbcTemplate, customProperties);

        QueryResult result = customService.executeQuery("SELECT employee_name FROM employees");

        // There are 11 employees, but maxRows is 3
        assertThat(result.rowCount()).isEqualTo(3);
        assertThat(result.rows()).hasSize(3);
    }

    @Test
    void shouldThrowQueryExecutionExceptionOnInvalidSql() {
        String invalidSql = "SELECT * FROM nonexistent_table_xyz";

        assertThatThrownBy(() -> sqlExecutionService.executeQuery(invalidSql))
                .isInstanceOf(QueryExecutionException.class)
                .hasMessageContaining("An error occurred while executing the database query.")
                .satisfies(ex -> {
                    QueryExecutionException qee = (QueryExecutionException) ex;
                    assertThat(qee.getErrorCode()).isEqualTo("QUERY_EXECUTION_FAILED");
                });
    }
}
