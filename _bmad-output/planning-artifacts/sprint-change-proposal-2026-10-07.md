---
title: "Sprint Change Proposal — Alinhamento dos requisitos à UX final de 2026-09-29 (institution-autocomplete, remoção da /selecionar-instituicao)"
project: Fatec-Repositorios
created: 2026-10-07
status: aprovada
related:
  - planning-artifacts/DESIGN.md (spine, final 2026-09-29 — fonte verdade, não mexer)
  - planning-artifacts/EXPERIENCE.md (spine, final 2026-09-29 — fonte verdade, não mexer)
  - planning-artifacts/UX-SELECAO-INSTITUICOES-PROFESSOR.md (consolidação do recorte)
  - planning-artifacts/mockups/institution-autocomplete.html (Frame B · mode=multi)
  - planning-artifacts/epics.md (Stories 2.2, 2.3, 3.1, 5.1, 6.1)
  - DESIGN.md (raiz, §4.4.1 — desatualizado)
  - planning-artifacts/sprint-change-proposal-2026-09-23.md (antecessora)
---

# Sprint Change Proposal — Alinhar requisitos à UX final de 2026-09-29

## 1. Issue Summary

### Trigger

A rodada `bmad-ux` de 2026-09-29 finalizou os spines (`DESIGN.md` + `EXPERIENCE.md`, status final) com duas decisões canônicas:

1. **A rota `/selecionar-instituicao` foi removida do produto** (DECISÃO 5-REV — reversão da DECISÃO 5). A seleção 0–4 do professor vive **exclusivamente na aba "Instituições (0–4)" do `/perfil`** via `institution-autocomplete mode=multi`.
2. **Um único componente `institution-autocomplete` em três modos**: `single` (Seção 2 do `/projeto-forms`, aluno), `multi` (aba do Perfil, professor), `filter` (filtro da Home, público sem token). Comportamento **tipo-para-revelar**: nada renderiza antes de digitar; texto livre nunca é aceito (catálogo fechado, CAP-3/AD-5).

Os artefatos de **requisitos** (`epics.md`) e o `DESIGN.md` da **raiz** ainda descrevem superfícies/ACs que esta UX proíbe. Um dev que pegar essas stories hoje constrói contra um AC inválido (mapeado em `EXPERIENCE.md` §Contract Delta e em `UX-SELECAO-INSTITUICOES-PROFESSOR.md` §10).

### Categoria

Refinamento de requisitos por convergência de UX (decisão de produto já tomada e especificada nos spines), **não** limitação técnica e **não** mudança de escopo do MVP. Impacto restrito a texto de AC em `epics.md` (4 stories) + deprecação anotada no `DESIGN.md` raiz (§4.4.1). **Nenhum** arquivo-fonte de verdade é tocado nesta proposta: `planning-artifacts/DESIGN.md`, `EXPERIENCE.md`, `UX-SELECAO-INSTITUICOES-PROFESSOR.md` e `mockups/institution-autocomplete.html` **não são reescritos**.

### Evidência

| Fonte verdade (não mexer) | O que fixa |
|---|---|
| `planning-artifacts/EXPERIENCE.md` — Contract Delta (resolvido), IA, Component/State Patterns, Flows 2/3/4 | Tabela de 5 drifts (2.2, 2.3, 3.1, 5.1 + 6.1 já correta); aba "Instituições (0–4)"; tipo-para-revelar; `mode=filter` sem token |
| `planning-artifacts/DESIGN.md` — Brand, Components (`institution-autocomplete`, `combo-popup`, `combo-option`, `institution-chip`, `selection-counter`) | Visual canônico: cantos retos, `border #6A6A6C`, foco `accent-ink`, sem vermelho CPS |
| `planning-artifacts/UX-SELECAO-INSTITUICOES-PROFESSOR.md` §10 | Mapeamento de aceite item a item + gap do endpoint |
| `planning-artifacts/mockups/institution-autocomplete.html` Frame B (+ `README.md`) | Composição da aba: 3-de-4 + teto 4-de-4 com 5ª recusada inline |
| `planning-artifacts/.memlog.md` (entradas 2026-09-29) | DECISÃO 5 → anulada pela DECISÃO 5-REV; SPINE REVERSAL aplicado |

### Pergunta alvo respondida por esta proposta

