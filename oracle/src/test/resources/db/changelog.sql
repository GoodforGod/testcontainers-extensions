--liquibase formatted sql

--changeset example:1
CREATE TABLE users
(
    id INTEGER NOT NULL PRIMARY KEY
)
