CREATE TABLE "user" (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL
);

ALTER TABLE "user"
    ADD CONSTRAINT uk_user_email UNIQUE (email);