> **Aprovar a emenda dos ACs das Stories 2.2, 2.3, 3.1 e 5.1 + deprecação anotada do `DESIGN.md` raiz §4.4.1, sem tocar nos spines, antes de mover essas stories para ready-for-dev?**

**Sim.** Aprovar a emenda textual (esta proposta é o texto a colar; `epics.md` **não** é editado nesta rodada) e registrar o gap de back-end como bloqueante da Story 2.3 — porque qualquer implementação a partir do AC atual recria superfícies removidas.

## 2. Impact Analysis

### Epic Impact

| Epic | Status atual | Impacto | Ação |
|---|---|---|---|
| Epic 1 — Autenticação/papel | backlog | **Nenhum** | Sem mudança |
| Epic 2 — Catálogo e seleção 0–4 | in-progress (2-1) | **Texto de AC** em 2.2 e 2.3; regras de negócio 0–4 inalteradas | Emendar ACs (proposta §4.1–4.2); registrar gap de endpoint |
| Epic 3 — Postagem | backlog | **Reancoragem de superfície** em 3.1; regras de postagem inalteradas | Emendar ACs (proposta §4.3) |
| Epic 4 — Aprovação | backlog | **Nenhum** (já re-ancorado em 2026-09-23) | Sem mudança |
| Epic 5 — Vitrine pública | backlog | **Texto de AC** em 5.1 (conversão `<select>` → combobox + fix do bug de binding) | Emendar ACs (proposta §4.4) |
| Epic 6 — Perfil hub | backlog | **Nenhum — Story 6.1 confirmada já correta** | Sem ação (§4.6) |

### Story Impact (resumo — detalhe em §4)

| Story | Tipo de drift | Severidade |
|---|---|---|
| 2.2 | AC "vejo a lista do catálogo" contradiz tipo-para-revelar | **Bloqueante de implementação** (constrói lista de 86 proibida) |
| 2.3 | Superfície "Seleção de Instituições" (rota) vs aba "Instituições (0–4)" do `/perfil` | **Bloqueante de implementação** + gap de back-end |
| 3.1 | AC ancorado em passo prévio de instituição (rota removida) | **Bloqueante de implementação** (recria hop removido) |
| 5.1 | Filtro como `<select>` + bug `inst.nome` × `instituicaoId` | **Bloqueante de qualidade** (recria controle extinto + bug conhecido) |
| 6.1 | — | **Sem ação** (já correta) |

### Artifact Conflicts

| Artefato | Conflito | Ação nesta proposta |
|---|---|---|
| `epics.md` Story 2.2 | AC lista catálogo inteiro | Propor AC novo (§4.1). Não editar o arquivo nesta rodada |
| `epics.md` Story 2.3 | Superfície = rota avulsa | Propor AC novo retargetado p/ aba (§4.2). Não editar o arquivo nesta rodada |
| `epics.md` Story 3.1 | Passo prévio de instituição | Propor AC novo reancorado na Seção 2 (§4.3). Não editar o arquivo nesta rodada |
| `epics.md` Story 5.1 | Filtro `<select>` + bug de binding | Propor AC novo `mode=filter` (§4.4). Não editar o arquivo nesta rodada |
| `epics.md` Story 6.1 | Nenhum | Confirmar correta (§4.6) |
| `DESIGN.md` (raiz) §4.4.1 + §5 | `area-professor` como rota; tabela de rotas com `area-professor[/fila]` | Propor **nota de deprecação** com ponteiro p/ spines (§4.5). Não reescrever o doc |
| `planning-artifacts/DESIGN.md`, `EXPERIENCE.md`, `UX-SELECAO-*.md`, `mockups/` | Nenhum — fontes verdade | **Não mexer** (regra dura desta rodada) |
| SPEC / PRD / Architecture / sprint-status / sprint-plan | Nenhum conflito novo (CAP-8/FR-13/FR-14 já consolidados em 2026-09-23) | Sem mudança |

### Technical Impact

- **Back-end/modelo/regras: nenhuma mudança de regra.** Teto 0–4, catálogo fechado, `instituicaoId`, público sem token — todos preservados.
- **Gap bloqueante registrado (não criado por esta proposta):** o **endpoint de persistência professor↔instituição não existe** (sem controller no back-end — cf. `EXPERIENCE.md` Component Patterns "Salvar vínculos", `UX-SELECAO-*.md` §10, `sprint-plan.md` P2). A Story 2.3 especifica comportamento; a implementação do Save depende de `bmad-architecture` definir o contrato (método, path, 400/409 da 5ª, leitura dos vínculos para a aba).
- **Front-end:** nenhuma decisão nova de componente — os três modos já estão especificados; esta proposta só alinha o texto dos ACs ao que já existe.
- **Risco de quebrar trabalho em andamento:** nenhum — Epic 2 em curso é catálogo/back-end (2-1), intocado por texto de AC de apresentação.

