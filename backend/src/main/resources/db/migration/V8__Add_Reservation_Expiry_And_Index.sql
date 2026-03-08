-- Adicionar campos faltantes à tabela reservations (expires_at, created_at, updated_at)
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP NULL;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Índice parcial: impede que o mesmo usuário reserve o mesmo livro 2 vezes ativamente
CREATE UNIQUE INDEX IF NOT EXISTS idx_active_user_book_reservation
    ON reservations(user_id, book_id)
    WHERE status IN ('PENDING', 'AVAILABLE_FOR_PICKUP');
