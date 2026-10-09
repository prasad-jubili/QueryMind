package com.querymind.dto;

import java.util.List;

/**
 * Structured query result containing column names, row data, and total returned row count.
 * Rows are represented as lists of cell values matching the order of {@code columns}.
 */
public record QueryResult(
        List<String> columns,
        List<List<Object>> rows,
        int rowCount
) {
    public QueryResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