### Impacto em escopo, prazos e prioridades vigentes

- **Escopo:** **inalterado.** Nenhuma story entra ou sai; nenhum FR/NFR muda. É correção de texto de aceite (antes/depois em §4), não replanejamento. Classificação: **Small** (edição de planning, zero código).
- **Prazos:** **sem deslocamento.** A emenda é pré-condição textual para ready-for-dev de 2.2/2.3/3.1/5.1 (evita retrabalho de construir rota/lista/`<select>` extintos), mas não rearranja o sprint: a sequência vigente permanece — fechar 2-1 → Epic 1 → 6-1 + 5-1/5-2 → 2-3 → 3-x → 4-x (cf. `sprint-plan.md` §Roteiro).
- **Prioridades vigentes:** **inalteradas.** P0 (2-1, Epic 1), P1 (6-1, 5-1/5-2), P2 (2-2/2-3/3-x/4-x) preservadas. Observação de ordenação: 2.3 depende de 6.1 (a aba vive no hub) **e** do contrato do endpoint em aberto — o gap deve ir para `bmad-architecture` antes do dev de 2.3, sem mudar a prioridade relativa.
- **Dependência nova explicitada:** dev de 2.3 bloqueado até (a) emenda do AC aprovada e (b) contrato do endpoint professor↔instituição definido. Dev de 2.2/3.1/5.1 bloqueado até (a) apenas.

## 3. Recommended Approach

**Opção 1 — Direct Adjustment (ajuste direto do plano).**

- **Racional:** as decisões de produto já estão tomadas, especificadas e mockadas nos spines finais; o MVP não muda; o esforço é edição de texto de AC (4 stories) + 1 nota de deprecação; nenhum backend muda (só se registra o gap); risco baixo.
- **Opção 2 (Rollback):** não aplicável — nada a reverter (a reversão 5-REV já é a decisão vigente).
- **Opção 3 (Revisão de MVP):** não necessária — nenhuma redução/redireção de escopo.
- **Esforço:** pequeno (planning-only; nenhum código).
- **Risco:** baixo.
- **Caminho recomendado:** aprovar esta proposta → colar os ACs de §4 em `epics.md` em rodada de consolidação → levar o gap do endpoint a `bmad-architecture` → só então ready-for-dev de 2.2/2.3/3.1/5.1.

## 4. Detailed Change Proposals (antes → depois, texto pronto para colar — NÃO aplicado nesta rodada)

Convenção: **OLD** = texto atual em `epics.md` (ou no `DESIGN.md` raiz); **NEW** = texto proposto. Microcopy segue os spines (verbo na frente; "2 de 4 unidades" por extenso; "Máximo de 4 instituições por professor"; nunca "Seleção de Instituições" como título; nunca vermelho).

### 4.1 Story 2.2 — Professor lista o catálogo oficial → tipo-para-revelar

**Arquivo:** `_bmad-output/planning-artifacts/epics.md` · **Story 2.2**

**Drift:** o AC atual manda renderizar a lista do catálogo ao abrir a seleção — o tipo-para-revelar proíbe: nada renderiza antes de digitar (`EXPERIENCE.md` State Patterns "Consulta vazia"; `UX-SELECAO-*.md` §5).

| Antes (OLD) | Depois (NEW — proposto) |
|---|---|
| **When** abro a seleção de instituições / **Then** vejo a lista do catálogo administrado / **And** não existe campo de criação livre de instituição na tela | **When** digito no campo da aba "Instituições (0–4)" do Perfil (`institution-autocomplete mode=multi`, Frame B) / **Then** o catálogo oficial filtra pela consulta (nome, cidade, código ou região, accent-insensitive) e **nada é exibido antes de digitar** / **And** não existe campo de criação livre de instituição em superfície nenhuma |

**NEW (texto completo proposto para os ACs da Story 2.2):**

