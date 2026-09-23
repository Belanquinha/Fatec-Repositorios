---
title: "Sprint Change Proposal — Unificação da área autenticada no hub de Perfil (/perfil)"
project: Fatec-Repositorios
created: 2026-09-23
status: proposta
related:
  - SPEC-fatec-repositorios (CAP-8)
  - prd-Fatec-Repositorios-2026-09-16 (4.8, 4.9, FR-13, FR-14, UJ-1, UJ-2)
  - epics.md (Epics 3, 4; novo Epic 6)
  - DESIGN.md / EXPERIENCE.md (status final, 2026-09-23)
  - _bmad-output/implementation-artifacts/sprint-status.yaml
---

# Sprint Change Proposal — Hub de Perfil (/perfil) como área autenticada única

## 1. Issue Summary

### Trigger

Decisão de UX da elicitação de 2026-09-23: a área **pós-login** deixa de ser um conjunto de superfícies avulsas ("Meus Projetos", "Fila de Aprovação", "Seleção de Instituições") e passa a ser um **hub único de Perfil** (`/perfil`, rota protegida) com layout compartilhado entre **ALUNO** e **PROFESSOR** — abas/atalhos dirigidos por papel, usando o padrão estrutural de *página de canal do YouTube*: bloco de identidade + shortcuts + listas de conteúdo vinculadas ao usuário.

`EXPERIENCE.md` (Foundation) sinaliza expressamente o conflito com o contrato:

> "Landing pós-login: **todo papel autenticado cai no Perfil** ... ⚠️ Isto **delta** sobre o SPEC/PRD (FR-14 destina landings distintas por papel) — confirmado, vira proposta de correção de curso."

### Categoria

Refinamento de IA/experiência do usuário autenticado (nova leitura de requisito vinda de stakeholder/UX), **não** limitação técnica. Impacto restrito a front-end (rotas/IA) e critérios de aceite (AC) de 2–3 stories + 1 story nova.

### Evidência

| Fonte | Evidência |
|---|---|
| `EXPERIENCE.md` Foundation/Information Architecture | Conflito FR-14 explícito; `Perfil (/perfil) — hub do usuário autenticado`; "abas em vez de rotas separadas" |
| `.memlog.md` (2026-09-23) | `(decision)` perfil como hub; `(event)` CONFLITO SURFACEADO: perfil unificado vs FR-14 |
| `DESIGN.md` (2026-09-23) | Tokens `profile-header`, `profile-tab`, `profile-name`, `quick-action-card` já fixados |
| `front-end/src/app/app.routes.ts` | Raiz ainda redireciona `'' → projeto-forms`; **não existe** rota `/perfil`, home nem vitrine |
| `sprint-status.yaml` | Epic 2 in-progress (2-1); Epic 1/3/4 backlog; rótulo **`2-3-professor-seleciona-de-1-a-4-instituições`** diverge do contrato "0 a 4" (SPEC/PRD/epics) |

### Pergunta alvo respondida por esta proposta

> **Aprovar o delta FR-14 e consolidar em SPEC/PRD/epics antes de mover Epic 3/4 para ready-for-dev?**

**Sim.** Aprovar o delta e consolidar os artefatos de contrato **antes** de preparar Epic 3/4 — porque as stories 3-1/4-1 terão ACs ancoradas nas abas do Perfil a partir de agora.

## 2. Impact Analysis

### Epic Impact

| Epic | Status atual | Impacto | Ação |
|---|---|---|---|
| Epic 1 — Autenticação/papel | backlog | **Nenhum** substantivo. Classificação por domínio e guards não mudam; landing vive no SPA após MSAL. | Sem mudança. |
| Epic 2 — Catálogo e seleção 0–4 | **in-progress** (2-1) | **Nenhum** substantivo. Backend seed/CRUD intocado; story 2-2/2-3 permanecem válidas — a superfície "Seleção de Instituições" vira a aba **"Instituições (0–4)"** do Perfil (só muda o contêiner de apresentação). | Corrigir rótulo no sprint-status (1→0 a 4). Nota de apresentação em 2-3. |
| Epic 3 — Postagem | backlog | Story 3-1/3-2 (regras de criação, estado `AGUARDANDO_APROVACAO`, responsável/integrantes) **inalteradas**. O acompanhamento da postagem e o CTA "Novo Projeto" passam a viver no hub. | Nota de ancoragem em 3-1. Bloqueado até consolidar. |
| Epic 4 — Aprovação | backlog | Story 4-1 vira a aba **"Pendentes"** (fila) do Perfil; 4-2 (aprovar/rejeitar com `motivoRejeicao`) permanece — ação dedicada em dialog, perfil é hub, não workspace. | Re-escopo de 4-1 (old→new abaixo). Bloqueado até consolidar. |
| Epic 5 — Vitrine pública | backlog | **Nenhum** — Home (`/`) continua portal público `APROVADO` sem login (CAP-7/FR-12), inalterada. | Sem mudança. |
| **Epic 6 (novo)** — Perfil hub | — | Novo epic para o hub `/perfil` + landing única (FR-13/FR-14). | Criar epic 6 + story 6.1. |

