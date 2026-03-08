-- ============================================================
-- V10__Create_Book_Copies_Table.sql
-- Rastreamento individual de exemplares com código hash único
-- Cada cópia física de um livro recebe um identificador próprio
-- ============================================================

-- ========================
-- Tabela: book_copies (Exemplares)
-- ========================
CREATE TABLE IF NOT EXISTS book_copies (
    id          BIGSERIAL       PRIMARY KEY,
    book_id     BIGINT          NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    copy_code   VARCHAR(20)     NOT NULL UNIQUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================
-- Gerar exemplares para livros já existentes
-- copy_code = 8 primeiros chars hex do MD5(bookId + copyNumber)
-- ========================
INSERT INTO book_copies (book_id, copy_code, created_at)
SELECT b.id,
       UPPER(SUBSTR(MD5(b.id::text || '-' || s.n::text), 1, 8)),
       CURRENT_TIMESTAMP
FROM books b
CROSS JOIN LATERAL generate_series(1, b.quantity) AS s(n);

-- ========================
-- Adicionar referência ao exemplar na tabela de empréstimos
-- Nullable para manter compatibilidade com empréstimos antigos
-- ========================
ALTER TABLE loans ADD COLUMN book_copy_id BIGINT REFERENCES book_copies(id);

-- ========================
-- Atribuir exemplares aos empréstimos ACTIVE e OVERDUE existentes
-- Cada empréstimo recebe uma cópia distinta do mesmo livro
-- ========================
WITH ranked_loans AS (
    SELECT l.id AS loan_id, l.book_id,
           ROW_NUMBER() OVER (PARTITION BY l.book_id ORDER BY l.id) AS rn
    FROM loans l
    WHERE l.status IN ('ACTIVE', 'OVERDUE')
),
ranked_copies AS (
    SELECT bc.id AS copy_id, bc.book_id,
           ROW_NUMBER() OVER (PARTITION BY bc.book_id ORDER BY bc.id) AS rn
    FROM book_copies bc
)
UPDATE loans
SET book_copy_id = rc.copy_id
FROM ranked_loans rl
JOIN ranked_copies rc ON rl.book_id = rc.book_id AND rl.rn = rc.rn
WHERE loans.id = rl.loan_id;

-- ========================
-- Índices para performance
-- ========================
CREATE INDEX IF NOT EXISTS idx_book_copies_book_id ON book_copies(book_id);
CREATE INDEX IF NOT EXISTS idx_book_copies_copy_code ON book_copies(copy_code);
CREATE INDEX IF NOT EXISTS idx_loans_book_copy_id ON loans(book_copy_id);
