
---
USE cachecruncher;
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
    second                  INT              NOT NULL,
    endpoint                VARCHAR(255)     NOT NULL,
    status                  SMALLINT         NOT NULL,
    data                    MEDIUMTEXT       NOT NULL
);

---..
DROP SEQUENCE IF EXISTS cache_trace_id_seq;
CREATE SEQUENCE cache_trace_id_seq START WITH 1 INCREMENT BY 1 CACHE 1024;

DROP TABLE IF EXISTS cache_trace;
CREATE TABLE IF NOT EXISTS cache_trace (

    id                      BIGINT           PRIMARY KEY DEFAULT (NEXT VALUE FOR cache_trace_id_seq),
    created_at              BIGINT           NOT NULL,
    updated_at              BIGINT           NOT NULL,
    description             VARCHAR(1024)    NOT NULL,
    name                    VARCHAR(128)     NOT NULL,
    data                    TEXT             NOT NULL
);

---
SET FOREIGN_KEY_CHECKS = 1;

---
