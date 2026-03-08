-- ============================================================
-- V9__Seed_Dev_Data.sql
-- Dados de desenvolvimento para teste local
-- Senha de TODOS os usuários: senha123
-- ============================================================

-- ========================
-- Usuários de teste
-- Senha: senha123 (BCrypt strength=12, prefix $2a$)
-- ========================
INSERT INTO users (name, email, password, role, active) VALUES
    ('Administrador do Sistema', 'admin@biblioteca.com',
     '$2a$12$CVmViHLUKuNoLPEVOhHzZOV9fqHJgH9mqgIhceddJJmANuwlTcVN.', 'ADMIN', true),
    ('Carlos Bibliotecário', 'bibliotecario@biblioteca.com',
     '$2a$12$CVmViHLUKuNoLPEVOhHzZOV9fqHJgH9mqgIhceddJJmANuwlTcVN.', 'LIBRARIAN', true),
    ('Ana Estudante', 'ana@aluno.ufu.br',
     '$2a$12$CVmViHLUKuNoLPEVOhHzZOV9fqHJgH9mqgIhceddJJmANuwlTcVN.', 'STUDENT', true),
    ('Pedro Estudante', 'pedro@aluno.ufu.br',
     '$2a$12$CVmViHLUKuNoLPEVOhHzZOV9fqHJgH9mqgIhceddJJmANuwlTcVN.', 'STUDENT', true)
ON CONFLICT (email) DO NOTHING;

-- ========================
-- Livros: acervo variado
-- ========================
INSERT INTO books (title, author, isbn, category, publisher, year, quantity) VALUES
    -- Tecnologia (5 livros)
    ('Engenharia de Software Moderna', 'Marco Tulio Valente', '978-6500019506', 'Tecnologia', 'Independente', 2020, 3),
    ('Clean Code', 'Robert C. Martin', '978-0132350884', 'Tecnologia', 'Prentice Hall', 2008, 2),
    ('Design Patterns', 'Erich Gamma', '978-0201633610', 'Tecnologia', 'Addison-Wesley', 1994, 2),
    ('Estruturas de Dados e Algoritmos com Java', 'Robert Lafore', '978-8536502205', 'Tecnologia', 'Ciência Moderna', 2004, 1),
    ('Redes de Computadores', 'Andrew Tanenbaum', '978-8582604694', 'Tecnologia', 'Bookman', 2021, 2),

    -- Ficção (4 livros)
    ('Dom Casmurro', 'Machado de Assis', '978-8520923153', 'Ficção', 'Ática', 1899, 4),
    ('1984', 'George Orwell', '978-8535914849', 'Ficção', 'Companhia das Letras', 1949, 3),
    ('O Senhor dos Anéis', 'J.R.R. Tolkien', '978-8595084759', 'Ficção', 'HarperCollins', 1954, 2),
    ('Cem Anos de Solidão', 'Gabriel García Márquez', '978-8501012173', 'Ficção', 'Record', 1967, 1),

    -- Educação (4 livros)
    ('Pedagogia do Oprimido', 'Paulo Freire', '978-8577531646', 'Educação', 'Paz e Terra', 1968, 3),
    ('Didática', 'José Carlos Libâneo', '978-8524917264', 'Educação', 'Cortez', 1990, 2),
    ('Psicologia da Educação', 'César Coll', '978-8536302085', 'Educação', 'Artmed', 2004, 1),
    ('Escola e Democracia', 'Dermeval Saviani', '978-8574962085', 'Educação', 'Autores Associados', 1983, 2),

    -- História (3 livros)
    ('Brasil: Uma Biografia', 'Lilia Schwarcz', '978-8535925890', 'História', 'Companhia das Letras', 2015, 2),
    ('Sapiens: Uma Breve História da Humanidade', 'Yuval Noah Harari', '978-8525432186', 'História', 'L&PM', 2014, 3),
    ('A Era dos Extremos', 'Eric Hobsbawm', '978-8571644687', 'História', 'Companhia das Letras', 1994, 1)
ON CONFLICT (isbn) DO NOTHING;

-- ========================
-- Empréstimos de teste
-- Criamos cenários variados para testar TODAS as telas:
--   • Ativos dentro do prazo
--   • Atrasados (vários graus de severidade)
--   • Devolvidos (histórico)
-- ========================

-- IDs auxiliares (subselect)
-- Ana (STUDENT)  → ana@aluno.ufu.br
-- Pedro (STUDENT) → pedro@aluno.ufu.br

