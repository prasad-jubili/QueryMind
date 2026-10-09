package com.querymind;

import com.querymind.service.SqlGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live-provider smoke test as recommended in README § 10.
 * Verifies real round-trip communication with the configured AI provider.
 */
@SpringBootTest
class LiveSqlGenerationSmokeTest {

    @Autowired
    private SqlGenerationService sqlGenerationService;

    @Test
    void shouldGenerateValidSqlFromRealModel() {
        String apiKey = System.getProperty("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY"));
        if (apiKey == null || !apiKey.startsWith("sk-")) {
            // Skip live test if no real provider key is available in the environment
            return;
        }

        String question = "Show the five employees with the highest salaries";
        String generatedSql = sqlGenerationService.generateSql(question);

        assertThat(generatedSql).isNotBlank();
        assertThat(generatedSql.toUpperCase()).contains("SELECT");
        assertThat(generatedSql.toLowerCase()).contains("employees");
        assertThat(generatedSql.toLowerCase()).contains("salary");
    }
}
