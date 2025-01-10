CREATE TABLE locations (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  point GEOMETRY(Point, 4326) NOT NULL
);

INSERT INTO locations (name, point)
VALUES ('New York', ST_MakePoint(:x, :y));

INSERT INTO locations (name, point)
VALUES ('New York', ST_SetSRID(ST_MakePointM(:x, :y, :m), :srid));

INSERT INTO locations (name, point)
VALUES ('New York', ST_SetSRID(ST_Point(:x, :y, :srid)));

INSERT INTO locations (name, point)
VALUES ('New York', ST_SetSRID(ST_PointZ(:x, :y, :z, :srid)));

INSERT INTO locations (name, point)
VALUES ('New York', ST_SetSRID(ST_PointM(:x, :y, :m, :srid)));

INSERT INTO locations (name, point)
VALUES ('New York', ST_SetSRID(ST_PointZM(:x, :y, :z, :m, :srid)));

SELECT ST_MakePoint(-71.1043443253471, 42.3150676015829);

SELECT ST_MakePoint(1, 2, 1.5);

SELECT ST_MakePoint(?::numeric, ?::numeric);

SELECT ST_MakePoint(?, ?);

SELECT ST_MakePoint(:x, :y);

SELECT ST_MakePointM(-71.1043443253471, 42.3150676015829, :m);

SELECT ST_PointZM(-71.104, 42.315, 3.4, 4.5, 4326);

SELECT ST_PointZM(-71.104, 42.315, 3.4, :m);

SELECT ST_SetSRID(ST_MakePoint(-123.365556, 48.428611), 4326) AS wgs84long_lat;

SELECT ST_DWithin(
  ST_GeographyFromText('SRID=4326;POINT(13.405 52.52)'),
  ST_GeographyFromText('SRID=4326;POINT(19.945 50.0647)'),
  4000,
  TRUE
);
