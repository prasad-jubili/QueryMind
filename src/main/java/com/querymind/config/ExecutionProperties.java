package com.querymind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "querymind.execution")
public class ExecutionProperties {

    /**
     * Maximum number of rows returned by query execution.
     */
    private int maxRows = 100;

    /**
     * Timeout in seconds for executing queries.
     */
    private int queryTimeoutSeconds = 5;

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }
}
