package com.querymind.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Service that supplies only approved schema metadata to the AI model.
 * Restricts schema exposure to allowed tables and columns, preventing leakage
 * of system catalogs, internal metadata, or sensitive tables.
 */
@Service
public class SchemaService {

    private static final Set<String> ALLOWED_TABLES = Set.of("departments", "employees");

    private static final String SCHEMA_PROMPT = """
        Database: PostgreSQL
        Allowed Schema:

        Table: departments
        Columns:
          - department_id (BIGINT, PRIMARY KEY)
          - department_name (VARCHAR(100), NOT NULL)

        Table: employees
        Columns:
          - employee_id (BIGINT, PRIMARY KEY)
          - employee_name (VARCHAR(100), NOT NULL)
          - salary (NUMERIC(12, 2))
          - department_id (BIGINT, FOREIGN KEY REFERENCES departments(department_id))
        """;

    /**
     * Returns the formatted schema description for inclusion in the AI prompt.
     */
    public String getSchemaDescription() {
        return SCHEMA_PROMPT;
    }

    /**
     * Returns the unmodifiable set of approved table names.
     */
    public Set<String> getAllowedTables() {
        return ALLOWED_TABLES;
    }

    /**
     * Checks if a table name is in the approved allowlist.
     */
    public boolean isTableAllowed(String tableName) {
        if (tableName == null) {
            return false;
        }
        return ALLOWED_TABLES.contains(tableName.toLowerCase());
    }
}
