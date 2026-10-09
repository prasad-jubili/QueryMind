package com.querymind.service;

import com.querymind.exception.AiServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqlGenerationServiceTest {

    private ChatClient chatClient;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private SchemaService schemaService;
    private SqlGenerationService sqlGenerationService;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        chatClientBuilder = mock(ChatClient.Builder.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        schemaService = new SchemaService();
        sqlGenerationService = new SqlGenerationService(chatClientBuilder, schemaService);
    }

    @Test
    void shouldGenerateSqlSuccessfully() {
        when(responseSpec.content())
                .thenReturn("SELECT employee_name, salary FROM employees ORDER BY salary DESC LIMIT 5");

        String sql = sqlGenerationService.generateSql("Show the 5 employees with highest salary");

        assertThat(sql).isEqualTo("SELECT employee_name, salary FROM employees ORDER BY salary DESC LIMIT 5");
    }

    @Test
    void shouldCleanMarkdownFencesFromModelResponse() {
        String fencedSql = """
            ```sql
            SELECT employee_id, employee_name FROM employees
            ```
            """;
        when(responseSpec.content()).thenReturn(fencedSql);

        String sql = sqlGenerationService.generateSql("List all employees");

        assertThat(sql).isEqualTo("SELECT employee_id, employee_name FROM employees");
    }

    @Test
    void shouldStripBackticksFromSingleLineSql() {
        when(responseSpec.content()).thenReturn("`SELECT * FROM employees`");

        String sql = sqlGenerationService.generateSql("List employees");

        assertThat(sql).isEqualTo("SELECT * FROM employees");
    }

    @Test
    void shouldRejectBlankQuestion() {
        assertThatThrownBy(() -> sqlGenerationService.generateSql("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Question must not be blank.");
    }

    @Test
    void shouldRejectNullQuestion() {
        assertThatThrownBy(() -> sqlGenerationService.generateSql(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Question must not be blank.");
    }

    @Test
    void shouldRejectOverlyLongQuestion() {
        String longQuestion = "a".repeat(501);

        assertThatThrownBy(() -> sqlGenerationService.generateSql(longQuestion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Question length exceeds maximum limit");
    }

    @Test
    void shouldWrapEmptyResponseInAiServiceException() {
        when(responseSpec.content()).thenReturn("   ");

        assertThatThrownBy(() -> sqlGenerationService.generateSql("Show employees"))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("AI provider returned an empty response.")
                .satisfies(ex -> assertThat(((AiServiceException) ex).getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE"));
    }

    @Test
    void shouldWrapProviderFailureInAiServiceException() {
        when(requestSpec.call()).thenThrow(new RuntimeException("OpenRouter timeout"));

        assertThatThrownBy(() -> sqlGenerationService.generateSql("Show employees"))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("Failed to communicate with AI provider.")
                .satisfies(ex -> assertThat(((AiServiceException) ex).getErrorCode()).isEqualTo("AI_PROVIDER_ERROR"));
    }
}
