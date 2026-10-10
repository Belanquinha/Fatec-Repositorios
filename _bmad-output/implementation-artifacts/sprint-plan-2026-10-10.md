# Sprint Plan — Fatec-Repositorios (2026-10-10)

> Comando: `bmad-sprint-planning`. Escopo solicitado vazio → sprint de **consolidação**:
> o código (commits 2026-10-05 → 2026-10-10) avançou muito além do tracking
> (`sprint-plan.md` e `sprint-status.yaml` parados em 2026-09-23). Nenhuma story nova;
> o trabalho é sincronizar, fechar gaps de contrato e levar o implementado a `done`.

- **Sprint goal:** sincronizar o tracking com a realidade do código, fechar os gaps
  de contrato conhecidos (vitrine pública, regra da fila) e levar todas as stories
  implementadas a `done` com Definition of Done verificável.
- **Data:** 2026-10-10
- **Estado de partida:** 6 epics com implementação mergeada; tracking 17 dias
  desatualizado (dizia "tudo backlog exceto 2-1 in-progress" — falso desde 10-05).

## 1. Prontidão das User Stories (verificada no código, não no papel)

| Story | Veredito | Evidência |
|---|---|---|
| 1-1 MSAL restrito ao tenant CPS | **Pronta → `review`** | `spec-fix-msal-tenant-login.md` (status done, revisão com mutação); `AuthService` valida `tid`; front `quandoPronto()` idempotente |
| 1-2 Classificação de papel por domínio | **Pronta → `review`** | `AuthServiceTest` (11 testes, 3 casos de domínio); regra no servidor |
| 1-3 Sessão e logout | **Pronta → `review`** | dev-login por papel (75fa689), sessão MSAL durável, interceptor limpa sessão no 401 |
| 1-4 Autorização por papel no servidor | **Pronta → `review`** | `SecurityConfig` fail-closed (`anyRequest().authenticated()`, comentário explica a troca); `AutorizacaoPorPapelE2ETest` (15 testes) |
| 2-1 Admin mantém catálogo | **Pronta → `review`** | Seed 86 idempotente + CRUD `InstituicaoController` com escrita `hasRole("ADMIN")`; E2E `CatalogoInstituicoes` (14) + `SeedCatalogo` (4) |
| 2-2 Consulta tipo-para-revelar | **Pronta → `review` (verificar micro-ACs)** | Autocomplete com filtro implementado em `upload-projeto.ts` e aba do Perfil; **verificar**: máx. 20 linhas, empty states, accent-insensitive, "Tentar novamente" |
| 2-3 Vínculo 0–4 na aba do Perfil | **Pronta → `review`** | Contrato AD-VP-1 implementado (`GET`+`PUT /professores/me/instituicoes`, 409 no teto); E2E `VinculoProfessorInstituicao` (152 linhas) + contrato (245); aba Frame B mergeada (5f7953a) |
| 3-1/3-2 Postagem + professor responsável | **Pronta → `review`** | `POST /projetos` nasce `AGUARDANDO_APROVACAO`; `PostagemProjetoE2ETest` (22 testes, NFR5 coberta); form com `mode=single` |
| 4-1 Fila "Pendentes" | **Decidida em 2026-10-10: responsável-somente** (vínculo auto-declarado não é barreira; identidade/autorização via MSAL — `ProjetoService.listarPendentesParaProfessor` / `verificarPermissaoAvaliacao` na árvore de trabalho do review); AC da 4.1 reescrito no `epics.md`; E2E `AvaliacaoProjetoE2ETest` (12 testes) | `GET /projetos/pendentes` + aba no Perfil |
| 4-2 Aprovar/rejeitar | **Pronta → `review`** | `PATCH aprovar/rejeitar` (motivo obrigatório, 409 se já avaliado) + ações no Perfil (55deeba); falta E2E dedicada (QA 10-03 já recomendava) |
| 5-1/5-2 Vitrine pública | **P0-2 já cumprido (verificado 2026-10-10)** — `GET /projetos/publicos` existe; teste `listarPublicosRespondeComSucesso` assevera 200; `rg PROJETOS_MOCK front-end/src` retorna zero (nenhum mock; só seeders de projeto) | `'' → CatalogoPublico` sem redirect; `ver-projeto` |
| 6-1 Perfil hub + landing única | **Pronta → `review` (verificar landing)** | `/perfil` com `authGuard`, abas por papel, identidade MSAL; **verificar**: pré-seleção por estado (aluno→Meus projetos; professor zerado→Instituições; com vínculos→Pendentes; admin→Admin Main) |

Nenhuma story está em `backlog` real. O bloqueador antigo do sprint-plan de 09-23
(endpoints das abas) foi resolvido pelo contrato de 10-07; o gap `GET /projetos/publicos`
foi resolvido no código.

## 2. Escopo da sprint

