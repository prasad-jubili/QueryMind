package com.querymind;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldVerifyPostgresConnectionAndSampleSchema() {
        // Verify database is reachable
        Integer testQuery = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(testQuery).isEqualTo(1);

        // Verify Flyway migrations created departments and employees
        Integer deptCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM departments", Integer.class);
        assertThat(deptCount).isEqualTo(5);

        Integer empCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employees", Integer.class);
        assertThat(empCount).isEqualTo(11);
    }

    @Test
    void shouldSuccessfullyJoinEmployeesAndDepartments() {
        String sql = """
            SELECT e.employee_name, d.department_name
            FROM employees e
            JOIN departments d ON e.department_id = d.department_id
            WHERE e.employee_id = 1
        """;

        var result = jdbcTemplate.queryForMap(sql);
        assertThat(result.get("employee_name")).isEqualTo("Alice Smith");
        assertThat(result.get("department_name")).isEqualTo("Engineering");
    }
}
