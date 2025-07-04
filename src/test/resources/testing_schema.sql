
---
USE cachecruncher_test;
SET FOREIGN_KEY_CHECKS = 0;

---
DROP SEQUENCE IF EXISTS log_id_seq;
CREATE SEQUENCE IF NOT EXISTS log_id_seq START WITH 1 INCREMENT BY 1 CACHE 1024;

DROP TABLE IF EXISTS log;
CREATE TABLE IF NOT EXISTS log (

    id                      BIGINT           PRIMARY KEY DEFAULT (NEXT VALUE FOR log_id_seq),
    created_at              BIGINT           NOT NULL,
    thread                  VARCHAR(128)     NOT NULL,
    logger                  VARCHAR(128)     NOT NULL,
    message                 TEXT             NOT NULL,

    level                   ENUM(
                                'TRACE',
                                'DEBUG',
                                'INFO',
                                'WARN',
                                'ERROR'
                            )                NOT NULL
);

---..
DROP SEQUENCE IF EXISTS metric_id_seq;
CREATE SEQUENCE IF NOT EXISTS metric_id_seq START WITH 1 INCREMENT BY 1 CACHE 1024;

DROP TABLE IF EXISTS metric;
CREATE TABLE IF NOT EXISTS metric (

    id                      BIGINT           PRIMARY KEY DEFAULT (NEXT VALUE FOR metric_id_seq),
    created_at              BIGINT           NOT NULL,
    timestamp               BIGINT           NOT NULL,
    endpoint                VARCHAR(255)     NOT NULL,
    data                    MEDIUMTEXT       NOT NULL,
    status                  SMALLINT         NOT NULL
);

---..
DROP SEQUENCE IF EXISTS cache_trace_id_seq;
CREATE SEQUENCE cache_trace_id_seq START WITH 1 INCREMENT BY 1 CACHE 1024;

DROP TABLE IF EXISTS cache_trace;
CREATE TABLE IF NOT EXISTS cache_trace (

    id                      BIGINT           PRIMARY KEY DEFAULT (NEXT VALUE FOR cache_trace_id_seq),
    created_at              BIGINT           NOT NULL,
    updated_at              BIGINT           NULL,
    description             VARCHAR(1024)    NOT NULL,
    name                    VARCHAR(128)     NOT NULL,
    statistics              TEXT             NOT NULL,
    data                    MEDIUMBLOB       NOT NULL
);

---..
DROP SEQUENCE IF EXISTS user_id_seq;
CREATE SEQUENCE user_id_seq START WITH 1 INCREMENT BY 1 CACHE 1024;

DROP TABLE IF EXISTS user;
CREATE TABLE IF NOT EXISTS user (

    id                      BIGINT           PRIMARY KEY DEFAULT (NEXT VALUE FOR user_id_seq),
    locked_until            BIGINT           NULL,
    created_at              BIGINT           NOT NULL,
    validated_at            BIGINT           NULL,
    email                   VARCHAR(64)      NOT NULL UNIQUE,
    password                VARCHAR(128)     NOT NULL,
    failed_accesses         SMALLINT         NOT NULL,

    role                   ENUM(
                                'DEFAULT',
                                'UPLOADER',
                                'ADMIN'
                            )                NOT NULL
);

---..
DROP TABLE IF EXISTS session;
CREATE TABLE IF NOT EXISTS session (

    user_id                 BIGINT           NOT NULL,
    expires_at              BIGINT           NOT NULL,
    email                   VARCHAR(64)      NOT NULL,
    device                  VARCHAR(128)     NOT NULL,
    id                      VARCHAR(128)     PRIMARY KEY,

    role                   ENUM(
                                'DEFAULT',
                                'UPLOADER',
                                'ADMIN'
                            )                NOT NULL,

    CONSTRAINT session_fk FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE CASCADE
);

---
SET FOREIGN_KEY_CHECKS = 1;

---.
INSERT INTO user (locked_until, created_at, validated_at, email, password, failed_accesses, role) VALUES (NULL, 0, 0, 'admin@test.com', '$2a$10$cbe9tmKjwizZrYcZbwBGjuHdOh6IwTLeW12seIPXAKDBiMxu66YgG', 0, 'ADMIN'); -- Password123?!

INSERT INTO user (locked_until, created_at, validated_at, email, password, failed_accesses, role) VALUES (NULL, 0, 0, 'normal@test.com', '$2a$10$6DJuBQV.mIQCmTxEDJ6vFOBZzoFCAJHE.nzG8qwu9DhLZmvJCYCHW', 0, 'DEFAULT'); -- Password123?!

INSERT INTO user (locked_until, created_at, validated_at, email, password, failed_accesses, role) VALUES (NULL, 0, 0, 'uploader@test.com', '$2a$10$6DJuBQV.mIQCmTxEDJ6vFOBZzoFCAJHE.nzG8qwu9DhLZmvJCYCHW', 0, 'UPLOADER'); -- Password123?!

---
