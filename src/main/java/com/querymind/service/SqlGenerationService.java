package com.querymind.service;

import com.querymind.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for communicating with the AI model provider to translate
 * natural-language questions into SQL queries using the approved database schema.
 *
 * NOTE: Per QueryMind architecture, this service ONLY generates SQL text.
 * It NEVER validates or executes the generated SQL.
 */
@Service
public class SqlGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SqlGenerationService.class);
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final Pattern CODE_BLOCK_PATTERN =
            Pattern.compile("^```(?:sql)?\\s*([\\s\\S]*?)\\s*```$", Pattern.CASE_INSENSITIVE);

    private static final String SYSTEM_PROMPT = """
        You are a SQL generation assistant for a PostgreSQL database.
        Your job is to translate the user's natural-language question into a single valid PostgreSQL SELECT query.

        Strict Rules:
        1. Output ONLY the raw SQL query.
        2. Do NOT output markdown code blocks (e.g., no ```sql or ```), backticks, explanations, preamble, or notes.
        3. Only generate a single read-only SELECT statement.
        4. Do NOT generate INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, EXECUTE, or multi-statement SQL.
        5. Use only the tables and columns provided in the schema.
        """;

    private final ChatClient chatClient;
    private final SchemaService schemaService;

    @Autowired
    public SqlGenerationService(ChatClient.Builder chatClientBuilder, SchemaService schemaService) {
        this.chatClient = chatClientBuilder.build();
        this.schemaService = schemaService;
    }

    /**
     * Protected constructor for test stubs.
     */
    protected SqlGenerationService() {
        this.chatClient = null;
        this.schemaService = null;
    }

    /**
     * Translates a natural language question into a candidate SQL query string.
     *
     * @param question the user's natural language question
     * @return the raw generated SQL query
     * @throws IllegalArgumentException if the question is blank or exceeds length limits
     * @throws AiServiceException if the AI provider call fails or returns an empty response
     */
    public String generateSql(String question) {
        validateQuestion(question);

        String trimmedQuestion = question.trim();
        log.info("Requesting SQL generation for question: '{}'", trimmedQuestion);

        String userPrompt = buildUserPrompt(trimmedQuestion);

        try {
            String rawResponse = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            if (rawResponse == null || rawResponse.isBlank()) {
                throw new AiServiceException("EMPTY_AI_RESPONSE", "AI provider returned an empty response.");
            }

            String cleanedSql = cleanSqlOutput(rawResponse);
            log.info("AI generated SQL: '{}'", cleanedSql);
            return cleanedSql;
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("AI provider failure during SQL generation: {}", ex.getMessage(), ex);
            throw new AiServiceException("AI_PROVIDER_ERROR", "Failed to communicate with AI provider.", ex);
        }
    }

    private void validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be blank.");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Question length exceeds maximum limit of " + MAX_QUESTION_LENGTH + " characters.");
        }
    }

    private String buildUserPrompt(String question) {
        return """
            Database Schema:
            %s

            User Question:
            %s
            """.formatted(schemaService.getSchemaDescription(), question);
    }

    /**
     * Strips accidental markdown code fences, backticks, or trailing semicolons.
     */
    public static String cleanSqlOutput(String rawSql) {
        String trimmed = rawSql.trim();

        Matcher matcher = CODE_BLOCK_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            trimmed = matcher.group(1).trim();
        }

        // Strip single leading/trailing backticks if present
        if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        return trimmed;
    }
}
