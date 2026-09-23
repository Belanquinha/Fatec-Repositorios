# Seed do catálogo oficial de instituições (epic-2, story 2-1)

Data: 2026-09-22 — bmad-build — AD-CI-1/2/3 (`ARCHITECTURE-CATALOGO-INSTITUICOES.md`) + SPEC §"Seed do catálogo de instituições" (AC-1..AC-6).

## O que foi entregue

| Artefato | Descrição |
| --- | --- |
| `back-end/init/generate_instituicoes_seed.py` | Gerador determinístico do seed a partir de `exportacao_cursos_etec_22_09_2026.csv` (regras AD-CI-1/AD-CI-2). Reexecutável quando o CPS publicar novo export. |
| `back-end/src/main/resources/seeds/instituicoes.csv` | 86 unidades em operação; UTF-8 BOM, `;`, cabeçalho fixo: `codigoUnidade;nome;endereco;cidade;estado;regiaoAdministrativa;cnpj;telefone;site;linkLogo`. |
| `model/Instituicao.java` | + `codigoUnidade` (`unique`/`not null`, chave de negócio), `regiaoAdministrativa`, `cnpj`, `telefone`, `site`, `linkLogo`, `ativo` (`not null`, default `true`). |
| `DTOs` (+ResponseMapper) | `InstituicaoRequest`/`InstituicaoResponse` espelham os novos campos; Request exige `codigoUnidade` e `nome` (`@NotBlank`). |
| `InstituicaoRepository` | + `findByCodigoUnidade`, `existsByCodigoUnidade`. |
| `seed/SeedProperties` | `faterepo.seed.mode` + `faterepo.seed.files` (`@ConfigurationProperties`). |
| `seed/InstituicaoCsvParser` | Parsing commons-csv (BOM/UTF-8), reject de linha com código vazio/duplicado sem abortar boot. |
| `seed/InstituicaoSeedLoader` | `upsert` (inserção se ausente — preserva edições admin) e `replace` (DELETE+INSERT no mesmo transaction; FK falha → fail-fast). |
| `seed/CatalogoSeeder` | `CommandLineRunner` único, ordem declarada dos arquivos (FK-safe p/ futuro `projetos.csv`). |
| `application.yml` | `faterepo.seed.mode: ${FATEREPO_SEED_MODE:upsert}` + lista `seeds/instituicoes.csv`. |
| `pom.xml` | + `org.apache.commons:commons-csv:1.12.0`. |
| `schema.sql` | Fix pré-existente: script só-comentário é rejeitado pelo Spring (boot quebrava em banco zerado); + `SELECT 1;` no-op. |
| Testes | `InstituicaoCsvParserTest` (3): BOM/aspas, rejeição de inválidas, integridade das 86 linhas versionadas. |

Sem endpoint de importação em runtime; `SecurityConfig` inalterado.

## Correção factual nos artefatos de planejamento

A fonte oficial tem **1 telefone vazio** (Fatec Votorantim — contém só "Discagem Abreviada"), não 0 como o AD/SPEC afirmavam. Telefone é opcional; 2 sites vazios permanecem. Métricas corrigidas no AD e no SPEC.

## Verificação (runtime — Postgres temporário zerado)

| AC | Resultado |
| --- | --- |
| AC-1 | Boot zerado upsert → 86 `Instituicao`, 86 códigos únicos, 0 nulos, `ativo=true` em todas, `estado='SP'`; telefone vazio 1, site vazio 2 (0 inválidas no parse). |
| AC-2 | Re-boot upsert → "0 novas inseridas, 86 mantidas" (nenhuma duplicada); edição de admin em `site` (291) preservada. |
| AC-3 | `replace` → recarrega 86 a partir da fonte (edição admin sobrescrita — comportamento de resync). Parte FK (fail-fast com `Projeto` referenciando) **não testável**: entidade `Projeto` ainda não existe. |
| AC-4 | Linhas inválidas rejeitadas com log, boot continua (coberto por teste unitário). |
| AC-5 | `findByCodigoUnidade` validada pelo boot (derivação de query JPA válida); pronta p/ future seed de projetos. |
| AC-6 | `GET /instituicoes` sem auth → 200 com 86 itens e novos campos; `POST/PUT/DELETE` sem token → 401 (SecurityConfig inalterado). |

## Caveats / open

- `ddl-auto: update` adiciona `codigo_unidade NOT NULL` sem default: em banco **não zerado** com linhas antigas na tabela, o alter pode falhar. Fluxo recomendado p/ rebuild: `docker compose down -v` (conforme AD-CI-3). O seed em si não quebra boot por sujeira de fonte (linha inválida é logada e ignorada).
- Seed não é executado em modo de teste (sem `@SpringBootTest` / H2 / Testcontainers no projeto); CRUD de admin com duplicidade de `codigoUnidade` fica coberto por teste quando a pilha de testes com banco existir.
- Unidades 2027: fora do CSV; `ativo=false` reservado apenas se houver pré-cadastro futuro.
- Manutenção do CSV versionado: reexecutar `back-end/init/generate_instituicoes_seed.py` ao publicar novo export (recomenda-se documentar junto a `resources/seeds/README` quando o fluxo de manutenção for formalizado).