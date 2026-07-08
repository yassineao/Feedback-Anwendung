CREATE TABLE IF NOT EXISTS question (
    id UUID PRIMARY KEY,
    question VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS answer (
    id UUID PRIMARY KEY,
    answer VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    question_id UUID NOT NULL,
    CONSTRAINT fk_answer_question
        FOREIGN KEY (question_id) REFERENCES question (id)
);

CREATE INDEX IF NOT EXISTS idx_answer_question_id ON answer (question_id);
CREATE INDEX IF NOT EXISTS idx_answer_user_id ON answer (user_id);
