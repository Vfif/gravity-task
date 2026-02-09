--liquibase formatted sql

--changeset gravity:001-create-currency-table
CREATE TABLE currency (
    id         BIGSERIAL    PRIMARY KEY,
    code       VARCHAR(3)   NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

--rollback DROP TABLE currency;
