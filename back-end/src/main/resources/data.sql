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
    '$2a$06$QyZr6g4Oypu2eoykzY64duBusA.7KHYhWSYY0olSp/2Lfc89eaDAa',
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