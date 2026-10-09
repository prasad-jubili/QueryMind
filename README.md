# QueryMind --- AI SQL Assistant

> **Project purpose:** Build a practical, secure, Java-first application
> that turns natural-language questions into SQL, validates the
> generated SQL, executes approved read-only queries, and explains the
> results.

This README is both the project brief for the developer and the working
guide for an AI coding agent. Treat it as the current source of truth.
If a requirement is ambiguous, ask before making a major architectural
decision.

------------------------------------------------------------------------

## 1. Product vision

A user asks a question such as:

> Show the five employees with the highest salaries.

QueryMind uses the database schema and an AI model to generate SQL,
checks that the SQL is permitted, executes it using a restricted
database account, and returns the SQL plus the results.

### Main user flow

1.  User submits a natural-language question.
2.  Backend loads the allowed database schema.
3.  AI provider generates one SQL query.
4.  A separate validation layer parses and checks the SQL.
5.  Only an approved read-only query is executed.
6.  Backend returns the SQL, column names, result rows, and a useful
    error if something fails.
7.  Later, the application can explain the query and summarize its
    results.

**Core principle:** The AI proposes SQL. The Java application---not the
AI prompt---enforces security and decides whether the query may run.

## 2. Project goals

### Learning goals

-   Improve practical SQL skills: filtering, sorting, joins,
    aggregation, grouping, subqueries, and pagination.
-   Practise Spring Boot REST API design, dependency injection, DTOs,
    services, repositories, validation, and exception handling.
-   Learn how to call an AI API from Java using Spring AI.
-   Learn SQL parsing, safe query execution, integration testing, and
    deployment.

### Product goals

-   Make querying a database accessible to people who do not know SQL.
-   Show the generated SQL so users can inspect and learn from it.
-   Return structured results that a future frontend can render as a
    table.
-   Provide clear errors instead of silently running incorrect queries.
-   Keep the architecture extensible without overengineering the first
    release.

### Non-goals for the first MVP

-   Supporting every SQL dialect.
-   Allowing AI-generated `INSERT`, `UPDATE`, `DELETE`, DDL, stored
    procedures, or arbitrary multi-statement SQL.
-   Connecting to arbitrary user-provided databases.
-   Building a complex frontend before the backend flow works.
-   Using Python for backend logic.
-   Adding agents, tools, or multi-agent orchestration unless a
    demonstrated requirement calls for them.

## 3. Initial technical decisions

Use these defaults unless the developer explicitly approves a change.

  -----------------------------------------------------------------------
Area                                Default
  ----------------------------------- -----------------------------------
Language                            Java 21

Backend                             Spring Boot, Maven

AI integration                      Spring AI with an API-based model
provider

Initial AI provider                 OpenAI API, configurable through
environment variables

Database                            PostgreSQL

Database access                     Spring JDBC / `JdbcTemplate` for
dynamic SQL execution

SQL validation                      A Java SQL parser such as
JSqlParser, plus explicit policy
checks

API style                           REST, JSON

API testing                         Postman or `.http` request files

Automated testing                   JUnit 5, Spring Boot Test, and
PostgreSQL integration tests

Version control                     Git

Later deployment                    Docker; deployment platform to be
chosen when needed

Frontend                            Deferred until backend MVP is
proven
  -----------------------------------------------------------------------

Use Spring Data JPA where it helps with ordinary application-owned
entities, such as query history. **Do not use JPA repository methods to
execute arbitrary AI-generated SQL.** Dynamic SQL execution should be
isolated in a dedicated service using JDBC and strict validation.

Use a Spring AI version compatible with the selected Spring Boot
version. Follow current official documentation rather than copying
dependency versions from old examples.

## 4. MVP scope

The first working version should:

-   Accept a natural-language question.
-   Use a known schema for the sample database.
-   Generate one SQL query through the AI API.
-   Validate the generated query independently of the AI prompt.
-   Execute only approved read-only SQL.
-   Apply a maximum row count and query timeout.
-   Return the generated SQL and structured result data.
-   Handle AI-provider errors, invalid SQL, and database errors safely.
-   Include tests for allowed and rejected queries.
-   Keep API keys and database credentials out of source control.

### Initial sample schema

Use two related tables to practise joins and aggregation.

``` sql
CREATE TABLE departments (
    department_id BIGINT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL
);

CREATE TABLE employees (
    employee_id BIGINT PRIMARY KEY,
    employee_name VARCHAR(100) NOT NULL,
    salary NUMERIC(12, 2),
    department_id BIGINT REFERENCES departments(department_id)
);
```

