INSERT INTO departments (department_id, department_name) VALUES
    (1, 'Engineering'),
    (2, 'Sales'),
    (3, 'Marketing'),
    (4, 'Human Resources'),
    (5, 'Research & Development');

INSERT INTO employees (employee_id, employee_name, salary, department_id) VALUES
    (1, 'Alice Smith', 95000.00, 1),
    (2, 'Bob Jones', 82000.00, 1),
    (3, 'Charlie Brown', 110000.00, 1),
    (4, 'Diana Prince', 75000.00, 2),
    (5, 'Evan Wright', 62000.00, 2),
    (6, 'Fiona Gallagher', 48000.00, 2),
    (7, 'George Clark', 58000.00, 3),
    (8, 'Hannah Abbott', 54000.00, 3),
    (9, 'Ian Malcolm', 125000.00, 5),
    (10, 'Julia Roberts', 72000.00, 4),
    (11, 'Kevin Bacon', 45000.00, NULL);
