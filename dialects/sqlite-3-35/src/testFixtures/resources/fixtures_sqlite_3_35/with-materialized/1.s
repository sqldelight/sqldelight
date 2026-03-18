CREATE TABLE employees (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    department TEXT NOT NULL,
    salary INTEGER NOT NULL
);

--- Using MATERIALIZED or NOT MATERIALIZED after the AS keyword provides non-binding hints to the query planner
--- about how the CTE should be implemented.

--- If the MATERIALIZED phrase is used, then select-stmt will be materialized into an ephemeral table that is
--- held in memory or in a temporary disk file.
WITH dept_avg_materialized AS MATERIALIZED (
    SELECT
        department,
        AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
)
SELECT
    e.name,
    e.department,
    e.salary,
    da.avg_salary,
    e.salary - da.avg_salary AS diff_from_avg
FROM employees e
JOIN dept_avg_materialized da ON e.department = da.department
ORDER BY e.department, e.name;

WITH update_dept_avg_materialized AS MATERIALIZED (
    SELECT
        department,
        AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
)
UPDATE employees
SET salary = salary * 1.10
WHERE id IN (
    SELECT e.id
    FROM employees e
    JOIN update_dept_avg_materialized da ON e.department = da.department
    WHERE e.salary < da.avg_salary
);

--- If the NOT MATERIALIZED phrase is used, then select-stmt is substituted as a subquery in place of every occurrence
--- of the CTE table name. The query planner is still free to implement the subquery using materialization if it feels
--- that is the best solution. The true meaning of NOT MATERIALIZED is closer to "TREAT LIKE ANY ORDINARY VIEW OR SUBQUERY
WITH dept_avg_not_materialized AS NOT MATERIALIZED (
    SELECT
        department,
        AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
)
SELECT
    e.name,
    e.department,
    e.salary,
    da.avg_salary,
    e.salary - da.avg_salary AS diff_from_avg
FROM employees e
JOIN dept_avg_not_materialized da ON e.department = da.department
ORDER BY e.department, e.name;

WITH delete_dept_avg_not_materialized AS NOT MATERIALIZED (
    SELECT
        department,
        AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
)
DELETE FROM employees
WHERE id IN (
    SELECT e.id
    FROM employees e
    JOIN delete_dept_avg_not_materialized da ON e.department = da.department
    WHERE e.salary < (da.avg_salary * 2)
);

--- If neither hint is present, then SQLite is free to choose whatever implementation strategy it thinks will work best.
--- This is the recommended approach. Do not use the MATERIALIZED or NOT MATERIALIZED keywords on a common table
--- expression unless you have a compelling reason to do so.
WITH dept_avg AS (
    SELECT
        department,
        AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
)
SELECT
    e.name,
    e.department,
    e.salary,
    da.avg_salary,
    e.salary - da.avg_salary AS diff_from_avg
FROM employees e
JOIN dept_avg da ON e.department = da.department
ORDER BY e.department, e.name;

WITH
  dept_avg AS MATERIALIZED (
    SELECT
      department,
      AVG(salary) AS avg_salary,
      COUNT(*) AS employee_count
    FROM employees
    GROUP BY department
  ),

  above_avg_earners AS NOT MATERIALIZED (
    SELECT
      e.id,
      e.name,
      e.department,
      e.salary,
      da.avg_salary,
      e.salary - da.avg_salary AS salary_diff
    FROM employees e
    JOIN dept_avg da ON e.department = da.department
    WHERE e.salary > da.avg_salary
  )
SELECT *
FROM above_avg_earners
ORDER BY avg_salary DESC;
