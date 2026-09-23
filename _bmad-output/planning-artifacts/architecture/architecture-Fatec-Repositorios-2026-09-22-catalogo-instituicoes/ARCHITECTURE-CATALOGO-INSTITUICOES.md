---
name: ARCHITECTURE-Fatec-Repositorios-Catalogo-Instituicoes
type: architecture-decision-record
purpose: build-substrate
altitude: feature
paradigm: layered (Controller-Service-Repository) REST API + seed idempotente no boot
scope: População do catálogo oficial de instituições Fatec/CPS a partir do export CSV oficial; preparação para seed de projetos relacionados
status: final
created: 2026-09-22
updated: 2026-09-22
binds: [CAP-3, FR-5, FR-6, FR-7, NFR-4, AD-5]
sources:
  - exportacao_cursos_etec_22_09_2026.csv
  - ../../specs/spec-fatec-repositorios/SPEC.md
  - ../../../planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - ../../../planning-artifacts/epics.md
  - ../../architecture/architecture-Fatec-Repositorios-2026-09-16/ARCHITECTURE-SPINE.md
companions: []
---

# Arquitetura — Catálogo Oficial de Instituições Fatec/CPS

## 1. Contexto e problema

O sistema precisa de um **catálogo oficial pré-populado** de instituições Fatec/CPS (CAP-3): o professor seleciona de 0 a 4 instituições, o aluno vincula o projeto a uma delas e o catálogo é **dado autoritativo mantido por admin** (FR-7, NFR-4, AD-5). Hoje:

- O modelo `Instituicao` guarda apenas `id`, `nome`, `endereco`, `cidade`, `estado` (`model/Instituicao.java`).
- O `SecurityConfig` já exige `hasRole("ADMIN")` para `POST/PUT/DELETE /instituicoes` e libera `GET` — **sem mudança de segurança necessária**.
- Não existe mecanismo de carga em lote; o SPEC declarava importação batch "pós-MVP" como non-goal.
- Existe uma fonte oficial: `exportacao_cursos_etec_22_09_2026.csv` (sem espaçamento no timestamp), com **95 linhas de unidades Fatec** (apesar do nome, não há ETEC no arquivo).

Decisão tomada (sessão 2026-09-22): usar esse CSV para **rebuild do catálogo no banco**, ignorando o que está atualmente armazenado, e preparar a mecânica para que **futuras cargas relacionadas (ex.: projeto CSV → instituição) funcionem no primeiro boot do docker**.

## 2. Análise da fonte e qualidade dos dados

| Métrica | Valor |
| --- | --- |
| Linhas no CSV | 95 |
| Colunas | 17 |
| Unidades em operação (sem "2027" no nome) | **86** |
| Unidades planejadas para 2027 (excluídas) | 9 |
| Códigos de unidade duplicados no total | 3 pares (309, 275, 204) — **todos resolvidos pela exclusão de 2027** |
| Códigos de unidade vazios no total | 6 — todos em unidades 2027 |
| Após exclusão: códigos únicos / vazios | **86 únicos / 0 vazios** |
| Após exclusão: CNPJ, endereço, município, região, logotipo vazios | 0 |
| Após exclusão: telefone vazio | 1 (Fatec Votorantim — só contém "Discagem Abreviada") |
| Após exclusão: site vazio | 2 |
| Endereços com CEP | 70/86 |

**Conclusão:** excluindo as 9 unidades de 2027, o restante está **limpo o suficiente para virar dado estruturado** com um passo de transformação/canonicalização determinística. A coluna `Cursos` é **inconcatenável** (ex.: `"PresenciaisCiência de DadosGestão Comercial"` — sem separador), logo **não é persistida**; cursos pertencem a um futuro modelo próprio.

## 3. Decisões de arquitetura

### AD-CI-1 — Instituição ganha atributos enriquecidos, estáveis e exploráveis; textos longos ficam fora

O modelo `Instituicao` passa a persistir o que o produto usa ou exibirá (catálogo, filtros, cartões, "admin-main") e **não** o que é narrativa da fonte:

