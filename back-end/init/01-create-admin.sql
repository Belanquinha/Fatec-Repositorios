CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Admin do sistema: identificado pelo e-mail institucional (MSAL, sem senha).
-- IMPORTANTE: substitua por um e-mail real @cps.sp.gov.br do dono do projeto.
INSERT INTO usuarios (
    id,
    nome,
    email,
    role,
    criado_em,
    atualizado_em
)
VALUES (
    gen_random_uuid(),
    'Administrador',
    'admin@cps.sp.gov.br',
    'ADMIN',
    NOW(),
    NOW()
)
ON CONFLICT (email) DO UPDATE
SET
    nome = EXCLUDED.nome,
    role = EXCLUDED.role,
    atualizado_em = NOW();