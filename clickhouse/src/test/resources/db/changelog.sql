--liquibase formatted sql

--changeset example:1
CREATE TABLE IF NOT EXISTS users
(
    id INT
)
    ENGINE = MergeTree()
    PRIMARY KEY (id);