| Campo CSV | Campo do modelo | Norma de persistência |
| --- | --- | --- |
| `Cód. da Unidade` | `codigoUnidade` | `UNIQUE NOT NULL`. **Chave de negócio** para idempotência do seed e para conferência (validações rejeitam linha sem código). |
| `Unidade` | `nome` | Normalizado (`trim`/colapso de espaços). Nome completo oficial (inclui denominação, ex.: "Fatec Araçatuba – Prof. Fernando Amaral de Almeida Prado"). |
| `Endereço` | `endereco` | Texto limpo (mantém CEP quando presente). |
| `Município` | `cidade` | Normalizado. |
| — | `estado` | Constante `"SP"` (todas as unidades são SP); mantém compatibilidade com modelo e UI. |
| `Região Administrativa` | `regiaoAdministrativa` | Normalizado. Habilita filtro por região (17 regiões). |
| `CNPJ` | `cnpj` | Normalizado, formato da fonte. |
| `Telefone` | `telefone` | **Extrair** apenas a parte de telefones (ex.: `(18) 3522-4181 / 3502-4500`); descartar o sufixo colado `Discagem Abreviada: (6) 291`. Vazio permitido (1 caso: Fatec Votorantim, sem números na fonte). |
| `Site` | `site` | URL validada; vazio permitido (2 casos). |
| `Logotipo` | `linkLogo` | URL externa apontada pela fonte (blob CPS); mantida como link, **não** baixada. |
| — | `ativo` | `true` para todas as semeadas. Reserva o caminho p/ ativar units 2027 no futuro sem re-modelo. |
| — | `criadoEm` / `atualizadoEm` | Padrão existente. |

**Não persistidos** (racional): `Histórico` (narrativa longa, sem uso), `Direção`/`Direção Administrativo`/`Direção Acadêmica` (PII + e-mails; 285 e-mails `@cps.sp.gov.br` — fora do escopo do MVP), `Aniversário da Cidade`, `Redes Sociais`, `Arquivos` (decretos/legislação), `Cursos` (coluna inconcatenável; futuro modelo próprio). Nada disso é consumido pelas telas atuais.

### AD-CI-2 — O seed cobre as 86 unidades operacionais; unidades 2027 ficam fora (com caminho de ativação)

- **Rule:** o contrato versionado (`instituicoes.csv` em `resources/`) contém **as 86 unidades em operação com código oficial**. As 9 unidades 2027 são excluídas na transformação (não existem; professor não deve selecioná-las).
- **Por quê:** exclusão elimina simultaneamente os 3 pares de código duplicado e os 6 códigos vazios — o subconjunto vira **88 → 86 registros íntegros e singularizáveis**.
- **Futuro:** quando uma unidade iniciar, basta adicionar a linha ao CSV versionado com código oficial e re-bootar (upsert). O campo `ativo=false` não é necessário hoje; foi previsto apenas como extensão, se o dono do projeto preferir pré-cadastrar unidades.

### AD-CI-3 — Carga no boot via CommandLineRunner idempotente, ordenado por dependência de FK (habilita seed de projetos relacionados)

**Resposta à questão levantada na sessão (seed de projeto CSV associado à instituição no primeiro docker run):** sim, o seeder no boot garante isso — **desde que** o FK seja resolvido por **chave de negócio** e não por UUID, e que a **ordem de seed** respeite a dependência. Regra:

1. **Um único `CatalogoSeeder` (`CommandLineRunner`)**, executado após o schema do Hibernate, lê os resources versionados em ordem declarada: `seeds/instituicoes.csv` → (futuro) `seeds/projetos.csv`.
2. **Resolução de FK por chave de negócio:** o seed de um `Projeto` referencia a instituição via `codigoUnidade` (coluna do CSV de projeto), o seeder faz `lookup` da `Instituicao` pela chave (ex.: `instituicaoRepository.findByCodigoUnidade(...)`) e associa o `instituicaoId`. Nunca hard-coda UUID — UUID é gerado pelo banco a cada carga.
3. **Ordem sequencial** dentro do mesmo runner garante que, no **primeiro boot do docker**, instituições existam **antes** dos projetos que as referenciam → FK resolvido sem retrabalho.
4. **Idempotência por upsert:** cada bloco usa a chave de negócio como coluna de conflito (`ON CONFLICT (codigoUnidade) DO NOTHING` para instituições; `ON CONFLICT`—a definir—para projetos). Re-boots repetem sem duplicar e **sem sobrescrever edições do admin** em linhas já semeadas.
5. **Modo de carga controlado por propriedade** `faterepo.seed.mode`:
   - `upsert` (**padrão**, seguro): insere apenas registros ausentes, preserva alterações admin; nada é removido. Atende o "rodar de novo sem quebrar dados de usuário".
   - `replace`: `DELETE` + `INSERT` no mesmo transaction, **resync total a partir da fonte**. **Uso pontual** para o rebuild pedido agora ("ignorar o que está no banco") — recomendado **somente em banco vazio/dev** (ex.: `docker compose down -v`) ou quando não houver `Projeto` referenciando `Instituicao`, pois o `DELETE` de instituição com projetos apontando falha por FK (comportamento desejado: faill-fast com mensagem clara).
