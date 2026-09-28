-- V2__seed_admin.sql: 初始管理员账号
-- 密码: admin123 (BCrypt)
INSERT INTO users (username, password_hash, nickname, system_role, status)
VALUES ('admin@uhoo.cn', '$2b$12$eZewZfZxhlHYeOpi1cVV3OhCALG.vyP9rnvLddPoVgSTw7hP.45zu', '管理员', 'ADMIN', 1)
ON CONFLICT (username) DO NOTHING;
