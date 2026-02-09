--liquibase formatted sql

--changeset gravity:004-seed-users
-- Password for all users is 'password' (BCrypt encoded)
-- Generated with BCryptPasswordEncoder(10)

INSERT INTO app_user (username, password, enabled)
VALUES ('user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true);

INSERT INTO app_user (username, password, enabled)
VALUES ('premium', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true);

INSERT INTO app_user (username, password, enabled)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true);

-- Assign roles
INSERT INTO user_role (user_id, role) VALUES ((SELECT id FROM app_user WHERE username = 'user'), 'USER');

INSERT INTO user_role (user_id, role) VALUES ((SELECT id FROM app_user WHERE username = 'premium'), 'USER');
INSERT INTO user_role (user_id, role) VALUES ((SELECT id FROM app_user WHERE username = 'premium'), 'PREMIUM_USER');

INSERT INTO user_role (user_id, role) VALUES ((SELECT id FROM app_user WHERE username = 'admin'), 'USER');
INSERT INTO user_role (user_id, role) VALUES ((SELECT id FROM app_user WHERE username = 'admin'), 'PREMIUM_USER');
INSERT INTO user_role (user_id, role) VALUES ((SELECT id FROM app_user WHERE username = 'admin'), 'ADMIN');

--rollback DELETE FROM user_role; DELETE FROM app_user;