### Story Impact

- **2-3** (`sprint-status.yaml`): corrigir rótulo `de-1-a-4` → `de-0-a-4` (contrato). Nenhuma AC muda.
- **3-1**: nota de ancoragem — CTA "Novo Projeto" e acompanhamento de estado na aba "Meus projetos" do Perfil. Nenhuma AC muda.
- **4-1**: re-escopo — "Professor vê a fila" → aba **"Pendentes"** do Perfil (destaque de `emailProfessorResponsavel` mantido). ACs re-ancoradas.
- **6-1 (nova)**: hub de Perfil (shell compartilhado ALUNO/PROFESSOR, identidade MSAL somente leitura, abas/atalhos por papel, landing única por estado do papel, rota protegida).

### Artifact Conflicts

| Artefato | Conflito/Desejo | Ação |
|---|---|---|
| SPEC (CAP-8) | Landing por papel distinta (aluno→Meus Projetos; professor 1º acesso→Seleção; retorno→Fila) | Reescrever CAP-8 para landing única no Perfil + abas por papel |
| PRD (4.8, 4.9, FR-13, FR-14) | Mesmo delta; FR-13/FR-14 com superfícies avulsas | Reescrever 4.8/4.9, FR-13/FR-14 |
| PRD (UJ-1, UJ-2) | Jornadas chegam em superfícies separadas | Atualizar paths e resolutions para o hub |
| epics.md | Sem epic/story de Perfil; FR-13/FR-14 ausentes do coverage map | Novo Epic 6 + story 6.1; mapa FR13/14→Epic 6 |
| Architecture | AD-1..AD-9 **não cobrem** routing/IA (backend inalterado) | Sem AD obrigatória; sync opcional do seed estrutural (`features/perfil`) |
| sprint-status.yaml | Epic 6 ausente; rótulo 2-3 "1-a-4" divergente | Adicionar epic-6/story-6-1; corrigir rótulo 2-3 |
| DESIGN.md / EXPERIENCE.md | **Já incorporam o hub** (status final 2026-09-23) | Sem mudança |

### Technical Impact

- **Back-end/modelo/API/regras: NENHUMA mudança** — entidades, máquina de estados, `motivoRejeicao`, teto 0–4, domínio de papel e endpoints permanecem como estão (Epic 2 em andamento não é impactado).
- **Front-end/IA:** nova rota protegida `/perfil` (só o próprio vê); consolidação das superfícies em abas (sem rotas paralelas duplicadas); `/projeto-forms` e `/admin` seguem.
- **Risco de quebrar o que está em andamento:** nenhum — Epic 2 (backend catálogo) não toca routing/IA.

## 3. Recommended Approach

**Opção 1 — Direct Adjustment (ajuste direto do plano).**

- **Racional:** não há trabalho concluído a reverter (Epic 2 é catálogo backend, intocado pelo delta); o MVP não muda de escopo (o hub é refinamento de IA do que já era FR-13/FR-14); o delta é pequeno, de baixo acoplamento e já consensado em DESIGN/EXPERIENCE.
- **Opção 2 (Rollback):** não aplicável — nada a reverter.
- **Opção 3 (Revisão de MVP):** não necessária — nenhuma redução/redireção de escopo é requerida.
- **Esforço:** baixo-médio (predominantemente edição de contrato/planning + 1 story nova; nenhum esforço de backend).
- **Risco:** baixo.
- **Impacto em timeline:** Epic 2 segue sem interrupção; Epic 1 segue backlog; Epic 6-6.1 pode iniciar logo após Epic 1; Epic 3/4 ganham ready-for-dev **somente após a consolidação** (evita ACs ancoradas em rotas erradas).
- **Prioridade vigente:** inalterada — vitrine pública (Epic 5) e catálogo (Epic 2) preservados; o hub entra como dependência de apresentação de Epic 3/4.

