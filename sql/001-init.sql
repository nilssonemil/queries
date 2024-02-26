CREATE TABLE users (
    id          VARCHAR(256) PRIMARY KEY
);

CREATE TABLE questions (
    id          UUID PRIMARY KEY,
    questioner  VARCHAR(256) REFERENCES users(id),
    summary       VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL
);

CREATE TABLE answers (
    id          UUID PRIMARY KEY,
    question    UUID REFERENCES questions(id),
    answerer    VARCHAR(256) REFERENCES users(id),
    text        TEXT NOT NULL,
    answered_at TIMESTAMP
);
