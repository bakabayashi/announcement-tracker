-- V3: enable PostGIS and derive a geography point from lng/lat for radius search (deduplication ±1km).

CREATE EXTENSION IF NOT EXISTS postgis;

-- Stored generated column: a WGS84 geography point, NULL when either coordinate is missing
-- (ST_MakePoint returns NULL for NULL inputs). The geometry->geography cast is immutable, so it is
-- allowed in a STORED generated column.
ALTER TABLE listings
    ADD COLUMN geo geography(Point, 4326)
        GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography) STORED;

CREATE INDEX idx_listings_geo ON listings USING GIST (geo);
