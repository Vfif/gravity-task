--liquibase formatted sql

--changeset gravity:003-create-user-tables
CREATE TABLE app_user (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE user_role (
    user_id BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

--rollback DROP TABLE user_role; DROP TABLE app_user;
