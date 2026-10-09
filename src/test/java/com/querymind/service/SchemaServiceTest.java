package com.querymind.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaServiceTest {

    private final SchemaService schemaService = new SchemaService();

    @Test
    void shouldAllowConfiguredTables() {
        assertThat(schemaService.isTableAllowed("departments")).isTrue();
        assertThat(schemaService.isTableAllowed("DEPARTMENTS")).isTrue();
        assertThat(schemaService.isTableAllowed("employees")).isTrue();
        assertThat(schemaService.isTableAllowed("Employees")).isTrue();
    }

    @Test
    void shouldRejectDisallowedTables() {
        assertThat(schemaService.isTableAllowed("users")).isFalse();
        assertThat(schemaService.isTableAllowed("pg_shadow")).isFalse();
        assertThat(schemaService.isTableAllowed("information_schema")).isFalse();
        assertThat(schemaService.isTableAllowed(null)).isFalse();
        assertThat(schemaService.isTableAllowed("")).isFalse();
    }

    @Test
    void shouldProvideSchemaDescriptionContainingKeyColumns() {
        String schema = schemaService.getSchemaDescription();

        assertThat(schema).contains("departments", "department_id", "department_name");
        assertThat(schema).contains("employees", "employee_id", "employee_name", "salary", "department_id");
    }
}