## 4. Detailed Change Proposals

### 4.1 SPEC — CAP-8 (FR-14)

**Arquivo:** `_bmad-output/specs/spec-fatec-repositorios/SPEC.md` · **Seção:** CAP-8

**OLD:**

> **CAP-8** — Perfil do usuário e landing pós-login por papel
> - **intent:** Todo usuário autenticado visualiza seus dados do MSAL (nome, e-mail institucional, papel) e o contexto do papel; após o login, o sistema direciona por papel. **Landing (elicitação 2026-09-22):** aluno → Meus Projetos; professor sem instituições vinculadas → Seleção de Instituições (onboarding opcional, não bloqueante); professor com vínculos → Fila de Aprovação; admin → Admin Main.
> - **success:** Aluno/professor logados acessam o Perfil (dados do MSAL somente leitura + contexto: aluno vê Meus Projetos; professor vê e edita instituições 0 a 4). O retorno do MSAL redireciona cada papel conforme a regra acima, idempotente (professor zerado é tratado como primeiro acesso).

**NEW:**

> **CAP-8** — Perfil como hub do usuário autenticado e landing única pós-login
> - **intent:** Todo usuário autenticado aterrissa no **Perfil** (`/perfil`, rota protegida, privado — só o próprio vê) — um único hub com layout compartilhado entre ALUNO e PROFESSOR (padrão estrutural de página de canal: bloco de identidade + atalhos + listas de conteúdo dirigidas por papel). Identidade do MSAL (nome, e-mail institucional, papel) **somente leitura**; as superfícies avulsas "Meus Projetos", "Fila de Aprovação" e "Seleção de Instituições" **deixam de ser rotas de destino e viram abas** do hub. **Landing única (correção de curso 2026-09-23):** após o login, todo papel aterrissa no Perfil com aba/atalho pré-selecionado pelo estado do papel — aluno → aba "Meus projetos" (agrupada por estado: Aguardando aprovação / Publicados / Rejeitados com `motivoRejeicao` inline) + CTA "Novo Projeto"; professor sem vínculos → destaque/aba "Instituições (0–4)" (onboarding opcional, **não bloqueante**); professor com vínculos → aba "Pendentes" (fila) e histórico "Aprovados"; admin → atalho Admin Main.
> - **success:** Aluno e professor logados acessam o Perfil (identidade MSAL somente leitura + abas por papel). O retorno do MSAL leva **sempre** ao Perfil (idempotente — professor zerado tratado como primeiro acesso com onboarding não-bloqueante); aprovação/rejeição permanece ação dedicada (dialog de `motivoRejeicao`) a partir da lista — o perfil é **hub, não workspace**; nenhuma das superfícies consolidadas existe como rota paralela duplicada.

**Rationale:** torna o contrato canônico consistente com o hub já fixado em DESIGN/EXPERIENCE e elimina a divergência de landing por papel que induziria rotas paralelas duplicadas.

### 4.2 PRD — 4.8, 4.9, FR-13, FR-14, UJ-1, UJ-2

**Arquivo:** `_bmad-output/planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md`

**§4.8 Description (OLD):**

> Todo usuário autenticado visualiza seus dados vindos da sessão MSAL ... e o contexto do seu papel: aluno acessa Meus Projetos; professor visualiza/edita suas instituições vinculadas (0 a 4) e a Fila. Landing pós-login: 1º acesso do professor sem vínculos → Seleção de Instituições; retorno → Fila; aluno → Meus Projetos.

**§4.8 Description (NEW):**

> Todo usuário autenticado cai no **Perfil** (`/perfil`, privado — só o próprio vê): hub único com layout compartilhado entre ALUNO e PROFESSOR (padrão estrutural de página de canal — bloco de identidade + atalhos + listas de conteúdo). Identidade do MSAL (nome, e-mail institucional, papel) **somente leitura** (FR-13). **ALUNO:** aba "Meus projetos" agrupada por estado (Aguardando aprovação / Publicados / Rejeitados com `motivoRejeicao` inline) + atalho CTA "Novo Projeto". **PROFESSOR:** abas "Pendentes" (fila, destacando quando `emailProfessorResponsavel` = o professor), "Aprovados" (histórico) e "Instituições (0–4)" com onboarding quando sem vínculos. **ADMIN:** atalho para Admin Main. Aprovação/rejeição permanece ação dedicada (dialog de motivo obrigatório); o perfil é hub, não workspace. (decisão de elicitação 2026-09-23 — correção de curso sobre o SPEC/FR-14)