### Dentro (P0 — fecha a sprint)
| # | Tarefa | Fecha |
|---|---|---|
| P0-1 | Sincronizar `sprint-status.yaml` com §1 (stories implementadas → `review`) | tracking falso |
| P0-2 | ~~Inverter `LacunaDeContrato` (500→200) e remover fallback `PROJETOS_MOCK` se ainda ativo~~ **Feito antes da sprint — verificado 2026-10-10**: teste já assevera 200 (`listarPublicosRespondeComSucesso`), zero ocorrência de `PROJETOS_MOCK` no front | teste documentando bug extinto; vitrine mostrando dados fictícios como reais |
| P0-3 | ~~**Decidir o drift da 4.1**~~ **Decidido 2026-10-10: responsável-somente** (revisto após code-review; AC da 4.1 reescrito no `epics.md`; `sprint-status.yaml` atualizado). Leitura de "interseção" superada | regra de negócio ambígua em produção |
| P0-4 | ~~Remover componente morto `selecionar-instituicao/`~~ **Feito pelo code-review 2026-10-10** (4 arquivos deletados, unstaged) | dead code com teste que sugere superfície extinta |
| P0-5 | ~~`bmad-code-review` nas frentes mergeadas~~ **Executado 2026-10-10 pelo usuário** (gerou ainda `AvaliacaoProjetoE2ETest`, `ClassificacaoPapelE2ETest`, `PayloadTooLargeException` + mudança de regra 4.1) — mover `review` → `done` após run verde da suite | DoD exige revisão |

### Dentro (P1 — cabe se P0 fluir)
| # | Tarefa | Origem |
|---|---|---|
| P1-1 | ~~E2E Epic 4~~ **Feito pelo code-review 2026-10-10**: `AvaliacaoProjetoE2ETest` (12 testes: fila, aprovar/rejeitar, 403/409/400, visão pública LGPD) — só falta run verde para validar | recomendação QA 10-03 §7.4 |
| P1-2 | Checklist de micro-ACs: 2.2 (20 linhas, "…e mais N", empty states, retry) e 6.1 (landing por estado, idempotência) | §1 "verificar" |
| P1-3 | Decidir superfície admin: sem rota `/admin` hoje (admin opera via API/Swagger). Ou declara-se "admin via API = suficiente no MVP" (pós-MVP a UI), ou cria-se story de Admin UI | Story 2-1 fala em "fluxo administrativo" sem definir UI |

### Fora de escopo (pós-MVP / debt registrado)
- `deferred-work.md` (9 itens: single source of truth do tenant via config runtime,
  binding `tenant-id` sem teste, DTO com campos client-supplied, etc.) — atacar após P0/P1.
- Importação de catálogo em runtime, notificações, perfil público, gestor (já fora desde 09-23).
- Auditoria de vínculo, ETag, paginação do GET (open questions do contrato 10-07).

## 3. Riscos e bloqueios

1. **Drift 4.1 (P0-3) — resolvido em 2026-10-10:** responsável-somente, AC reescrito,
   código do review e E2E consistentes. Sem bloqueador conceitual restante.
   E2E da fila (P1-1) pode usar a semântica decidida.
2. **Sprint-status falso há 17 dias** — qualquer `bmad-build` novo parte de premissa
   errada (ex.: reimplementaria o que já existe). P0-1 é pré-requisito de tudo.
3. **Teste que mente (LacunaDeContrato)** — suite verde asseverando comportamento
   extinto; inversão é trivial mas inadiável (P0-2).
4. Flakiness observada no front em 10-03 (`header`/`microsoft-login-button`,
   `setTimeout` zoneless) — não reproduziu em 6 runs; investigar antes de ligar CI
   como gate (recomendação QA §7.5 ainda válida).

## 4. Definition of Done (sprint)

- [ ] `sprint-status.yaml` reflete §1; nenhuma story implementada marcada `backlog`.
- [x] Nenhum teste assevera comportamento extinto (verificado 2026-10-10: teste assevera 200; suite back 107+ e front 74+ — revalidar com run completo na execução).
- [x] Sem dados fictícios na vitrine (`rg PROJETOS_MOCK` zero; só seeders de projeto).
- [x] Semântica da fila (4.1) decidida e registrada (2026-10-10); código e AC consistentes.
- [ ] `bmad-code-review` executado nas frentes P0-5; `review` → `done` onde aprovado.
- [ ] `docker compose up` sobe; lint/build verdes (front + back).
- [ ] Roteiro de execução: P0-1 → P0-3 (decisão) → P0-2 + P0-4 + P1-1 (paralelizáveis após decisão) → P1-2/P1-3 → P0-5 → `bmad-retrospective` (optional por epic).

## 5. Handoff

- **Dono P0-3:** PO/usuário (decisão de produto, não técnica).
- **Depois de P0:** `bmad-build` para P1-1/P1-2; `bmad-code-review` (P0-5);
  `bmad-retrospective` por epic ao fechar cada `done`.
- **Não reabrir:** spines de UX (final 2026-09-29), contrato de vínculo (final 2026-10-07).
