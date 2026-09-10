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