6. **Validação no transform:** a transformação (oficial, uma vez) gera o CSV versionado já canonicalizado; o seeder valida ao carregar e **rejeita linha inválida com log** (código vazio/duplicado), sem abortar o boot, para não travar a aplicação por sujeira de fonte.

**Por que startup e não endpoint/script externo:**
- Startup: reproduzível no docker (a pilha sobe sempre igual), zero UI nova, mantém o non-goal "importação batch em runtime" do SPEC. A carga acontece **no deploy**, não num fluxo de usuário.
- Alternativa rejeitada — `POST /instituicoes/importar`: flexível, porém cria superfície de upload/validação em runtime e entra em conflito explícito com o SPEC (importação batch pós-MVP).
- Alternativa rejeitada — script SQL one-off externo: o banco sai pronto, mas a carga fica fora do docker e nada garante relação com futuros seeds de projeto.

### AD-CI-4 — Mudanças estruturais necessárias

| Artefato | Ação |
| --- | --- |
| `model/Instituicao.java` | + `codigoUnidade`, `regiaoAdministrativa`, `cnpj`, `telefone`, `site`, `linkLogo`, `ativo` (e `@Column(unique = true, nullable = false)` em `codigoUnidade`). |
| `InstituicaoRepository` | + `Optional<Instituicao> findByCodigoUnidade(String)`. |
| `dto/request/InstituicaoRequest` / `dto/response/InstituicaoResponse` | espelhar os novos campos (admin curador + catálogo de exibição). |
| `resources/seeds/instituicoes.csv` | arquivo versionado resultante da transformação das 86 linhas (codificado UTF-8-BOM, separador `;`). |
| `CatalogoSeeder` | novo `CommandLineRunner` (pacote `config` ou `seed`), injeta `InstituicaoRepository`, lê CSV (OpenCSV/apache-commons-csv a adicionar ao `pom.xml`), aplica modos `upsert`/`replace`. |
| `application.yml` | `faterepo.seed.mode: upsert` + listagem de arquivos de seeds. |
| `schema.sql`/Hibernate | `ddl-auto: update` cria as novas colunas; sem migração manual. |

## 4. Trade-offs avaliados

| Opção | Prós | Contras | Decisão |
| --- | --- | --- | --- |
| Persistir 17 colunas completas | Fidelidade total à fonte | Campos sem uso, sujeira de formatação, PII (direções/e-mails) | **Não** — AD-CI-1 |
| Modelo mínimo atual | Zero refactor | Não suporta filtros/info que o catálogo deve exibir | **Não** |
| Incluir unidades 2027 | Catálogo "completo" | Códigos vazios/duplicados + unidades inexistentes na seleção | **Não** — AD-CI-2 |
| Endpoint de importação | Flexível pós-deploy | Quebra non-goal do SPEC, superfície nova | **Não** — AD-CI-3 |
| Seed no boot `upsert` | Reproduzível, idempotente, permite seeds relacionados | Exige reinício p/ nova carga (aceitável p/ catálogo curado) | **Sim** — AD-CI-3 |

## 5. Sequência de implementação

1. Gerar `resources/seeds/instituicoes.csv` a partir do export oficial (transform canonicalizador: trim/colapso de espaços, extração de telefones, perfil das 86 unidades).
2. Refatorar `Instituicao` com os novos campos + `unique(codigoUnidade)`.
3. Espelhar DTOs (`*Request`/`*Response`).
4. Implementar `CatalogoSeeder` (modos `upsert`/`replace`, chave `codigoUnidade`, FK-safe ordering).
5. Rodar `docker compose up -d` em banco zerado (`down -v`) → conferir 86 registros; re-boot → conferir idempotência.
6. (Futuro) `seeds/projetos.csv` associando projetos a instituições por `codigoUnidade` no mesmo runner.

## 6. Open questions

- **Coluna de conflito do seed de projeto** (`titulo`+`instituicao_id` vs chave própria) será decidida quando o seed de projetos existir.
- **Quem mantém o CSV versionado**: convenção de atualização quando o CPS publicar novo export (procedimento manual documentado junto a `resources/seeds/README`).
- Units 2027 usarão `ativo=false` quando houver necessidade de pré-cadastro (hoje: manter fora).