Add a small, repeatable set of sample data. Include multiple
departments, employees with different salaries, and a few employees with
no department if the schema/test scenario needs them. Use a migration or
seed script so a new developer can recreate the database.

Example questions to test:

-   List all employees.
-   Show the five highest-paid employees.
-   Find employees earning more than 50000.
-   Show the average salary for each department.
-   List employees with their department names.
-   Which department has the highest average salary?

Do not assume that the model always generates correct SQL. Correctness
must be tested against the actual schema and data.

## 5. Architecture

Keep responsibilities separate and easy to test.

``` text
Client (Postman / future frontend)
              |
              v
        REST Controller
              |
              v
       Query Orchestrator
        /      |       \
       v       v        v
 Schema      AI SQL    SQL Policy
 Provider   Generator  Validator
       \       |        /
        \      v       /
         Approved SQL only
                 |
                 v
       Read-only JDBC Executor
                 |
                 v
          Structured Result
                 |
                 v
          JSON API Response
```

### Responsibilities

-   **Controller:** HTTP mapping, request/response DTOs, status codes.
    No SQL generation or database policy logic.
-   **Query orchestration service:** coordinates schema retrieval, AI
    generation, validation, execution, and response creation.
-   **Schema provider:** supplies only approved table and column
    metadata to the model. Avoid exposing secrets, unrelated tables, or
    sensitive data.
-   **SQL generation service:** calls the AI provider and requests a
    constrained SQL response.
-   **SQL validator/policy service:** parses SQL and enforces the
    application policy. Never trust prompt instructions as a security
    boundary.
-   **SQL execution service:** executes only validated SQL using a
    restricted database connection, row limits, and a timeout.
-   **DTOs:** define validated API inputs and stable response shapes.
-   **Exception handling:** translates known failures into safe,
    consistent API errors without leaking credentials, internal stack
    traces, or database internals.

### Suggested package structure

Adapt to the existing project if it already has a sensible structure. Do
not move or rename many files without a reason.

``` text
src/main/java/com/querymind/
  QueryMindApplication.java
  controller/
    QueryController.java
  dto/
    QueryRequest.java
    QueryResponse.java
    QueryResult.java
    ApiError.java
  service/
    QueryOrchestrator.java
    SqlGenerationService.java
    SqlValidationService.java
    SqlExecutionService.java
    SchemaService.java
  config/
    AiConfig.java
    DatabaseConfig.java
  exception/
    InvalidSqlException.java
    QueryExecutionException.java
    GlobalExceptionHandler.java
```

A repository/entity layer can be added later for persistent query
history, users, and saved queries. Avoid creating empty classes before
they are needed.

## 6. API contract

### `POST /api/queries`

Request:

``` json
{
  "question": "Show the five employees with the highest salaries"
}
```

Successful response (illustrative):

``` json
{
  "question": "Show the five employees with the highest salaries",
  "sql": "SELECT employee_id, employee_name, salary FROM employees ORDER BY salary DESC LIMIT 5",
  "columns": ["employee_id", "employee_name", "salary"],
  "rows": [
    [12, "Example Employee", 95000.00]
  ],
  "rowCount": 1
}
```

The example row is illustrative, not required seed data. Choose a
response representation that is consistent and safe; JSON objects keyed
by column name are also acceptable if documented and tested.

Error response (illustrative):

``` json
{
  "code": "SQL_NOT_ALLOWED",
  "message": "The generated query is not permitted."
}
```

Use appropriate HTTP statuses, such as `400` for invalid input, `422`
for a rejected query, `502` or `503` for AI-provider failures where
appropriate, and a generic `500` for unexpected server errors. Do not
return raw stack traces or expose sensitive database error details to
clients.

### API behaviour

-   Reject blank or excessively long questions.
-   Set a sensible maximum request size.
-   Apply a server-side maximum result row count; do not rely on the
    model to add `LIMIT`.
-   Set a database query timeout.
-   Log request IDs and safe operational metadata, but avoid logging API
    keys, credentials, or sensitive result rows.
-   Never execute a query if parsing or policy validation fails.

## 7. Security requirements --- mandatory

Treat generated SQL as untrusted input, even if it came from the
configured AI provider.

1.  **Read-only database role:** Create a database account that can read
    only the approved schema/tables. Do not run generated SQL with an
    owner, administrator, or migration account.
2.  **Parser-based validation:** Parse the SQL using a maintained Java
    SQL parser. Do not rely on a regex or a prompt alone.
