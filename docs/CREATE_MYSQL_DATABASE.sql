-- BEGO MySQL database bootstrap
-- Execute this file with a MySQL administrator account before running backend Flyway migrations.
-- Flyway will create application tables in stage 1; this script only creates the database and app user.

CREATE DATABASE IF NOT EXISTS bego
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'bego_app'@'localhost'
    IDENTIFIED BY 'icebear618';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON bego.*
    TO 'bego_app'@'localhost';

FLUSH PRIVILEGES;

