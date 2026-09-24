CREATE TABLE db_check (
    id BIGSERIAL PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);

INSERT INTO db_check (message) VALUES ('Works?');