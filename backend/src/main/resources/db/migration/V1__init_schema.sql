-- V1: początkowy schemat bazy (komplet tabel + indeksy).
-- Enumy jako VARCHAR + CHECK. Atrybuty/filtry per kategoria w JSONB.
-- Listing jest GLOBALNY (bez user_id) - jedno ogłoszenie = jeden rekord.

-- == users: użytkownicy (OAuth2 Google), korzeń relacji per-user ==
CREATE TABLE users (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    google_sub  VARCHAR(255) NOT NULL,
    email       VARCHAR(320) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    picture_url VARCHAR(1024),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_google_sub UNIQUE (google_sub),
    CONSTRAINT uq_users_email      UNIQUE (email)
);

-- == listings: ogłoszenia (globalne). price/currency/city/region nullable ==
CREATE TABLE listings (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    source        VARCHAR(32)   NOT NULL,
    external_id   VARCHAR(255)  NOT NULL,
    category      VARCHAR(16)   NOT NULL,
    title         VARCHAR(512)  NOT NULL,
    description   TEXT,
    price         NUMERIC(14,2),
    currency      VARCHAR(3),
    url           VARCHAR(2048) NOT NULL,
    city          VARCHAR(255),
    region        VARCHAR(255),
    lat           DOUBLE PRECISION,
    lng           DOUBLE PRECISION,
    attributes    JSONB         NOT NULL DEFAULT '{}'::jsonb,
    status        VARCHAR(16)   NOT NULL,
    first_seen_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_listings_source_external UNIQUE (source, external_id),
    CONSTRAINT chk_listings_source   CHECK (source   IN ('OLX', 'OTODOM', 'OTOMOTO', 'ALLEGRO')),
    CONSTRAINT chk_listings_category CHECK (category IN ('PLOT', 'CAR')),
    CONSTRAINT chk_listings_status   CHECK (status   IN ('ACTIVE', 'INACTIVE', 'MERGED'))
);

-- == price_history: historia cen (insert-only) ==
CREATE TABLE price_history (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id  UUID          NOT NULL,
    price       NUMERIC(14,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL,
    recorded_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_price_history_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE
);

-- == saved_listings: zapisane ogłoszenia (per-user) ==
CREATE TABLE saved_listings (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    listing_id UUID        NOT NULL,
    notes      TEXT,
    saved_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_saved_listings_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_listings_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT uq_saved_listings_user_listing UNIQUE (user_id, listing_id)
);

-- == search_criteria: kryteria wyszukiwania (per-user), filtry w JSONB ==
CREATE TABLE search_criteria (
    id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id  UUID         NOT NULL,
    name     VARCHAR(255) NOT NULL,
    category VARCHAR(16)  NOT NULL,
    filters  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT fk_search_criteria_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_search_criteria_category CHECK (category IN ('PLOT', 'CAR'))
);

-- == notifications: powiadomienia (bell w UI) ==
CREATE TABLE notifications (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    listing_id UUID        NOT NULL,
    type       VARCHAR(16) NOT NULL,
    is_read    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT chk_notifications_type CHECK (type IN ('PRICE_DROP', 'NEW_MATCH', 'REPOSTED'))
);

-- == duplicate_groups + members: wykrywanie/scalanie duplikatów ==
CREATE TABLE duplicate_groups (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    primary_listing_id UUID        NOT NULL,
    status             VARCHAR(16) NOT NULL,
    CONSTRAINT fk_duplicate_groups_primary
        FOREIGN KEY (primary_listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT chk_duplicate_groups_status CHECK (status IN ('SUGGESTED', 'CONFIRMED', 'REJECTED'))
);

CREATE TABLE duplicate_group_members (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id   UUID NOT NULL,
    listing_id UUID NOT NULL,
    CONSTRAINT fk_duplicate_group_members_group
        FOREIGN KEY (group_id) REFERENCES duplicate_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_duplicate_group_members_listing
        FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE,
    CONSTRAINT uq_duplicate_group_members UNIQUE (group_id, listing_id)
);

-- == indeksy (w tym GIN dla kolumn JSONB) ==
CREATE INDEX idx_listings_status_last_seen     ON listings (status, last_seen_at);
CREATE INDEX idx_listings_category_region      ON listings (category, region);
CREATE INDEX idx_listings_attributes_gin       ON listings USING GIN (attributes);
CREATE INDEX idx_search_criteria_filters_gin   ON search_criteria USING GIN (filters);
CREATE INDEX idx_notifications_user_is_read     ON notifications (user_id, is_read);
CREATE INDEX idx_price_history_listing_recorded ON price_history (listing_id, recorded_at);
