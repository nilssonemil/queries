CREATE TABLE questions (
    id          UUID PRIMARY KEY,
    title       VARCHAR(100) NOT NULL
);

CREATE TABLE users (
    id          VARCHAR(256) PRIMARY KEY
);