> ### Story 2.2: Professor consulta o catálogo oficial (tipo-para-revelar)
>
> As a professor,
> I want digitar e ver o catálogo oficial filtrar pela minha consulta,
> So that eu escolha somente instituições oficiais, sem digitar nome livre e sem percorrer a lista inteira.
>
> **Acceptance Criteria:**
> - **Given** que sou professor autenticado na aba **"Instituições (0–4)"** do Perfil (`/perfil`, `institution-autocomplete mode=multi`)
> - **When** o campo está vazio (inclusive no foco/clique)
> - **Then** **nenhum popup/lista é exibido** — vejo apenas o texto de ajuda "Buscamos no catálogo oficial do Centro Paula Souza (86 unidades em operação)."
> - **When** digito uma consulta (ex.: "ipe")
> - **Then** o popup filtra o catálogo oficial por `nome`, `cidade`, `codigoUnidade` e `regiaoAdministrativa` (accent-insensitive), ordenado por `nome` e depois `cidade`, com no máximo 20 linhas + "… e mais N unidades. Refine a busca." (contagem anunciada = nº real de correspondências)
> - **And** sem correspondência, o popup mostra o empty state "Nada encontrado por nome, cidade, código ou região." + "Limpar busca" (cinza, sem vermelho)
> - **And** catálogo indisponível mostra no popup "Catálogo indisponível no momento. [Tentar novamente]", mantendo a consulta
> - **And** não existe campo de criação livre de instituição em superfície nenhuma (sem texto livre, sem auto-cadastro)

**Rationale:** troca a "lista visível" (86 linhas = ferramenta de navegação, rejeitada nos spines) pelo comportamento tipo-para-revelar já especificado; preserva FR-5/NFR4.

### 4.2 Story 2.3 — Professor seleciona de 0 a 4 instituições → aba do Perfil + Save + gap de endpoint

**Arquivo:** `_bmad-output/planning-artifacts/epics.md` · **Story 2.3**

**Drift:** o AC atual fala em "superfície Seleção de Instituições" (leitura de rota avulsa) — a superfície vigente é a **aba "Instituições (0–4)" do `/perfil`**, com contador, chips, Save e recusa inline da 5ª + reforço servidor (cf. `UX-SELECAO-*.md` §§3–5, Frame B).

| Antes (OLD) | Depois (NEW — proposto) |
|---|---|
| **When** seleciono entre 0 e 4 instituições e salvo (seleção opcional no primeiro login) / **Then** o vínculo é salvo / **And** tentar a 5ª é recusado com mensagem clara (limite reforçado no servidor) / **And** posso alterar no futuro | Superfície = **aba "Instituições (0–4)" do `/perfil`** (título "Vincule as unidades onde você atua"); chips + contador "X de 4 unidades" + CTA "Salvar vínculos"; **5ª recusada inline** ("Máximo de 4 instituições por professor", sem modal/vermelho) **+ 400/409 no servidor**; remoção = estado local até o Save; sair com alterações não salvas pede confirmação; erro de Save = painel neutro mantendo chips/contagem |

**NEW (texto completo proposto para os ACs da Story 2.3):**

> ### Story 2.3: Professor vincula de 0 a 4 instituições na aba do Perfil
>
> As a professor,
> I want associar de 0 a 4 instituições do catálogo na aba **"Instituições (0–4)"** do meu Perfil,
> So that eu gerencie a aprovação apenas das instituições onde atuo, sem sair do meu hub.
>
> **Acceptance Criteria:**
> - **Given** que sou professor autenticado no `/perfil`, aba **"Instituições (0–4)"** (título "Vincule as unidades onde você atua", `institution-autocomplete mode=multi` — Frame B)
> - **When** confirmo unidades (`Enter`/clique)
> - **Then** cada confirmação **acrescenta um chip e reabre a consulta**; o contador mostra "X de 4 unidades" por extenso (no teto, "4 de 4 unidades" em `{accent-ink}`); a remoção de chip é **estado local imediato**, sem chamada ao servidor, até o Save
> - **And** com 0 vínculos vejo o onboarding inline "Sem instituições vinculadas ainda — escolha até 4 unidades." + "0 de 4" (mínimo 0 permitido, não bloqueante — FR-6)
> - **When** tento vincular a 5ª unidade
> - **Then** ela é **recusada inline** com "Máximo de 4 instituições por professor", popup continua aberto, sem modal e sem vermelho; para trocar, removo um chip e adiciono outro — **e** o servidor reforça o teto (400/409 na 5ª)
> - **When** clico em **"Salvar vínculos"**
> - **Then** os vínculos persistem; sair/trocar de aba com alterações não salvas pede confirmação; falha no Save mostra painel neutro **acima** do conteúdo da aba **mantendo chips e contagem**, com Save reabilitado
> - **And** posso alterar a seleção no futuro, mantendo entre 0 e 4 instituições
>
> ⚠️ **Gap bloqueante (registrado, dono: `bmad-architecture` → Story 2.3):** o **endpoint de persistência professor↔instituição não existe no back-end** (sem controller; comportamento especificado, não implementado). O contrato (método/path, body de vínculos, 400/409 da 5ª, leitura dos vínculos pela aba, idempotência do Save com 0) precisa ser definido antes do dev desta story.

