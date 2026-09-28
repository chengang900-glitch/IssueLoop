ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

UPDATE users SET must_change_password = TRUE
WHERE username = 'admin@uhoo.cn'
  AND password_hash = '$2b$12$eZewZfZxhlHYeOpi1cVV3OhCALG.vyP9rnvLddPoVgSTw7hP.45zu';
