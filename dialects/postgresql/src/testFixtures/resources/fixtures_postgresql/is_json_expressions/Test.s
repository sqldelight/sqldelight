CREATE TABLE myTable(
    data TEXT NOT NULL,
    js_1 JSON CHECK (js_1 -> 'scores' IS NOT NULL AND js_1 -> 'scores' IS JSON ARRAY),
    js_2 JSON CHECK ((js_2 -> 'scores'::TEXT) IS NOT NULL AND (js_2 -> 'scores'::TEXT) IS JSON ARRAY),
    js_3 JSON CHECK ((js_3 -> 'scores' IS JSON ARRAY) IS TRUE),
    js_4 JSON CHECK (pg_input_is_valid(js_4 ->> 'factor', 'double precision'))
);

SELECT data,
  data IS NULL,
  data IS NOT NULL,
  data IS JSON,
  data IS NOT JSON "json?",
  data IS JSON VALUE "value?",
  data IS JSON SCALAR "scalar?",
  data IS JSON OBJECT "object?",
  data IS NOT JSON OBJECT "object?",
  data IS JSON ARRAY "array?",
  data IS JSON ARRAY WITH UNIQUE KEYS "array with unq key?",
  data IS JSON ARRAY WITHOUT UNIQUE KEYS "array without unq key?"
FROM myTable
WHERE data IS NOT NULL;
