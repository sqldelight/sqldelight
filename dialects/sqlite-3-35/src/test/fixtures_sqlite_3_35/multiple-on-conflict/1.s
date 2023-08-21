CREATE TABLE apparel (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  size TEXT NOT NULL,
  price TEXT NOT NULL,
  quantity INTEGER,
  CONSTRAINT apparel_name_size_key UNIQUE (name, size)
);

INSERT INTO apparel (id, name, size, price, quantity) VALUES(31, 'Shirt', 'XL', 3.99, 3)
ON CONFLICT(name, size) DO UPDATE SET size = excluded.size, quantity = excluded.quantity
ON CONFLICT DO UPDATE SET name= excluded.name = excluded.size, quantity = excluded.quantity;

INSERT INTO apparel (id, name, size, price, quantity) VALUES(31, 'Shirt', 'XL', 2.99, 2)
ON CONFLICT(name, size) DO UPDATE SET size = excluded.size, quantity = excluded.quantity
ON CONFLICT DO NOTHING;