**FR-13 (NEW):**

> #### FR-13: Visualizar perfil como hub e contexto do papel
> Usuário autenticado visualiza dados do MSAL (somente leitura) e o contexto do papel em abas/atalhos dirigidos por papel: aluno → "Meus projetos"; professor → "Pendentes", "Aprovados", "Instituições (0–4)"; admin → atalho Admin Main. `motivoRejeicao` exibido inline em projetos rejeitados do aluno.

**§4.9 Description (NEW):**

> **Description:** Direcionamento pós-MSAL para o hub único de Perfil, com aba/atalho pré-selecionado pelo estado do papel; primeiro acesso do professor (zerado) tratado como onboarding não-bloqueante.

**FR-14 (NEW):**

> #### FR-14: Aterrissar no Perfil após login MSAL
> Após autenticar via MSAL, **todo papel aterrissa no Perfil** (`/perfil`, rota protegida); a aba/atalho pré-selecionada é decidida pelo estado do papel: aluno → "Meus projetos"; professor sem instituições vinculadas → destaque/aba "Instituições (0–4)" (onboarding não bloqueante); professor com vínculos → "Pendentes"; admin → atalho Admin Main. Fluxo idempotente no 1º acesso (professor zerado nunca é bloqueado).

**UJ-1 (Path/Resolution — OLD→NEW):**

> **Path (OLD):** ...→ cai no papel aluno (e-mail `@aluno.cps.sp.gov.br`) → acessa "Novo projeto" → preenche...
> **Path (NEW):** ...→ cai no papel aluno (e-mail `@aluno.cps.sp.gov.br`) → aterrissa no **Perfil** com a aba "Meus projetos" pré-selecionada → aciona o CTA **"Novo Projeto"** → preenche...
>
> **Resolution (OLD):** Mariana acompanha o status nas próximas visitas até que mude para `APROVADO`.
> **Resolution (NEW):** Mariana acompanha o status nas abas do Perfil ("Meus projetos"), que agrupa por estado até `APROVADO`; se rejeitado, o `motivoRejeicao` aparece inline.

**UJ-2 (Path — OLD→NEW):**

> **Path (OLD):** → acessa a área do professor → pode selecionar de 0 a 4 instituições... + fila...
> **Path (NEW):** → aterrissa no **Perfil**; sem vínculos no 1º acesso, a aba **"Instituições (0–4)"** é destacada como onboarding (não bloqueante); vincula de 0 a 4 unidades do catálogo; a fila é a aba **"Pendentes"** (projetos com `emailProfessorResponsavel` = Roberto destacados), com histórico na aba **"Aprovados"**; aprova/rejeita com `motivoRejeicao` obrigatório no dialog.

**Rationale:** 4.8/4.9 e as jornadas passam a descrever o hub como superfície única, coerente com CAP-8 e com o que Epic 3/4 entregarão.

### 4.3 Epics — re-escopo 4-1, novo Epic 6, ancoragem 3-1

**Arquivo:** `_bmad-output/planning-artifacts/epics.md`

**Story 4.1 (OLD→NEW):**

**OLD:**

> ### Story 4.1: Professor vê a fila de projetos das suas instituições
> As a professor, I want visualizar os projetos vinculados às instituições que selecionei, So that eu acompanhe e julgue o que foi produzido nas minhas unidades.
> **Acceptance Criteria:** ... Quando abro a fila de aprovação → vejo os projetos ... em `AGUARDANDO_APROVACAO` ... destacados.

**NEW:**

