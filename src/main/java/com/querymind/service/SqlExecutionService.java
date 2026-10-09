package com.querymind.service;

import com.querymind.config.ExecutionProperties;
import com.querymind.dto.QueryResult;
import com.querymind.exception.QueryExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class SqlExecutionService {

    private static final Logger log = LoggerFactory.getLogger(SqlExecutionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ExecutionProperties executionProperties;

    @Autowired
    public SqlExecutionService(JdbcTemplate jdbcTemplate, ExecutionProperties executionProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.executionProperties = executionProperties;
    }

    /**
     * Protected no-arg constructor for testing and stubbing.
     */
    protected SqlExecutionService() {
        this.jdbcTemplate = null;
        this.executionProperties = null;
    }

    /**
     * Executes a read-only SQL query with configured timeouts and row capping.
     * Maps results into structured column names and row lists.
     *
     * @param sql the SQL statement to execute
     * @return the structured {@link QueryResult}
     * @throws QueryExecutionException if execution fails or times out
     */
    public QueryResult executeQuery(String sql) {
        log.debug("Executing SQL query: {}", sql);
        int maxRows = executionProperties.getMaxRows();
        int timeoutSeconds = executionProperties.getQueryTimeoutSeconds();

        try {
            return jdbcTemplate.execute((Statement statement) -> {
                statement.setQueryTimeout(timeoutSeconds);
                statement.setMaxRows(maxRows);

                try (var rs = statement.executeQuery(sql)) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    List<String> columns = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(metaData.getColumnLabel(i));
                    }

                    List<List<Object>> rows = new ArrayList<>();
                    while (rs.next() && rows.size() < maxRows) {
                        List<Object> row = new ArrayList<>(columnCount);
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        rows.add(row);
                    }

                    return new QueryResult(columns, rows, rows.size());
                }
            });
        } catch (QueryTimeoutException ex) {
            log.error("Query timed out after {} seconds: {}", timeoutSeconds, sql, ex);
            throw new QueryExecutionException("QUERY_TIMEOUT",
                    "Query execution timed out after " + timeoutSeconds + " seconds.", ex);
        } catch (Exception ex) {
            log.error("Query execution failed: {}", sql, ex);
            throw new QueryExecutionException("QUERY_EXECUTION_FAILED",
                    "An error occurred while executing the database query.", ex);
        }
    }
}