-- ─── Empréstimos ATIVOS (dentro do prazo) ───
INSERT INTO loans (user_id, book_id, loan_date, due_date, return_date, status) VALUES
    -- Ana: pegou "Clean Code" há 3 dias, prazo de 14 dias
    ((SELECT id FROM users WHERE email='ana@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-0132350884'),
     CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '11 days', NULL, 'ACTIVE'),

    -- Ana: pegou "Dom Casmurro" há 1 dia
    ((SELECT id FROM users WHERE email='ana@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8520923153'),
     CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE + INTERVAL '13 days', NULL, 'ACTIVE'),

    -- Pedro: pegou "1984" há 5 dias
    ((SELECT id FROM users WHERE email='pedro@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8535914849'),
     CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '9 days', NULL, 'ACTIVE');

-- ─── Empréstimos ATRASADOS (vários graus de severidade) ───
-- Status = OVERDUE (já marcados pelo scheduler em prod; aqui inserimos diretamente)
INSERT INTO loans (user_id, book_id, loan_date, due_date, return_date, status) VALUES
    -- Ana: "Design Patterns" → 5 dias de atraso (LOW)
    ((SELECT id FROM users WHERE email='ana@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-0201633610'),
     CURRENT_DATE - INTERVAL '19 days', CURRENT_DATE - INTERVAL '5 days', NULL, 'OVERDUE'),

    -- Pedro: "Estruturas de Dados" → 12 dias de atraso (MEDIUM)
    ((SELECT id FROM users WHERE email='pedro@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8536502205'),
     CURRENT_DATE - INTERVAL '26 days', CURRENT_DATE - INTERVAL '12 days', NULL, 'OVERDUE'),

    -- Ana: "Pedagogia do Oprimido" → 22 dias de atraso (HIGH)
    ((SELECT id FROM users WHERE email='ana@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8577531646'),
     CURRENT_DATE - INTERVAL '36 days', CURRENT_DATE - INTERVAL '22 days', NULL, 'OVERDUE'),

    -- Pedro: "Redes de Computadores" → 35 dias de atraso (CRITICAL)
    ((SELECT id FROM users WHERE email='pedro@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8582604694'),
     CURRENT_DATE - INTERVAL '49 days', CURRENT_DATE - INTERVAL '35 days', NULL, 'OVERDUE'),

    -- Pedro: "Sapiens" → 8 dias de atraso (MEDIUM)
    ((SELECT id FROM users WHERE email='pedro@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8525432186'),
     CURRENT_DATE - INTERVAL '22 days', CURRENT_DATE - INTERVAL '8 days', NULL, 'OVERDUE');

-- ─── Empréstimos DEVOLVIDOS (histórico) ───
INSERT INTO loans (user_id, book_id, loan_date, due_date, return_date, status) VALUES
    -- Ana: devolveu "Sapiens" no prazo, 30 dias atrás
    ((SELECT id FROM users WHERE email='ana@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8525432186'),
     CURRENT_DATE - INTERVAL '45 days', CURRENT_DATE - INTERVAL '31 days',
     CURRENT_DATE - INTERVAL '33 days', 'RETURNED'),

    -- Ana: devolveu "Brasil: Uma Biografia" no prazo
    ((SELECT id FROM users WHERE email='ana@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8535925890'),
     CURRENT_DATE - INTERVAL '60 days', CURRENT_DATE - INTERVAL '46 days',
     CURRENT_DATE - INTERVAL '50 days', 'RETURNED'),

    -- Pedro: devolveu "Escola e Democracia" com 3 dias de atraso
    ((SELECT id FROM users WHERE email='pedro@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8574962085'),
     CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE - INTERVAL '16 days',
     CURRENT_DATE - INTERVAL '13 days', 'RETURNED'),

    -- Pedro: devolveu "Dom Casmurro" no prazo
    ((SELECT id FROM users WHERE email='pedro@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8520923153'),
     CURRENT_DATE - INTERVAL '50 days', CURRENT_DATE - INTERVAL '36 days',
     CURRENT_DATE - INTERVAL '40 days', 'RETURNED'),

    -- Ana: devolveu "Didática" no prazo
    ((SELECT id FROM users WHERE email='ana@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8524917264'),
     CURRENT_DATE - INTERVAL '40 days', CURRENT_DATE - INTERVAL '26 days',
     CURRENT_DATE - INTERVAL '28 days', 'RETURNED');

-- ─── Reservas (fila de espera) ───
INSERT INTO reservations (user_id, book_id, reservation_date, status, expires_at) VALUES
    -- Ana reservou "Cem Anos de Solidão" (só 1 cópia, pode estar emprestado)
    ((SELECT id FROM users WHERE email='ana@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8501012173'),
     CURRENT_DATE - INTERVAL '2 days', 'PENDING', NULL),

    -- Pedro reservou "Psicologia da Educação" (1 cópia)
    ((SELECT id FROM users WHERE email='pedro@aluno.ufu.br'),
     (SELECT id FROM books WHERE isbn='978-8536302085'),
     CURRENT_DATE - INTERVAL '1 day', 'PENDING', NULL);
