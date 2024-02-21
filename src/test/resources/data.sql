INSERT INTO users (username, password, role, first_name, last_name, enabled)
VALUES ('TestUserRoleAdmin', 'KxMPdnDmXTsUCr11YAYEjk7ncRrCihqB9z/Knp7c8bU=', 'ADMIN', 'Alex', 'Petrov', true);

INSERT INTO users (username, password, role, first_name, last_name, enabled)
VALUES ('TestUserRoleUser', 'KxMPdnDmXTsUCr11YAYEjk7ncRrCihqB9z/Knp7c8bU=', 'USER', 'John', 'Doe', true);

INSERT INTO users (username, password, role, first_name, last_name, enabled)
VALUES ('TestUserRoleModerator', 'KxMPdnDmXTsUCr11YAYEjk7ncRrCihqB9z/Knp7c8bU=', 'MODERATOR', 'Johny', 'Walker', true);


INSERT INTO file (location)
VALUES ('testFile1.txt');

INSERT INTO file (location)
VALUES ('testFile2.txt');

INSERT INTO file (location)
VALUES ('testFile3.txt');

INSERT INTO file (location)
VALUES ('testFile4.txt');


INSERT INTO event (user_id, file_id)
VALUES ('1', '1');

INSERT INTO event (user_id, file_id)
VALUES ('1', '2');

INSERT INTO event (user_id, file_id)
VALUES ('2', '3');

INSERT INTO event (user_id, file_id)
VALUES ('2', '4');
