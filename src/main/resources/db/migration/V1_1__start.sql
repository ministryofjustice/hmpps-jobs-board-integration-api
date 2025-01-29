CREATE TABLE IF NOT EXISTS employers_ext_ids
(
    id VARCHAR(36) UNIQUE,
    ext_id BIGINT UNIQUE,
    created_at TIMESTAMP(6) NOT NULL,
    last_modified_at TIMESTAMP(6) NOT NULL,

    PRIMARY KEY (id, ext_id)
);
