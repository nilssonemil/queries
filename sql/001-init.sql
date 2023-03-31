CREATE TABLE users (
    id          VARCHAR(256) PRIMARY KEY
);

CREATE TABLE questions (
    id          UUID PRIMARY KEY,
    questioner  VARCHAR(256) REFERENCES users(id),
    summary       VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL
);
