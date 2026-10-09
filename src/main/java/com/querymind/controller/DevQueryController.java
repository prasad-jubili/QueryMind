package com.querymind.controller;

import com.querymind.dto.QueryResult;
import com.querymind.service.SqlExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Developer-only controller for testing fixed, hard-coded read-only queries in Milestone 2.
 * Does NOT accept arbitrary SQL from clients.
 */
@RestController
@RequestMapping("/api/dev")
public class DevQueryController {

    private static final Map<String, String> FIXED_QUERIES = Map.of(
            "top-earners",
            "SELECT employee_name, salary FROM employees ORDER BY salary DESC LIMIT 5",

            "department-salaries",
            """
            SELECT d.department_name,
                   COUNT(e.employee_id) AS employee_count,
                   ROUND(AVG(e.salary), 2) AS average_salary
            FROM departments d
            LEFT JOIN employees e ON d.department_id = e.department_id
            GROUP BY d.department_name
            ORDER BY average_salary DESC NULLS LAST
            """,

            "all-employees",
            """
            SELECT e.employee_id, e.employee_name, e.salary, d.department_name
            FROM employees e
            LEFT JOIN departments d ON e.department_id = d.department_id
            ORDER BY e.employee_id ASC
            """
    );

    private final SqlExecutionService sqlExecutionService;

    public DevQueryController(SqlExecutionService sqlExecutionService) {
        this.sqlExecutionService = sqlExecutionService;
    }

    @GetMapping("/sample-query")
    public ResponseEntity<QueryResult> runSampleQuery(
            @RequestParam(defaultValue = "top-earners") String type) {

        String sql = FIXED_QUERIES.get(type);
        if (sql == null) {
            return ResponseEntity.badRequest().build();
        }

        QueryResult result = sqlExecutionService.executeQuery(sql);
        return ResponseEntity.ok(result);
    }
}
