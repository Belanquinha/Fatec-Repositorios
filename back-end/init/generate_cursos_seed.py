#!/usr/bin/env python3
"""Gera back-end/src/main/resources/seeds/cursos_instituicoes.csv (UTF-8 BOM, ';').

Fonte oficial: exportacao_cursos_etec_17_11_2026.csv (raiz do repositório).
Regras determinísticas:
  - Seleciona apenas as 86 unidades em operação (exclui '2027' no nome).
  - Segmenta a coluna 'Cursos' (que vem concatenada sem separadores no export do CPS)
    contra o catálogo canônico de Cursos Superiores de Tecnologia (CST) das Fatecs,
    exigindo cobertura de 100% do texto (sobra zero) e segmentação única.
  - Unifica modalidades Presencial e EaD na mesma unidade pelo nome canônico do curso.

Uso (da raiz): python back-end/init/generate_cursos_seed.py
"""
import csv
import io
import re
import sys
from functools import lru_cache
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
SOURCE = REPO_ROOT / "exportacao_cursos_etec_17_11_2026.csv"
INSTITUICOES_SEED = REPO_ROOT / "back-end" / "src" / "main" / "resources" / "seeds" / "instituicoes.csv"
OUTPUT = REPO_ROOT / "back-end" / "src" / "main" / "resources" / "seeds" / "cursos_instituicoes.csv"

HEADER = ["codigoUnidade", "nomeCurso"]

CURSOS_CANONICOS: dict[str, str] = {
    c: c
    for c in [
        "Agronegócio",
        "Agroindústria",
        "Alimentos",
        "Análise de Processos Agroindustriais",
        "Análise e Desenvolvimento de Sistemas",
        "Automação Industrial",
        "Banco de Dados",
        "Biocombustíveis",
        "Big Data no Agronegócio",
        "Big Data para Indústria",
        "Big Data para Negócios",
        "Ciência de Dados",
        "Ciência de Dados para Negócios",
        "Ciência de Dados para o Agronegócio",
        "Comércio Exterior",
        "Construção Civil – Edifícios",
        "Construção de Edifícios",
        "Construção Naval",
        "Controle de Obras",
        "Cosméticos",
        "Defesa Cibernética",
        "Desenvolvimento de Software Multiplataforma",
        "Design de Mídias Digitais",
        "Design de Moda",
        "Design de Produto com ênfase em Processos de Produção e Industrialização",
        "Design Gráfico",
        "Eletrônica Automotiva",
        "Eletrônica Industrial",
        "Engenharia Civil",
        "Engenharia de Produção",
        "Engenharia Mecânica",
        "Estradas",
        "Fabricação Mecânica",
        "Gastronomia",
        "Geoprocessamento",
        "Gestão Ambiental",
        "Gestão Comercial",
        "Gestão da Produção Industrial",
        "Gestão da Qualidade",
        "Gestão da Sustentabilidade Ambiental, Social e Governança Corporativa",
        "Gestão da Tecnologia da Informação",
        "Gestão de Comércio Eletrônico",
        "Gestão de Empreendimentos Gastronômicos",
        "Gestão de Energia e Eficiência Energética",
        "Gestão de Eventos",
        "Gestão de Logística Integrada",
        "Gestão de Negócios e Inovação",
        "Gestão de Negócios e Pessoas",
        "Gestão de Recursos Humanos",
        "Gestão de Serviços",
        "Gestão de Turismo",
        "Gestão Desportiva e de Lazer",
        "Gestão Empresarial",
        "Gestão Financeira",
        "Gestão Hospitalar",
        "Gestão Portuária",
        "Gestão Pública",
        "Hidráulica e Saneamento Ambiental",
        "Informática para Negócios",
        "Instalações Elétricas",
        "Inteligência Artificial",
        "Jogos Digitais",
        "Logística",
        "Logística Aeroportuária",
        "Manufatura Avançada",
        "Manutenção de Aeronaves",
        "Manutenção Industrial",
        "Marketing",
        "Materiais",
        "Mecânica Automobilística",
        "Mecânica de Precisão",
        "Mecânica: Processos de Soldagem",
        "Mecanização em Agricultura de Precisão",
        "Mecatrônica Industrial",
        "Meio Ambiente e Recursos Hídricos",
        "Microeletrônica",
        "Paisagismo e Jardinagem",
        "Polímeros",
        "Processos Metalúrgicos",
        "Processos Químicos",
        "Produção Agropecuária",
        "Produção Cultural",
        "Produção Fonográfica",
        "Produção Industrial",
        "Produção Têxtil",
        "Projetos de Estruturas Aeronáuticas",
        "Projetos Mecânicos",
        "Radiologia",
        "Redes de Computadores",
        "Refrigeração e Climatização",
        "Refrigeração, Ventilação e Ar Condicionado",
        "Secretariado",
        "Secretariado e Assessoria Internacional",
        "Segurança da Informação",
        "Silvicultura",
        "Sistemas Biomédicos",
        "Sistemas Embarcados",
        "Sistemas Inteligentes",
        "Sistemas Navais",
        "Sistemas para Internet",
        "Soldagem",
        "Transporte Terrestre",
    ]
}
# Normalização de capitalização inconsistente na exportação CPS (unidade 269)
CURSOS_CANONICOS["Big Data Para Indústria"] = "Big Data para Indústria"