3.  **Allowlist policy:** Permit only the supported query forms. Reject
    statements that are not a single approved `SELECT` query. Decide
    explicitly how to handle CTEs, subqueries, set operations, comments,
    and dialect-specific syntax; test each supported form.
4.  **Block side effects:** Reject DML, DDL, transaction control, stored
    procedure calls, file access, and multiple statements. A read-only
    account is an additional defence, not a replacement for validation.
5.  **Limit resource use:** Apply a maximum number of returned rows,
    execution timeout, and request-rate limits where appropriate.
6.  **Schema allowlist:** Restrict accessible schemas and tables. Never
    assume a generated table or column name is valid.
7.  **Secrets:** Read AI keys and database credentials from environment
    variables or a secret manager. Commit only placeholder values in
    `.env.example` or documentation.
8.  **Error safety:** Do not expose stack traces, connection strings,
    credentials, or internal database details in API responses.
9.  **Prompt injection awareness:** Treat user questions and schema
    comments as untrusted. Do not let either override the application's
    SQL policy.
10. **Tests:** Include adversarial inputs such as `DELETE`,
    `DROP TABLE`, multiple statements, attempts to access disallowed
    tables, and queries designed to return too many rows.

A parser can identify SQL structure, but policy must still inspect the
parsed statement and relevant nested expressions. When in doubt, reject
the query. For the MVP, supporting a smaller SQL subset safely is better
than claiming to support all SQL.

## 8. Configuration

Use environment variables for runtime secrets. Keep a placeholder-only
`.env.example` if useful.

Suggested configuration names:

``` text
OPENAI_API_KEY=replace-with-your-key
DB_URL=jdbc:postgresql://localhost:5432/querymind
DB_USERNAME=querymind_app
DB_PASSWORD=replace-with-local-password
```

Map these variables to Spring configuration explicitly in
`application.properties` or `application.yml`. The application should
fail clearly at startup if required configuration is missing, without
printing secret values.

Do not commit real credentials. Ensure the runtime database account is
read-only for query execution. Use a separate migration/owner account
for schema setup.

## 9. Implementation milestones

Work incrementally. Each milestone must compile and have a clear
test/demo.

### Milestone 1 --- Project and database

-   Create the Maven Spring Boot project.
-   Configure PostgreSQL.
-   Add schema migration/seed scripts.
-   Create and test the sample tables and data.
-   Add a health check or basic endpoint.

**Done when:** the application starts and the database schema can be
recreated from the repository.

### Milestone 2 --- Normal SQL execution

-   Create a temporary, developer-only endpoint or test that executes a
    fixed, hard-coded query.
-   Use `JdbcTemplate` and map result columns/rows.
-   Add integration tests against PostgreSQL.
-   Do not accept arbitrary SQL from the public endpoint at this stage.

**Done when:** a fixed query returns structured JSON and database
failures are handled cleanly.

### Milestone 3 --- AI SQL generation

-   Add the Spring AI OpenAI starter and compatible dependency
    management.
-   Configure the API key through the environment.
-   Build a service that sends the question and allowed schema to the
    model.
-   Request one SQL statement in a predictable format.
-   Test a few natural-language questions.

**Done when:** the service returns generated SQL, but does not yet
execute unvalidated model output.

### Milestone 4 --- SQL validation and safe execution

-   Add parser-based validation and explicit allowlist rules.
-   Add a read-only database role.
-   Add server-enforced row limits and query timeouts.
-   Execute only queries that pass validation.
-   Add rejection and integration tests.

**Done when:** supported read queries work and unsafe queries are
rejected before execution.

### Milestone 5 --- Public query API

-   Add request and response DTOs with validation.
-   Add controller, orchestration service, and global exception
    handling.
-   Document the API with example requests.
-   Add tests for success, invalid input, AI failure, SQL rejection, and
    database failure.

**Done when:** the main workflow can be demonstrated using Postman or
`.http` files.

### Milestone 6 --- Product improvements

Prioritize these after the secure MVP:

1.  Explain generated SQL in plain language.
2.  Let users inspect SQL before execution (optional approval mode).
3.  Add query history.
4.  Add a simple frontend for asking questions and viewing result
    tables.
5.  Add authentication and per-user authorization before multi-user
    access.
6.  Add Docker and deployment.
7.  Measure query success rate, latency, and AI cost with
    privacy-conscious logging.

Do not start all milestones in one coding-agent task.

## 10. Testing strategy

Use unit tests for isolated business rules and integration tests for the
real database path.

### Minimum test cases

