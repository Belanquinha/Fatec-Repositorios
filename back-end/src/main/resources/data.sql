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

-- Contas de teste semeadas (sempre presentes, como o catálogo de instituições).
-- Servem para testar os fluxos de aluno e professor sem depender de contas reais
-- no Microsoft Entra ID. O papel é fixo aqui (não derivado do domínio), igual ao
-- admin acima. O login real via MSAL com esses e-mails reutiliza as mesmas linhas.
INSERT INTO usuarios (
    id,
    nome,
    email,
    role,
    criado_em,
    atualizado_em
)
VALUES
(
    gen_random_uuid(),
    'Aluno Teste',
    'aluno.teste@aluno.cps.sp.gov.br',
    'ALUNO',
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'Professor Teste',
    'professor.teste@cps.sp.gov.br',
    'PROFESSOR',
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;