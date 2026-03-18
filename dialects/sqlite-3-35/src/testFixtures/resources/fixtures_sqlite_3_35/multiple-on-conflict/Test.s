CREATE TABLE test_conflict (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  size TEXT NOT NULL,
  price TEXT NOT NULL,
  quantity INTEGER,
  CONSTRAINT test_name_size_key UNIQUE (name, size)
);

-- succeeds with multiple ON CONFLICT clauses
INSERT INTO test_conflict (id, name, size, price, quantity) VALUES(31, 'Test', 'XL', 3.99, 3)
ON CONFLICT(name, size) DO  UPDATE SET size = excluded.size, quantity = excluded.quantity
ON CONFLICT DO UPDATE SET name = excluded.name = excluded.size, quantity = excluded.quantity;

-- fails with optional conflict target is only allowed with the final ON CONFLICT clause
INSERT INTO test_conflict (id, name, size, price, quantity) VALUES(31, 'Test', 'XL', 3.99, 3)
ON CONFLICT DO UPDATE SET name = excluded.name = excluded.size, quantity = excluded.quantity
ON CONFLICT(name, size) DO  UPDATE SET size = excluded.size, quantity = excluded.quantity;
