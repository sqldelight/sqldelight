CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  username TEXT,
  bio TEXT,
  image TEXT
);

CREATE TABLE articles (
  id INTEGER PRIMARY KEY,
  slug TEXT,
  title TEXT,
  description TEXT,
  body TEXT,
  author_id INTEGER REFERENCES users(id),
  createdAt TIMESTAMP,
  updatedAt TIMESTAMP
);

CREATE TABLE tags (
  id INTEGER PRIMARY KEY,
  article_id INTEGER REFERENCES articles(id),
  tag TEXT
);

SELECT articles.id, slug, title, description, body, users.username, users.bio, users.image, createdAt, updatedAt,
COALESCE (string_agg (DISTINCT tag, ',' ORDER BY tag DESC) FILTER (WHERE tag IS NOT NULL)) AS articleTags
FROM articles
LEFT JOIN tags ON articles.id = tags.article_id
JOIN users ON articles.author_id = users.id
GROUP BY articles.id, users.id;

SELECT string_agg (DISTINCT tag, ',') AS articleTags
FROM articles
LEFT JOIN tags ON articles.id = tags.article_id
JOIN users ON articles.author_id = users.id
GROUP BY articles.id, users.id;

SELECT string_agg (DISTINCT title || ' ' || tag, ',' ) AS articleTags
FROM articles
LEFT JOIN tags ON articles.id = tags.article_id
JOIN users ON articles.author_id = users.id
GROUP BY articles.id, users.id;

SELECT username, string_agg (tag, ',')
FROM articles
LEFT JOIN tags ON articles.id = tags.article_id
JOIN users ON articles.author_id = users.id
GROUP BY articles.id, users.id;

SELECT articles.id, slug, title, description, body, users.username, users.bio, users.image, createdAt, updatedAt,
COALESCE (array_agg (DISTINCT tag ORDER BY tag) FILTER (WHERE tag IS NOT NULL), '{}') AS articleTags
FROM articles
LEFT JOIN tags ON articles.id = tags.article_id
JOIN users ON articles.author_id = users.id
GROUP BY articles.id, users.id;

SELECT array_agg (tag ORDER BY tag)
FROM articles
LEFT JOIN tags ON articles.id = tags.article_id
JOIN users ON articles.author_id = users.id
GROUP BY articles.id, users.id;