-   Blank question is rejected.
-   Valid question generates SQL.
-   Generated query referencing an unknown table is rejected.
-   A valid `SELECT` query runs successfully.
-   `DELETE`, `UPDATE`, `INSERT`, `DROP`, and `ALTER` are rejected.
-   Multiple statements are rejected.
-   Disallowed tables are rejected.
-   Excessive result sets are capped or rejected according to policy.
-   Database timeout and AI-provider errors produce safe error
    responses.
-   A rejected query is never sent to the executor.

Mock the AI provider in most automated tests so tests are deterministic
and do not require API credits. Keep a small, optional live-provider
smoke test separate from the default test suite. Use PostgreSQL
integration tests for SQL dialect and execution behaviour.

## 11. Coding conventions

-   Prefer constructor injection.
-   Use clear class and method names.
-   Keep controllers thin and services focused.
-   Use DTOs instead of exposing internal objects directly.
-   Validate inputs at the API boundary.
-   Avoid giant service classes and unnecessary abstractions.
-   Keep secrets out of source code and logs.
-   Write tests for business rules and security policies.
-   Use migrations/seed scripts rather than relying on manual database
    setup.
-   Explain unfamiliar code in straightforward language.
-   Add dependencies only when they solve a clear need.
-   Keep the project runnable after every meaningful change.

## 12. Instructions for the AI coding agent

Follow this workflow for every task.

### Before coding

1.  Inspect the repository, existing source files, build configuration,
    tests, and Git status.
2.  Read this README and any existing architecture notes.
3.  Summarize the current state and identify the smallest next
    milestone.
4.  If a decision materially affects security, database compatibility,
    public APIs, or project structure, explain the options and ask the
    developer before proceeding.
5.  Give a short implementation plan listing files to add/change and
    tests to run.

### While coding

1.  Make small, coherent changes.
2.  Follow the existing project conventions unless they conflict with
    this README.
3.  Do not replace working code or restructure the project without a
    reason.
4.  Do not add unrelated features or dependencies.
5.  Never weaken SQL validation or security to make a demo pass.
6.  Do not invent successful test results. Run the available checks and
    report their actual outcome.
7.  If a dependency or API is uncertain, consult the official
    documentation or inspect the resolved dependency rather than
    guessing.

### After coding

1.  Run the relevant unit tests and integration tests that are
    available.
2.  Run the Maven build, normally `./mvnw test` and, where appropriate,
    `./mvnw verify`.
3.  Report changed files, important design choices, commands run, test
    results, and any unresolved issues.
4.  Explain the implementation simply, including what the developer
    should learn from it.
5.  Provide the next recommended task, but do not begin a large next
    phase without approval.

### Working style

The developer has an intermediate Java level and wants to learn while
building a real product. Do not dump the entire application in one
response. Work collaboratively, explain trade-offs in plain English, and
implement one milestone at a time. The developer makes the final
architectural decisions.

## 13. Definition of done for the MVP

The MVP is complete when:

-   [ ] A developer can configure and run the project from documented
    instructions.
-   [ ] The database can be initialized with repeatable scripts.
-   [ ] A user can submit a natural-language question through the REST
    API.
-   [ ] The application generates SQL using the configured AI API.
-   [ ] The SQL is parsed and validated before execution.
-   [ ] Only approved read-only queries run using a restricted database
    account.
-   [ ] The API returns SQL and structured query results.
-   [ ] Row limits and query timeouts are enforced by the application.
-   [ ] Unsafe queries and provider/database failures are handled
    safely.
-   [ ] Automated tests cover core success and security failure cases.
-   [ ] No secrets are committed to Git.
-   [ ] The README and API examples match the implementation.

## 14. Useful references

-   Spring Boot: https://spring.io/projects/spring-boot
-   Spring AI: https://spring.io/projects/spring-ai
-   Spring AI reference: https://docs.spring.io/spring-ai/reference/
-   Spring AI ChatClient:
    https://docs.spring.io/spring-ai/reference/api/chatclient.html
-   PostgreSQL: https://www.postgresql.org/docs/
-   JSqlParser: https://github.com/JSQLParser/JSqlParser
-   Maven: https://maven.apache.org/

------------------------------------------------------------------------

## First task for the coding agent

Start by **inspecting the repository and proposing the initial
implementation plan**. Do not implement the whole product immediately.

If the repository is empty, propose the Spring Boot project structure,
dependency versions compatible with Java 21, PostgreSQL setup, and the
first milestone. Wait for approval before making significant
architectural choices. Then implement Milestone 1, run the tests/build,
and explain how to run the application locally.