**Rationale:** retarget rota → aba; explicita contador/chips/Save/recusa inline + reforço servidor; registra o gap sem o qual o Save não é implementável. Regras 0–4 inalteradas.

### 4.3 Story 3.1 — Aluno publica projeto → reancorar na Seção 2 (mode=single)

**Arquivo:** `_bmad-output/planning-artifacts/epics.md` · **Story 3.1**

**Drift:** o AC atual (lido com a nota de 2026-09-23) ainda é compatível com um "passo prévio de instituição" (a rota removida navegava ao form com `?instituicaoId=`) — a escolha do aluno vive **dentro da Seção 2 do `/projeto-forms`** (`mode=single`), sem hop (`EXPERIENCE.md` IA + Flow 2; `UX-SELECAO-*.md` não cobre o aluno, mas o spine cobre).

| Antes (OLD) | Depois (NEW — proposto) |
|---|---|
| Projeto criado "com … `instituicaoId` …" (origem da instituição fora do AC; nota 2026-09-23 só ancora CTA/acompanhamento no Perfil) | Instituição escolhida **na Seção 2 do `/projeto-forms`** via `institution-autocomplete mode=single` (tipo-para-revelar, confirmação, "Alterar", validação de obrigatório); `?instituicaoId=` válido só **pré-preenche** (deep-link legado, não produzido por UI) |

**NEW (acréscimo proposto aos ACs da Story 3.1 — demais ACs inalterados):**

> - **And** a instituição (`instituicaoId`) é escolhida **na Seção 2 do `/projeto-forms`** via `institution-autocomplete mode=single`: nada renderiza antes de digitar; a confirmação (`Enter`/clique) fixa o valor e exibe metadado ("Cód. 291 · São Paulo · Região Litoral") + ação **"Alterar"** (cancelar a edição preserva o valor confirmado); sem texto livre
> - **And** enviar sem confirmar a instituição **não submete**: o foco volta ao campo com borda de erro neutra + mensagem ligada por `aria-describedby`
> - **And** `?instituicaoId=` válido na URL **pré-preenche** o campo já confirmado (deep-link retrocompatível, não produzido por nenhuma UI); id inválido/inativo volta o campo a vazio sem erro
>
> Nota (correção de curso 2026-09-23, mantida): o CTA "Novo Projeto" e o acompanhamento do estado vivem na aba **"Meus projetos"** do Perfil (FR-13/FR-14); esta story cobre apenas a criação e as regras da postagem, não superfície própria de listagem. **Não há passo prévio de instituição** — a rota `/selecionar-instituicao` foi removida (2026-09-29).

**Rationale:** elimina a leitura de hop/passo prévio; ancora o `instituicaoId` no campo vigente com suas travas (obrigatoriedade, deep-link, Alterar-cancelado — State Patterns do spine).

### 4.4 Story 5.1 — Filtro da vitrine → `mode=filter` público (mata o `<select>` + bug)

**Arquivo:** `_bmad-output/planning-artifacts/epics.md` · **Story 5.1**

**Drift:** o AC atual ("filtrar por instituição") é implementável como `<select>` nativo de 86 `<option>` — visual extinto pelo spine — e carrega o **bug real de binding**: `value = inst.nome` vs comparação `proj.instituicaoId === instituicaoSelecionadaId` (cf. `EXPERIENCE.md` Component Patterns "Filtros"; DECISÃO 7 no `.memlog.md`).

