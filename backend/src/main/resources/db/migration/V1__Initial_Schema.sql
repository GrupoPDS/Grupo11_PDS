-- ============================================================
-- V1__Initial_Schema.sql
-- Criação das tabelas iniciais: books, users, loans
-- Flyway executará este script automaticamente na inicialização
-- ============================================================

-- ========================
-- Tabela: books (Livros)
-- ========================
CREATE TABLE IF NOT EXISTS books (
    id          BIGSERIAL       PRIMARY KEY,
    title       VARCHAR(255)    NOT NULL,
    author      VARCHAR(255)    NOT NULL,
    isbn        VARCHAR(20)     NOT NULL UNIQUE,
    publisher   VARCHAR(255),
    year        INTEGER,
    quantity    INTEGER         NOT NULL DEFAULT 1,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================
-- Tabela: users (Usuários)
-- ========================
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    phone       VARCHAR(20),
    role        VARCHAR(50)     NOT NULL DEFAULT 'STUDENT',
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================
-- Tabela: loans (Empréstimos)
-- ========================
CREATE TABLE IF NOT EXISTS loans (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    book_id         BIGINT          NOT NULL REFERENCES books(id),
    loan_date       DATE            NOT NULL DEFAULT CURRENT_DATE,
    due_date        DATE            NOT NULL,
    return_date     DATE,
    status          VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_loans_user_id ON loans(user_id);
CREATE INDEX IF NOT EXISTS idx_loans_book_id ON loans(book_id);
CREATE INDEX IF NOT EXISTS idx_loans_status  ON loans(status);
CREATE INDEX IF NOT EXISTS idx_books_isbn    ON books(isbn);
CREATE INDEX IF NOT EXISTS idx_users_email   ON users(email);
