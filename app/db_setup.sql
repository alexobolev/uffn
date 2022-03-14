CREATE TYPE UFFN_ARCHIVE AS ENUM (
    'AO3', 'FFN'
);

CREATE TYPE UFFN_RATING AS ENUM (
    'K', 'T', 'M', 'E'
);

CREATE TYPE UFFN_UPLOAD_STATUS AS ENUM (
    'PENDING', 'FETCHING', 'COMPLETED', 'ERRORED', 'CANCELLED'
);

CREATE TYPE UFFN_UPLOAD_ERROR AS ENUM (
    'PAGE_UNAVAILABLE', 'STORY_INACCESSIBLE', 'MARKUP_UNEXPECTED', 'ERROR_FREEFORM'
);


CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    login VARCHAR NOT NULL UNIQUE,
    email VARCHAR NOT NULL UNIQUE,
    password VARCHAR NOT NULL,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_at TIMESTAMP,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE
);


CREATE TABLE stories (
    id SERIAL PRIMARY KEY,
    origin_identifier VARCHAR NOT NULL,
    origin_archive UFFN_ARCHIVE NOT NULL,
    is_public BOOL NOT NULL DEFAULT FALSE,
    owner_id INT,  -- references users (id)
    owner_summary TEXT,
    owner_rating UFFN_RATING,

    CONSTRAINT fk_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
        ON DELETE SET NULL
);

CREATE UNIQUE INDEX stories_unique_public
    ON stories (origin_identifier, origin_archive)
    WHERE (is_public IS TRUE);

CREATE UNIQUE INDEX stories_unique_user
    ON stories (origin_identifier, origin_archive, owner_id)
    WHERE (is_public IS FALSE);


CREATE TABLE story_versions (
    id SERIAL PRIMARY KEY,
    story_id INT NOT NULL,  -- references stories (id)
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_hidden BOOLEAN NOT NULL DEFAULT TRUE,
    title VARCHAR NOT NULL,
    rating UFFN_RATING,
    summary TEXT,
    notes_pre TEXT,
    notes_post TEXT,
    word_count INT,
    published_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_completed BOOL DEFAULT FALSE,

    CONSTRAINT fk_story
        FOREIGN KEY (story_id)
            REFERENCES stories (id)
        ON DELETE CASCADE
);


CREATE TABLE story_version_authors (
    id SERIAL PRIMARY KEY,
    story_version_id INT NOT NULL,  -- references story_versions (id)
    name VARCHAR NOT NULL,

    CONSTRAINT fk_story_version
        FOREIGN KEY (story_version_id)
            REFERENCES story_versions (id)
        ON DELETE CASCADE
);


CREATE TABLE story_version_chapters (
    id SERIAL PRIMARY KEY,
    story_version_id INT NOT NULL,  -- references story_versions (id)
    sequence_num INT NOT NULL,
    title VARCHAR,
    summary TEXT,
    notes_pre TEXT,
    notes_post TEXT,
    contents TEXT NOT NULL,

    CONSTRAINT fk_story_version
        FOREIGN KEY (story_version_id)
            REFERENCES story_versions (id)
        ON DELETE CASCADE,

    UNIQUE (story_version_id, sequence_num)
);


CREATE TABLE collections (
    id SERIAL PRIMARY KEY,
    parent_id INT,          -- references collections (id)
    owner_id INT NOT NULL,  -- references users (id)
    is_favourite BOOL NOT NULL DEFAULT FALSE,
    title VARCHAR NOT NULL,
    description TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_parent
        FOREIGN KEY (parent_id)
            REFERENCES collections (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
        ON DELETE CASCADE
);


CREATE TABLE collection_stories (
    id SERIAL PRIMARY KEY,
    collection_id INT NOT NULL,  -- references collections (id)
    story_id INT NOT NULL,       -- references stories (id)
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_collection
        FOREIGN KEY (collection_id)
            REFERENCES collections (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_story
        FOREIGN KEY (story_id)
            REFERENCES stories (id)
        ON DELETE CASCADE,

    UNIQUE (collection_id, story_id)
);


CREATE TABLE uploads (
    guid UUID PRIMARY KEY,
    owner_id INT NOT NULL,  -- references users (id)
    origin_archive UFFN_ARCHIVE NOT NULL,
    origin_identifier VARCHAR NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    title VARCHAR,
    status UFFN_UPLOAD_STATUS NOT NULL DEFAULT 'PENDING',
    error_type UFFN_UPLOAD_ERROR,
    error_desc VARCHAR,
    error_time TIMESTAMP,

    CONSTRAINT fk_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uploads_concurrent_user
    ON uploads (owner_id, origin_archive, origin_identifier)
    WHERE (status < 'COMPLETED');


CREATE TABLE upload_sessions (
    id SERIAL PRIMARY KEY,
    owner_id INT NOT NULL,  -- references users (id)
    auth_key VARCHAR NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    user_agent VARCHAR,
    user_address VARCHAR,

    CONSTRAINT fk_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_expires
        CHECK (created_at < expires_at)
);
