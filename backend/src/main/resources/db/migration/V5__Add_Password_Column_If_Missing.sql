-- ============================================================
-- V5__Add_Password_Column_If_Missing.sql
-- Adiciona a coluna password caso não tenha sido criada na V3
-- na base de produção (por conta do IF NOT EXISTS ter pulado
-- a criação da tabela).
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
