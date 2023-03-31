INSERT INTO users VALUES
    ('tom.hanks'),
    ('marie.curie');

INSERT INTO questions VALUES
    (gen_random_uuid(), 'tom.hanks', 'What does the fox say?', 'You heard me!'),
    (gen_random_uuid(), 'marie.curie', 'How does the horse move?', 'If Magnus does not know how are we supposed to?');