> ### Story 4.1: Professor vê a fila ("Pendentes") na aba do Perfil
> As a professor, I want acessar a fila dos projetos vinculados às minhas instituições pela aba **"Pendentes"** do Perfil, So that eu acompanhe e julgue o que foi produzido nas minhas unidades no meu hub.
> **Acceptance Criteria:**
> - Given que sou professor com instituições selecionadas (de 0 a 4) e estou no Perfil
> - When aciono a aba "Pendentes"
> - Then vejo os projetos das minhas instituições em `AGUARDANDO_APROVACAO`, como conteúdo da aba (não como rota paralela)
> - And projetos em que meu e-mail é o `emailProfessorResponsavel` aparecem destacados
>
> **Rationale:** a "fila de aprovação" deixa de ser superfície/rota avulsa e vira aba do hub `/perfil` (CAP-8/FR-14, correção de curso 2026-09-23).

**Story 3.1 — nota de ancoragem (adicionar ao fim dos ACs):**

> Nota (correção de curso 2026-09-23): o CTA "Novo Projeto" e o acompanhamento do estado vivem na aba **"Meus projetos"** do Perfil (FR-13/FR-14); a story cobre apenas criação/regras da postagem, não superfície de listagem própria.

**Novo Epic 6 (adicionar após Epic 5):**

> ## Epic 6: Perfil — Hub do Usuário Autenticado
> Todo usuário autenticado aterrissa no Perfil (`/perfil`, protegido): hub único com layout compartilhado entre ALUNO e PROFESSOR, identidade MSAL somente leitura e abas/atalhos dirigidos por papel; superfícies avulsas ("Meus Projetos", "Fila", "Seleção de Instituições") viram abas.
> **FRs covered:** FR-13, FR-14
>
> ### Story 6.1: Hub de Perfil com identidade MSAL e landing única
> As a usuário autenticado, I want acessar meu Perfil como hub (bloco de identidade + atalhos + abas por papel) e aterrissar nele após o login, So that eu encontre meu contexto e ações do meu papel em um só lugar, sem rotas paralelas.
> **Acceptance Criteria:**
> - Given que autentiquei via MSAL
> - When o login retorna
> - Then sou direcionado ao `/perfil` (rota **protegida**; só o próprio usuário vê o próprio perfil)
> - And a aba/atalho pré-selecionada segue o estado do papel: aluno → "Meus projetos"; professor zerado → destaque "Instituições (0–4)" (onboarding não bloqueante); professor com vínculos → "Pendentes"; admin → atalho Admin Main
> - And o bloco de identidade exibe avatar, nome (`h2`), e-mail institucional e papel — dados do MSAL **somente leitura** (FR-13)
> - And ALUNO e PROFESSOR compartilham o mesmo layout de hub (abas/atalhos dirigidos por papel — padrão canal)
> - And "Meus Projetos"/"Pendentes"/"Instituições" **não existem como rotas paralelas** (rotas: `/`, `/perfil`, `/projeto-forms`, `/projetos/:id`, `/admin`)

**FR Coverage Map — adicionar:**

> FR-13: Epic 6 - Perfil hub (identidade + abas por papel)
> FR-14: Epic 6 - Landing única no Perfil por estado do papel

**Rationale:** fecha o gap do mapa (FR-13/14 hoje não cobertos por epic algum), cria o epic de entrega do hub e re-ancora Epic 3/4 sem alterar suas regras de negócio.

### 4.4 Architecture — sync opcional (sem AD obrigatória)

**Arquivo:** `_bmad-output/planning-artifacts/architecture/architecture-Fatec-Repositorios-2026-09-16/ARCHITECTURE-SPINE.md`

- **Sem nova AD:** nenhum invariante/regra de backend muda; AD-1 (papel do backend usado para renderização de UI) já sustenta a UI dirigida por papel do hub.
- **Recomendado (baixo esforço):** atualizar `Structural Seed` de `front-end/src/app/features/` para incluir `perfil/` (hub, abas por papel) e `shared/` para `profile-header`/`profile-tab`; adicionar a linha de mapa de capacidade `CAP-8 Perfil hub & landing | front-end (rota /perfil + abas por papel) | FR-13/FR-14` (não presente hoje — CAP-8 não está mapeado).
- **Backend:** vorre; nenhum DTO/entidade/endpoint/regra.

### 4.5 sprint-status.yaml — rótulo + novo epic

**Arquivo:** `_bmad-output/implementation-artifacts/sprint-status.yaml`

- Corrigir `${'2-3-professor-seleciona-de-1-a-4-instituições'}` → **`2-3-professor-seleciona-de-0-a-4-instituições`** (alinhamento ao contrato 0–4; valor `backlog` inalterado).
- Adicionar ao final (status `backlog`):

