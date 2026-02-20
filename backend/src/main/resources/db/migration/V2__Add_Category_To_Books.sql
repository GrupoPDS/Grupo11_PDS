-- ============================================================
-- V2__Add_Category_To_Books.sql
-- Adiciona o campo categoria à tabela books
-- ============================================================

ALTER TABLE books ADD COLUMN IF NOT EXISTS category VARCHAR(100);

-- Criar índice para melhorar consultas por categoria
CREATE INDEX IF NOT EXISTS idx_books_category ON books(category);
