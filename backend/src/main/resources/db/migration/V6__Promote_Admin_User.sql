-- ============================================================
-- V6__Promote_Admin_User.sql
-- Promove o usuário específico para o perfil ADMIN
-- para acesso em produção.
-- ============================================================

UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'pedroolucaasms30@gmail.com';
