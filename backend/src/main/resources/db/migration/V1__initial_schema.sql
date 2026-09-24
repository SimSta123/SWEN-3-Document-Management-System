CREATE TABLE dbcheck (
    id BIGSERIAL PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);

INSERT INTO dbcheck (message) VALUES ('Works?');