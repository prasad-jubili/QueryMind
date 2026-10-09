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
