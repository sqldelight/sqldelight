CREATE TABLE person (
    id INTEGER PRIMARY KEY,
    name TEXT,
    created_at TIMESTAMPTZ
);

SELECT DISTINCT ON (person.name) *
FROM person;

SELECT DISTINCT ON (name) *
FROM person;

CREATE TABLE student(
    student_id INTEGER PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE grade(
    grade_id INTEGER PRIMARY KEY,
    student_id INTEGER REFERENCES student(student_id),
    grade INT NOT NULL,
    grade_date TIMESTAMP NOT NULL
);

SELECT DISTINCT ON (grade.student_id) grade.*, student.*
FROM grade
JOIN student USING (student_id)
ORDER BY grade.student_id, grade_date;

SELECT DISTINCT ON (grade.student_id, grade.grade_date) grade.*, student.*
FROM grade
JOIN student USING (student_id)
ORDER BY grade.student_id, grade_date;

SELECT DISTINCT ON (student_id) *
FROM grade
JOIN student USING (student_id)
ORDER BY student_id, grade_date;

-- fail
SELECT DISTINCT ON (name) *
FROM person
ORDER BY created_at DESC;

-- fail
SELECT DISTINCT ON (name, created_at) id, name, created_at
FROM person
ORDER BY id, name, created_at DESC;

-- fail
SELECT DISTINCT ON (name, id) id, name, created_at
FROM person
ORDER BY name, created_at, id DESC;
