CREATE TABLE Test (
   txt TEXT NOT NULL
);

SELECT * FROM Test WHERE txt LIKE 'testing%';

SELECT * FROM Test WHERE txt ILIKE 'test%';

SELECT * FROM Test WHERE txt ~~ 'testin%';

SELECT * FROM Test WHERE txt ~~* '%esting%';

SELECT txt !~~ 'testing%' FROM Test;

SELECT txt !~~* 'testing%' FROM Test;

SELECT txt ILIKE 'test%' FROM Test;
