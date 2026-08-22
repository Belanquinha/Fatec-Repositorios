# Fatec-Repositorios

CREATE DATABASE fatecrepository
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'Portuguese_Brazil.1252'
    LC_CTYPE = 'Portuguese_Brazil.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO usuarios (
    id,
    nome,
    email,
    senha,
    role,
    instituicao_id,
    criado_em,
    atualizado_em
)
VALUES (
    gen_random_uuid(),
    'Administrador',
    'admin@fatec.com',
    '$2b$10$Nf4yvyuawp07fbGGtWGPwupPEBsCN8bs53Nj0qqLBcOXwmkrugUs6',
    'ADMIN',
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO UPDATE
SET
    nome = EXCLUDED.nome,
    senha = EXCLUDED.senha,
    role = EXCLUDED.role,
    atualizado_em = NOW();