```yaml
  epic-6: backlog
  6-1-perfil-hub-com-identidade-e-landing-única: backlog
  epic-6-retrospective: optional
```

**Rationale:** aproveita a correção de curso para eliminar a divergência de rótulo "1-a-4" vs "0 a 4".

## 5. Implementation Handoff

- **Classificação de escopo:** **Moderate** — reorganização de backlog (coordenação PO/DEV); sem backend; sem replanejamento estratégico.
- **Handoff:** PO + Developer (edição de SPEC/PRD/epics/sprint-status) em uma única rodada; PM/Architect apenas ciência (nenhuma decisão estratégica).
- **Sequência recomendada:**
  1. Aprovação desta proposta (gate do usuário).
  2. Consolidar SPEC (CAP-8), PRD (4.8/4.9/FR-13/14/UJ-1/2), epics (4-1 re-escopo, nota 3-1, Epic 6 + story 6.1, mapa FR), sprint-status (rótulo 2-3 + epic-6).
  3. Epic 1 concluir → Epic 6-6.1 (hub shell) como próximo; abas populadas conforme Epic 2/3/4 entregam dados.
  4. **Epic 3/4 só vão a ready-for-dev após o passo 2** (ACs já ancoradas nas abas do Perfil, evitando retrabalho de rotas paralelas).
- **Sucesso:** DEFINIR os artefatos consistentes entre si (CAP-8 ↔ FR-13/14 ↔ stories 3.1/4.1/6.1 ↔ labels "0–4"); DESIGN/EXPERIENCE permanecem finais e alinhados; **zero diff de backend**; Epic 2 não é interrompido.

## 6. Checklist Record (execução desta rodada)

| Item | Status | Nota |
|---|---|---|
| 1.1 Trigger | Done | Decisão UX 2026-09-23 (hub /perfil) |
| 1.2 Categoria | Done | Refinamento de IA (novo entendimento do fluxo autenticado) |
| 1.3 Evidência | Done | EXPERIENCE/memlog/tokens/rotas atuais/rótulo sprint-status |
| 2.1 Epic atual (2) | Done | Completa como planejado (backend) |
| 2.2 Mudanças de epic | Done | Novo Epic 6; re-escopo 4.1 |
| 2.3 Epics futuros | Done | Epic 3/4 ancorados; Epic 5 intacto |
| 2.4 Obsoletos/novos | Done | Nenhum obsoleto; Epic 6 novo |
| 2.5 Ordem/prioridade | Done | Epic 6 após Epic 1; 3/4 dependem da consolidação |
| 3.1 PRD | Done | 4.8/4.9, FR-13/14, UJ-1/2 |
| 3.2 Architecture | Done | Sem backend; sync opcional do seed |
| 3.3 UX | N/A | DESIGN/EXPERIENCE já finais |
| 3.4 Outros | Done | sprint-status (rótulo + epic 6); sem CI/deploy |
| 4.1 Direct Adjustment | **Viable** | Esforço baixo-médio, risco baixo |
| 4.2 Rollback | N/A | Nada a reverter |
| 4.3 MVP Review | N/A | Escopo preservado |
| 4.4 Caminho recomendado | **Opção 1** | Ajuste direto do plano |
| 5.1–5.5 | Done | Seções 1–5 desta proposta |
| 6.1–6.3 | Done | Proposta completa e precisa; aprovação pendente |
| 6.4 sprint-status | Action-needed | Executar após aprovação (rótulo 2-3 + epic-6) |
| 6.5 Handoff | Done | Moderate → PO/DEV |

## 7. Decision / Approval

- **Recomendação:** APROVAR o delta FR-14 e consolidar SPEC/PRD/epics/sprint-status **antes** de mover Epic 3/4 para ready-for-dev.
- **Decisão:** [x] Aprovado · [ ] Revisar · [ ] Recusado
- **Condições (se houver):** aprovado em 2026-09-23; consolidação executada na mesma rodada (SPEC CAP-8; PRD 4.8/4.9/FR-13/14/UJ-1/2; epics 4.1/3.1/2.3/Epic 6+6.1/coverage; sprint-status rótulo 2-3, chave 4-1, epic-6; ARCHITECTURE-SPINE sync CAP-8/seed).

---
*Gerado por bmad-correct-course em 2026-09-23.*