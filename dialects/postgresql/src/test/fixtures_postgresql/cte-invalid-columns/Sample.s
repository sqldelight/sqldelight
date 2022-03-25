CREATE TABLE actor(
    id   SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE cast_member(
    actor_id INT  NOT NULL REFERENCES actor,
    film     TEXT NOT NULL
);

WITH actor_ids AS (
    INSERT INTO actor (name)
        VALUES ('Edward Norton')
        RETURNING id AS actor_id
)
INSERT
INTO cast_member
SELECT unknown_id, 'Primal Fear'
FROM actor_ids;
