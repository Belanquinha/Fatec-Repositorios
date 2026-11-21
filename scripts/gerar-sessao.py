#!/usr/bin/env python3
"""Emite uma sessão local válida para as contas de teste semeadas (sem MSAL).

Uso:
    python3 scripts/gerar-sessao.py aluno
    python3 scripts/gerar-sessao.py professor

O script monta um JWT com as mesmas claims que o
`JwtTokenProvider.generateToken` do back-end emite (iss, sub, userId, role,
iat, exp), assinado em HS256 com o `JWT_SECRET` do `.env` da raiz. Como a
autenticação do back-end resolve a identidade pelo `sub` (e-mail) e carrega
papel e id do banco (`JwtAuthenticationFilter` + `CustomUserDetailsService`),
o token equivale a um login real — o front-end não distingue a origem.

Pré-requisitos: stack no ar (`docker compose up -d`) com as contas semeadas
(`data.sql`). A saída é um snippet para colar no console do navegador
(DevTools) com a aplicação aberta.

Só existem dois alvos (`aluno` e `professor`), de propósito: é a allowlist das
contas de teste. Não emite token de admin nem de e-mail arbitrário.
Só stdlib — sem dependências.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
ENV_PATH = RAIZ / ".env"

CONTAS = {
    "aluno": {
        "nome": "Aluno Teste",
        "email": "aluno.teste@aluno.cps.sp.gov.br",
        "role": "ALUNO",
    },
    "professor": {
        "nome": "Professor Teste",
        "email": "professor.teste@cps.sp.gov.br",
        "role": "PROFESSOR",
    },
}

# Claim cosmética: nada no back-end autentica por ela (a identidade vem do
# banco via `sub`). Quando o container do Postgres está acessível, resolve o
# UUID real para o token ficar idêntico ao de um login Microsoft.
NIL_UUID = "00000000-0000-0000-0000-000000000000"


def ler_env(path: Path) -> dict[str, str]:
    valores: dict[str, str] = {}
    for linha in path.read_text(encoding="utf-8").splitlines():
        linha = linha.strip()
        if not linha or linha.startswith("#") or "=" not in linha:
            continue
        chave, _, valor = linha.partition("=")
        valores[chave.strip()] = valor.strip().strip("\"'")
    return valores


def base64url(dados: bytes) -> str:
    return base64.urlsafe_b64encode(dados).rstrip(b"=").decode("ascii")


def mintar_jwt(segredo: str, emissor: str, email: str, user_id: str, role: str, expiracao_horas: int) -> str:
    agora = int(time.time())
    header = base64url(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    payload = base64url(
        json.dumps(
            {
                "iss": emissor,
                "sub": email,
                "userId": user_id,
                "role": role,
                "iat": agora,
                "exp": agora + expiracao_horas * 3600,
            },
            separators=(",", ":"),
        ).encode()
    )
    assinatura = base64url(hmac.new(segredo.encode(), f"{header}.{payload}".encode(), hashlib.sha256).digest())
    return f"{header}.{payload}.{assinatura}"


def _decodificar(saida: bytes | str | None) -> str:
    if not saida:
        return ""
    return saida.decode("utf-8", "replace") if isinstance(saida, bytes) else saida


def _consulta_uuid_no_psql(psql: str, email: str, senha: str) -> str | None:
    env = dict(os.environ)
    env["PGPASSWORD"] = senha
    try:
        proc = subprocess.run(
            [
                psql,
                "-h", os.environ.get("PGHOST", "localhost"),
                "-p", os.environ.get("PGPORT", "5432"),
                "-U", os.environ.get("PGUSER", "postgres"),
                "-d", os.environ.get("PGDATABASE", "fatecrepository"),
                "-tAc",
                f"SELECT id FROM usuarios WHERE email='{email}'",
            ],
            capture_output=True,
            timeout=15,
            env=env,
        )
    except (OSError, subprocess.SubprocessError):
        return None
    if proc.returncode != 0:
        return None
    return _decodificar(proc.stdout).strip() or None


def _resolver_no_docker(email: str) -> tuple[str, bool]:
    try:
        proc = subprocess.run(
            [
                "docker",
                "exec",
                "fatec-postgres",
                "psql",
                "-U",
                "postgres",
                "-d",
                "fatecrepository",
                "-tAc",
                f"SELECT id FROM usuarios WHERE email='{email}'",
            ],
            capture_output=True,
            timeout=15,
        )
    except (OSError, subprocess.SubprocessError):
        return NIL_UUID, False
    user_id = _decodificar(proc.stdout).strip()
    return (user_id, True) if user_id else (NIL_UUID, False)


def _resolver_local(email: str, senhas: list[str]) -> tuple[str, bool]:
    # O `.env` pode trazer a senha do compose, então a do application.yml (123456)
    # também é tentada.
    candidatos = [os.environ.get("PSQL_PATH", ""), shutil.which("psql") or ""]
    candidatos.append(
        r"C:\Program Files\PostgreSQL\18\bin\psql.exe"
        if sys.platform.startswith("win")
        else "/usr/bin/psql"
    )
    for senha in dict.fromkeys(s for s in senhas if s):
        for candidato in candidatos:
            if not candidato:
                continue
            user_id = _consulta_uuid_no_psql(candidato, email, senha)
            if user_id:
                return user_id, True
    return NIL_UUID, False


def resolver_user_id(email: str, senhas: list[str], fonte: str = "auto") -> tuple[str, bool]:
    """Lê o UUID real da conta no Postgres que o back-end em uso está lendo.

    `auto`: se o container `fatec-postgres` do compose está no ar, ele é a fonte
    (stack Docker); senão, o Postgres local. `local`/`docker` forçam a fonte.
    """
    if fonte == "docker":
        return _resolver_no_docker(email)
    if fonte == "local":
        return _resolver_local(email, senhas)
    user_id, resolveu = _resolver_no_docker(email)
    if resolveu:
        return user_id, True
    return _resolver_local(email, senhas)


def main() -> int:
    parser = argparse.ArgumentParser(description="Emite sessão local para as contas de teste.")
    parser.add_argument("conta", choices=sorted(CONTAS), help="qual conta de teste simular")
    parser.add_argument(
        "--fonte",
        choices=["auto", "local", "docker"],
        default=os.environ.get("SESSAO_FONTE", "auto"),
        help="de onde ler o UUID da conta (default: auto — Docker se o compose estiver "
             "no ar, senão o Postgres local)",
    )
    args = parser.parse_args()

    if not ENV_PATH.exists():
        print(f"ERRO: {ENV_PATH} não encontrado. Copie .env.example para .env.", file=sys.stderr)
        return 1

    env = ler_env(ENV_PATH)
    segredo = env.get("JWT_SECRET", "")
    if len(segredo.encode()) < 32:
        print("ERRO: JWT_SECRET ausente ou com menos de 32 caracteres no .env.", file=sys.stderr)
        return 1
    emissor = env.get("JWT_ISSUER", "fatec-repository-api")
    try:
        expiracao_horas = int(env.get("JWT_EXPIRATION_HOURS", "24"))
    except ValueError:
        expiracao_horas = 24

    conta = CONTAS[args.conta]
    user_id, resolveu = resolver_user_id(
        conta["email"],
        [s for s in (env.get("POSTGRES_PASSWORD"), "123456") if s],
        args.fonte,
    )
    token = mintar_jwt(segredo, emissor, conta["email"], user_id, conta["role"], expiracao_horas)

    if not resolveu:
        print(
            f"AVISO: não foi possível ler o UUID da conta no Postgres (fonte: {args.fonte}). "
            "O claim `userId` saiu zerado e o front fica sem a ação Excluir — a autenticação "
            "não é afetada, pois o back-end resolve a identidade pelo e-mail.",
            file=sys.stderr,
        )

    snippet = (
        f"localStorage.setItem('accessToken','{token}');"
        f"localStorage.setItem('tokenType','Bearer');"
        f"localStorage.setItem('expiresInSeconds','{expiracao_horas * 3600}');"
        # Sem o `usuarioId` o front não sabe qual card é do usuário logado e a
        # ação Excluir (SPEC-exclusao-projetos) fica oculta. O UUID já foi
        # resolvido acima; quando a stack está fora do ar não há id para gravar.
        + (f"localStorage.setItem('usuarioId','{user_id}');" if resolveu else "")
        + f"localStorage.setItem('usuarioNome','{conta['nome']}');"
        f"localStorage.setItem('usuarioEmail','{conta['email']}');"
        f"localStorage.setItem('usuarioRole','{conta['role']}');"
        "location.reload();"
    )
    print(f"Sessão pronta: {conta['nome']} <{conta['email']}> (válida por {expiracao_horas}h).")
    print("Cole no console do navegador (DevTools) com a aplicação aberta:\n")
    print(snippet)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
