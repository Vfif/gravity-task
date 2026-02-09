--liquibase formatted sql

--changeset gravity:002-create-exchange-rate-table
CREATE TABLE exchange_rate (
    id              BIGSERIAL      PRIMARY KEY,
    base_currency   VARCHAR(3)     NOT NULL,
    target_currency VARCHAR(3)     NOT NULL,
    rate            NUMERIC(18, 8) NOT NULL,
    source          VARCHAR(50)    NOT NULL,
    timestamp       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exchange_rate_currencies
    ON exchange_rate (base_currency, target_currency, timestamp DESC);

CREATE INDEX idx_exchange_rate_timestamp
    ON exchange_rate (timestamp DESC);

--rollback DROP TABLE exchange_rate;
