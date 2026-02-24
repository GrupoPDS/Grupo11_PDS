-- ============================================================
-- V3__Create_Users_Table.sql
-- A tabela users já existe na V1 (com phone, active).
-- Esta migração apenas adiciona a coluna password.
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
