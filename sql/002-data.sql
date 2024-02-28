INSERT INTO users VALUES
  ('tom.hanks', 'http://localhost:5000/images/cat-avatar-2.jpg'),
  ('marie.curie', 'http://localhost:5000/images/cat-avatar-1.jpg'),
  ('anders.antonsen', 'http://localhost:5000/images/cat-avatar-3.jpg'),
  ('salvador.dali', 'http://localhost:5000/images/cat-avatar-4.jpg');

INSERT INTO questions VALUES
  (
    '3e3788e0-e2fd-4b97-a15e-9d7f1a9b4f4e',
    'tom.hanks',
    'What does the fox say?',
    'You heard me!'
  ),
  (
    '6a125f10-9a27-4760-9a19-4f5455aef13e',
    'marie.curie',
    'How does the horse move?',
    'If Magnus does not know how are we supposed to?'
  );

INSERT INTO answers VALUES
  (
    '97e746fb-00f7-4f41-bce1-17b0c4c89b6b',
    '3e3788e0-e2fd-4b97-a15e-9d7f1a9b4f4e',
    'marie.curie',
    'Meow!',
    '2024-02-02 12:34:35'
  ),
  (
    'd5e39c3d-6627-4e67-a0f8-bb9c45c9c9ee',
    '6a125f10-9a27-4760-9a19-4f5455aef13e',
    'salvador.dali',
    'upwards, methinks',
    '2024-02-05 13:33:33'
  ),
  (
    'f1b5a27b-89d1-4b2c-84f7-89d02e732024',
    '6a125f10-9a27-4760-9a19-4f5455aef13e',
    'anders.antonsen',
    'no, forwards!!!',
    '2024-02-06 13:37:37'
  );