SECTION_HEADERS = (
    "Cursos Superiores de Tecnologia Presenciais",
    "Cursos Superiores de Tecnologia EaD",
)


def collapse(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip() if value else ""


def segment_block(block: str, codigo: str) -> list[str]:
    block = collapse(block)
    if not block:
        return []

    tokens = sorted(CURSOS_CANONICOS.keys(), key=len, reverse=True)

    @lru_cache(maxsize=None)
    def solve(pos: int) -> list[tuple[str, ...]]:
        while pos < len(block) and block[pos] == " ":
            pos += 1
        if pos == len(block):
            return [()]
        solutions: list[tuple[str, ...]] = []
        for raw_name in tokens:
            if block.startswith(raw_name, pos):
                canonical = CURSOS_CANONICOS[raw_name]
                for tail in solve(pos + len(raw_name)):
                    solutions.append((canonical,) + tail)
        return solutions

    all_solutions = solve(0)
    if not all_solutions:
        raise ValueError(f"Não foi possível segmentar cursos da unidade {codigo}: {block!r}")
    if len(all_solutions) > 1:
        raise ValueError(f"Segmentação ambígua na unidade {codigo}: {all_solutions}")
    return list(all_solutions[0])


def parse_cursos_cell(raw_cursos: str, codigo: str) -> list[str]:
    text = collapse(raw_cursos)
    for header in SECTION_HEADERS:
        text = text.replace(header, "\n")

    encontrados: list[str] = []
    vistos: set[str] = set()
    for part in text.split("\n"):
        for curso in segment_block(part, codigo):
            if curso not in vistos:
                vistos.add(curso)
                encontrados.append(curso)

    return sorted(encontrados)


def build_rows() -> list[dict[str, str]]:
    with open(SOURCE, "r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(io.StringIO(f.read()), delimiter=";")
        rows = [{k.lstrip("\ufeff").strip(): v for k, v in r.items()} for r in reader]

    operational = [r for r in rows if "2027" not in r["Unidade"]]
    assert len(operational) == 86, f"esperado 86 unidades, obtido {len(operational)}"

    if INSTITUICOES_SEED.exists():
        with open(INSTITUICOES_SEED, "r", encoding="utf-8-sig") as f_inst:
            codigos_seed = {
                r["codigoUnidade"].strip()
                for r in csv.DictReader(f_inst, delimiter=";")
            }
        codigos_fonte = {collapse(r["Cód. da Unidade"]) for r in operational}
        assert codigos_seed == codigos_fonte, "divergência entre códigos de instituicoes.csv e fonte de cursos"

    out: list[dict[str, str]] = []
    for r in sorted(operational, key=lambda item: collapse(item["Cód. da Unidade"])):
        codigo = collapse(r["Cód. da Unidade"])
        cursos = parse_cursos_cell(r["Cursos"], codigo)
        assert cursos, f"unidade {codigo} ficou sem cursos extraídos"
        for nome_curso in cursos:
            out.append({"codigoUnidade": codigo, "nomeCurso": nome_curso})

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
    cursos_unicos = len({r["nomeCurso"] for r in out})
    print(
        f"{len(out)} vínculos (em {cursos_unicos} cursos únicos e 86 unidades) "
        f"escritos em {OUTPUT.relative_to(REPO_ROOT)}"
    )


if __name__ == "__main__":
    main()