| Antes (OLD) | Depois (NEW — proposto) |
|---|---|
| "posso pesquisar por palavras-chave e filtrar por instituição sem requerer token JWT" (controle inespecificado → `<select>` com bug `nome` × `Id`) | Filtro = **`institution-autocomplete mode=filter`** sobre `GET /instituicoes` **público** (sem token/login); escolha vira `institution-chip` removível; filtragem **por `instituicaoId` de verdade**; botão "Limpar" só com filtro ativo |

**NEW (texto proposto para o AC de filtro da Story 5.1 — demais ACs inalterados):**

> - **And** posso pesquisar por palavras-chave e filtrar por instituição **sem requerer token JWT**, sendo o filtro de instituição o `institution-autocomplete mode=filter` (mesmo campo dos outros usos, Frame C): digito e o catálogo público filtra (nome, cidade, código, região); a confirmação vira um `institution-chip` removível; a grade filtra **por `instituicaoId`** (o `<select>` antigo com `value = inst.nome` deixa de existir, e com ele o bug de binding); "Limpar" aparece só com filtro ativo; remover o chip restaura o recorte anterior

**Rationale:** converte o filtro no terceiro modo do componente canônico; fixa o bug pelo desenho (Id, não nome); reafirma CAP-7 (público sem token).

### 4.5 `DESIGN.md` raiz §4.4.1 (+ §5) — deprecação anotada, sem reescrever o doc

**Arquivo:** `DESIGN.md` (raiz do repo) · **Seções:** §4.4.1 (+ linha `area-professor[/fila]` em §4.1/§5 e menções em §4.4.2/§4.6)

**Drift:** o doc raiz ainda especifica `area-professor` como rota de seleção/onboarding e lista `area-professor`, `area-professor/fila` (e `meus-projetos`, `catalogo-publico`, `ver-projeto` como rotas avulsas) — superado pelos spines finais de 2026-09-29, onde as rotas autenticadas são `/perfil`, `/projeto-forms`, `/projetos/:id`, `/admin` e a seleção 0–4 é aba (cf. `UX-SELECAO-*.md` §11 "Divergência conhecida").

**Proposta (não reescrever o documento — colar nota de deprecação no topo do §4.4.1 e na tabela do §5):**

> **OLD (§4.4.1, excerto):** "#### 4.4.1 Seleção de Instituições / Onboarding (`area-professor`) — … É a **landing do 1º acesso** … grid de seleção do catálogo (cards/toggles…) … botões 'Salvar vínculos' … e 'Agora não' … Sucesso … redireciona para a Fila…"
>
> **NEW (nota a inserir acima do §4.4.1, sem apagar o texto legado):**
>
> > ⚠️ **DEPRECADO em 2026-09-29 (DECISÃO 5-REV) — não implementar a partir daqui.** A rota `area-professor` / `/selecionar-instituicao` **foi removida do produto**; a seleção 0–4 do professor vive exclusivamente na **aba "Instituições (0–4)" do `/perfil`** via `institution-autocomplete mode=multi`. Fonte vigente: `_bmad-output/planning-artifacts/DESIGN.md` + `EXPERIENCE.md` (status final 2026-09-29), consolidação em `UX-SELECAO-INSTITUICOES-PROFESSOR.md`, composição em `mockups/institution-autocomplete.html` Frame B. Este §4.4.1 é preservado como histórico; o comportamento especificado (contador, chips, Save, recusa inline da 5ª + 400/409) vale **na aba**, não nesta rota.
>
> **NEW (nota a inserir na tabela do §5, linha `area-professor`):** coluna Estado → "~~Especificada acima~~ **REMOVIDA (2026-09-29) — ver aba 'Instituições (0–4)' do `/perfil` nos spines**". Idem para `area-professor/fila` → aba **"Pendentes"** do `/perfil` (já consolidado em 2026-09-23, Story 4.1).

**Rationale:** depreciação com ponteiro (custo mínimo, sem reescrever o doc inteiro) — exatamente o pedido; elimina o risco de um dev implementar a rota a partir do doc raiz.

### 4.6 Story 6.1 — confirmada já correta (sem ação)

**Arquivo:** `_bmad-output/planning-artifacts/epics.md` · **Story 6.1**

Verificação item a item contra os spines 2026-09-29: landing única no `/perfil` por estado do papel ✓; identidade MSAL somente leitura ✓; layout compartilhado ALUNO/PROFESSOR ✓; lista de rotas sem paralelas (`/`, `/perfil`, `/projeto-forms`, `/projetos/:id`, `/admin`) ✓ — consistente com a remoção da `/selecionar-instituicao`. **Nenhuma emenda proposta.**

