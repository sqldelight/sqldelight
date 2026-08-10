CREATE TABLE departments (
  dept_id   INTEGER PRIMARY KEY,
  dept_name TEXT NOT NULL
);

CREATE TABLE employees (
  emp_id   INTEGER PRIMARY KEY,
  emp_name TEXT NOT NULL,
  dept_id  INTEGER
);

SELECT e.emp_name, d.dept_name
FROM employees e
  RIGHT JOIN departments d ON e.dept_id = d.dept_id
ORDER BY d.dept_name;

SELECT d.dept_name
FROM employees e
  RIGHT OUTER JOIN departments d ON e.dept_id = d.dept_id
WHERE e.emp_id IS NULL;

SELECT e.emp_name, d.dept_name
FROM employees e
  FULL JOIN departments d ON e.dept_id = d.dept_id
ORDER BY d.dept_name, e.emp_name;

SELECT e.emp_name, d.dept_name
FROM employees e
  FULL OUTER JOIN departments d ON e.dept_id = d.dept_id
WHERE e.emp_id IS NULL OR d.dept_id IS NULL;
