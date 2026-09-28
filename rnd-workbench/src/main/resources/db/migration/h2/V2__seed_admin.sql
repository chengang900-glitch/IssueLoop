MERGE INTO users (username, password_hash, nickname, system_role, status)
KEY (username)
VALUES ('admin@uhoo.cn', '$2b$12$eZewZfZxhlHYeOpi1cVV3OhCALG.vyP9rnvLddPoVgSTw7hP.45zu', '管理员', 'ADMIN', 1);