## 5. Implementation Handoff

- **Classificação de escopo:** **Small** — correção de texto de planning (4 ACs + 1 nota de deprecação + 1 confirmação); zero código; zero backend.
- **Handoff:** PO (aprovar texto de §4) → rodada de consolidação (colar ACs em `epics.md` + notas no `DESIGN.md` raiz) → Architect (contrato do endpoint professor↔instituição — gap de §4.2) → DEV (ready-for-dev de 2.2/3.1/5.1 após consolidação; 2.3 após consolidação **+** contrato do endpoint).
- **Sequência recomendada:**
  1. Aprovação desta proposta (gate do usuário).
  2. Consolidação textual: `epics.md` (2.2, 2.3, 3.1, 5.1) + notas de deprecação no `DESIGN.md` raiz (§4.4.1, §5). Spines **não** tocados.
  3. `bmad-architecture`: contrato do endpoint professor↔instituição (método/path/body/leitura/400-409/idempotência do Save com 0).
  4. Ready-for-dev: 2.2/3.1/5.1 após (2); 2.3 após (2)+(3). Ordem de build vigente inalterada (sprint-plan §Roteiro).
- **Sucesso:** `epics.md` sem AC que recrie rota/lista/`<select>` extintos; doc raiz sem rota implementável por engano; gap de endpoint com dono e sem ser descoberto no dev; spines intactos como fontes verdade.

## 6. Checklist Record (execução desta rodada)

| Item | Status | Nota |
|---|---|---|
| 1.1 Trigger | Done | UX final 2026-09-29 (remoção `/selecionar-instituicao`, 0–4 → aba via `mode=multi`) |
| 1.2 Categoria | Done | Refinamento de requisitos; sem mudança de escopo |
| 1.3 Evidência | Done | Spines + UX-SELECAO + Frame B + memlog 2026-09-29 |
| 2.1 Epic atual (2) | Done | Completa como planejado (2-1 intocado) |
| 2.2 Mudanças de epic | Done | Nenhum epic novo/obsoleto; só texto de AC |
| 2.3 Epics futuros | Done | 3/4/5 re-ancorados por texto; 6.1 confirmada |
| 2.4 Ordem/prioridade | Done | Inalterada (P0/P1/P2 do sprint-plan) |
| 3.1 PRD | N/A | Sem drift novo (já consolidado em 2026-09-23) |
| 3.2 Architecture | Action-needed | Gap do endpoint → `bmad-architecture` (dono: Story 2.3) |
| 3.3 UX | N/A | Spines finais, não mexer |
| 3.4 Outros | Done | Deprecação anotada no doc raiz (ponteiro, sem reescrita) |
| 4.1 Direct Adjustment | **Viable** | Esforço pequeno, risco baixo |
| 4.2 Rollback | N/A | Nada a reverter |
| 4.3 MVP Review | N/A | Escopo preservado |
| 4.4 Caminho recomendado | **Opção 1** | Ajuste direto do plano |
| 5.1–5.5 | Done | Seções 1–5 desta proposta |
| 6.1–6.3 | Done | Proposta completa; aprovação pendente |
| 6.4 sprint-status | N/A | Sem mudança de status/rótulo |
| 6.5 Handoff | Done | Small → PO/consolidação/Architect |

## 7. Decision / Approval

- **Recomendação:** APROVAR a emenda dos ACs (§4.1–4.4), a deprecação anotada do doc raiz (§4.5) e a confirmação de 6.1 (§4.6), e levar o gap do endpoint a `bmad-architecture` — **antes** de mover 2.2/2.3/3.1/5.1 para ready-for-dev.
- **Decisão:** [x] Aprovado · [ ] Revisar · [ ] Recusado (aprovada pelo usuário em 2026-10-07 — consolidação textual aplicada em `epics.md` + `DESIGN.md` raiz nesta data; spines intactos)
- **Condições (se houver):** consolidação textual executada em rodada própria (esta proposta NÃO edita `epics.md` nem o `DESIGN.md` raiz); spines permanecem intocados.

---
*Gerado por bmad-correct-course em 2026-10-07. Fontes verdade não tocadas: planning-artifacts/DESIGN.md + EXPERIENCE.md (final 2026-09-29) + UX-SELECAO-INSTITUICOES-PROFESSOR.md + mockups/institution-autocomplete.html Frame B.*
