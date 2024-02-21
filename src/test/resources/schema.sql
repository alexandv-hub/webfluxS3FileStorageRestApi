CREATE TABLE IF NOT EXISTS users (
     id         BIGINT AUTO_INCREMENT PRIMARY KEY,
     username   VARCHAR(64)   NOT NULL UNIQUE,
     password   VARCHAR(2048) NOT NULL,
     role       VARCHAR(32)   NOT NULL,
     first_name VARCHAR(64)   NOT NULL,
     last_name  VARCHAR(64)   NOT NULL,
     enabled    BOOLEAN       NOT NULL DEFAULT FALSE,
     created_at TIMESTAMP,
     updated_at TIMESTAMP,
     status     ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS file (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    location  VARCHAR(255) NOT NULL,
    status    ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS event (
     id       BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id  BIGINT NOT NULL,
     file_id  BIGINT NOT NULL,
     status   ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
     FOREIGN KEY (user_id) REFERENCES users(id),
     FOREIGN KEY (file_id) REFERENCES file(id),
     UNIQUE (user_id, file_id)
);
