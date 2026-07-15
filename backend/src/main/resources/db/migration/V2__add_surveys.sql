CREATE TABLE survey (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL
);

INSERT INTO survey (id, title, owner_id)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Legacy questions',
    '00000000-0000-0000-0000-000000000000'
);

ALTER TABLE question ADD COLUMN survey_id UUID;

UPDATE question
SET survey_id = '00000000-0000-0000-0000-000000000001'
WHERE survey_id IS NULL;

ALTER TABLE question ALTER COLUMN survey_id SET NOT NULL;

ALTER TABLE question
    ADD CONSTRAINT fk_question_survey
        FOREIGN KEY (survey_id) REFERENCES survey (id);

CREATE INDEX idx_question_survey_id ON question (survey_id);
CREATE INDEX idx_survey_owner_id ON survey (owner_id);
