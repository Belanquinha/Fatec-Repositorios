#!/usr/bin/env python3
"""Gera back-end/src/main/resources/seeds/instituicoes.csv (UTF-8 BOM, ';').

Fonte oficial: exportacao_cursos_etec_22_09_2026.csv (raiz do repositório).
Regras determinísticas (AD-CI-1/AD-CI-2 do ARCHITECTURE-CATALOGO-INSTITUICOES):
  - Seleciona apenas unidades em operação (exclui '2027' no nome).
  - colapso de espacos em nome/endereco/cidade/regiaoAdministrativa.
  - telefone: extrai so os telefones, remove o sufixo 'Discagem Abreviada'.
  - estado fixo 'SP'; ativo=true aplicado pelo seeder.

Uso (da raiz): python3 back-end/init/generate_instituicoes_seed.py
"""
import csv
import io
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SOURCE = REPO_ROOT / "exportacao_cursos_etec_22_09_2026.csv"
OUTPUT = REPO_ROOT / "back-end" / "src" / "main" / "resources" / "seeds" / "instituicoes.csv"

HEADER = [
    "codigoUnidade",
    "nome",
    "endereco",
    "cidade",
    "estado",
    "regiaoAdministrativa",
    "cnpj",
    "telefone",
    "site",
    "linkLogo",
]


def collapse(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip() if value else ""


def build_rows():
    with open(SOURCE, "r", encoding="utf-8") as f:
        content = f.read()
    reader = csv.DictReader(io.StringIO(content), delimiter=";")
    rows = [{k.lstrip("\ufeff").strip(): v for k, v in r.items()} for r in reader]

    operational = [r for r in rows if "2027" not in r["Unidade"]]

    codigos = [collapse(r["Cód. da Unidade"]) for r in operational]
    assert len(operational) == 86, f"esperado 86 unidades, obtido {len(operational)}"
    assert len(set(codigos)) == len(codigos), "codigos duplicados apos o corte"
    assert all(codigos), "codigo vazio apos o corte"

    out = []
    for r in operational:
        telefone = collapse(r["Telefone"])
        telefone = telefone.split("Discagem Abreviada")[0].strip()
        out.append(
            {
                "codigoUnidade": collapse(r["Cód. da Unidade"]),
                "nome": collapse(r["Unidade"]),
                "endereco": collapse(r["Endereço"]),
                "cidade": collapse(r["Município"]),
                "estado": "SP",
                "regiaoAdministrativa": collapse(r["Região Administrativa"]),
                "cnpj": collapse(r["CNPJ"]),
                "telefone": telefone,
                "site": collapse(r["Site"]),
                "linkLogo": collapse(r["Logotipo"]),
            }
        )
    return out


def main() -> None:
    if not SOURCE.exists():
        sys.exit(f"Fonte nao encontrada: {SOURCE}")
    out = build_rows()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=HEADER, delimiter=";", lineterminator="\n")
        writer.writeheader()
        writer.writerows(out)
    print(f"{len(out)} linhas escritas em {OUTPUT.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()