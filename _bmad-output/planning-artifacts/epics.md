---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - _bmad-output/specs/spec-fatec-repositorios/SPEC.md
  - _bmad-output/specs/spec-fatec-repositorios/roles.md
---

# Fatec-Repositorios - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Fatec-Repositorios, decomposing the requirements from the PRD, the canonical SPEC, and architecture constraints into implementable stories. Architecture document (bmad-architecture) is a downstream step; technical constraints below are drawn from the SPEC and the repository (`back-end/`, `front-end/`, `docker-compose.yml`).

## Requirements Inventory

### Functional Requirements

```
FR1: Login with CPS account via MSAL (tenant-restricted); non-CPS accounts cannot authenticate.
FR2: Session and logout; protected resources require valid token.
FR3: Backend classifies role by email domain: UPN ending in @aluno.cps.sp.gov.br -> Aluno (checked first); UPN ending in @cps.sp.gov.br -> Professor; any other CPS tenant member -> default Aluno.
FR4: Role enforced server-side on all authorized operations (403 for wrong role).
FR5: Professor lists the official institution catalog (no free-text creation).
FR6: Professor selects 1 to 4 institutions; a 5th selection is refused.
FR7: Admin maintains the official institution catalog (import/curate).
FR8: Student creates, edits, and publishes a project.
FR9: Student must select a responsible professor at posting time; cannot publish without one.
FR10: Professor sees an approval queue of projects tied to their selected institutions.
FR11: Professor approves or rejects a project (rejection requires a reason); state change visible to student.
```

### NonFunctional Requirements

```
NFR1 (Security): Login restricted to the single CPS tenant; personal/other-tenant Microsoft accounts are rejected.
NFR2 (Security): Role classification is centralized and authoritative in the back-end; never trusted from the front-end. Classificação por sufixo de domínio: aluno = `@aluno.cps.sp.gov.br` (checado primeiro), professor = `@cps.sp.gov.br`; demais membros do tenant → aluno.
NFR3 (Constraint): Hard cap of 4 institutions per professor, enforced server-side.
NFR4 (Constraint): Institutions are catalog data maintained by the system admin; no free-text institution input by professors.
NFR5 (Deployment): The stack runs via docker-compose (front-end + back-end + database).
```

### Additional Requirements

- Centralized role-classification rule per email domain lives in the back-end (MSAL UPN/preferred_username) — from SPEC CAP-2.
- Default stack present in the repo: Angular front-end, Spring back-end, docker-compose; production conventions follow the existing repository layout.
- MSAL front-end integration is expected (SPA login flow) with backend token validation.

### UX Design Requirements

Esta seção fica vazia: ainda não existe contrato UX (DESIGN.md/EXPERIENCE.md). Quando o bmad-ux rodar, reabrir este doc para extrair UX-DRs.

### FR Coverage Map

FR1: Epic 1 - Login MSAL restrito ao tenant CPS
FR2: Epic 1 - Sessão e logout
FR3: Epic 1 - Classificação de papel por domínio (aluno/@aluno.cps.sp.gov.br antes de professor/@cps.sp.gov.br)
FR4: Epic 1 - Autorização por papel no servidor
FR5: Epic 2 - Listar catálogo oficial
FR6: Epic 2 - Seleção de 1 a 4 instituições
FR7: Epic 2 - Admin mantém catálogo
FR8: Epic 3 - Postagem de projeto
FR9: Epic 3 - Professor responsável obrigatório no post
FR10: Epic 4 - Fila de aprovação por instituição
FR11: Epic 4 - Aprovar/rejeitar com estado visível

## Epic List

### Epic 1: Autenticação e Classificação de Papel
Usuários do tenant CPS acessam a plataforma via MSAL e o sistema decide corretamente se cada um é aluno ou professor (regra de domínio no back-end).
**FRs covered:** FR1, FR2, FR3, FR4

### Epic 2: Catálogo e Seleção de Instituições
O admin mantém o catálogo oficial de instituições e o professor seleciona de 1 a 4 delas.
**FRs covered:** FR5, FR6, FR7

### Epic 3: Postagem de Projetos
O aluno publica um projeto selecionando o professor responsável ao postar; o projeto nasce com dono imediato.
**FRs covered:** FR8, FR9

### Epic 4: Aprovação de Projetos
O professor vê a fila de projetos das suas instituições e aprova/rejeita, com o estado aparecendo para o aluno.
**FRs covered:** FR10, FR11

<!-- Repeat for each epic in epics_list (N = 1, 2, 3...) -->

## Epic 1: Autenticação e Classificação de Papel

Usuários do tenant CPS acessam a plataforma via MSAL e o sistema decide corretamente se cada um é aluno ou professor (regra de domínio no back-end).

### Story 1.1: Autenticar via MSAL restrito ao tenant CPS

As a membro do tenant CPS,
I want entrar na plataforma com meu e-mail institucional via MSAL,
So that só contas da CPS acessam o sistema.

**Acceptance Criteria:**

**Given** que não estou autenticado e abro a aplicação
**When** clico em "Entrar"
**Then** sou direcionado ao fluxo MSAL da Microsoft do app registrado
**And** após autenticar como conta do tenant CPS, recebo sessão válida e acesso à aplicação
**And** um teste com token emitido fora do tenant CPS é rejeitado no back-end (401)

### Story 1.2: Classificar papel por domínio no back-end

As a back-end,
I want determinar se o usuário autenticado é aluno ou professor pelo UPN/preferred_username,
So that as telas e regras reflitam o papel certo de cada um.

**Acceptance Criteria:**

**Given** um usuário do tenant CPS autenticado
**When** o back-end avalia o UPN
**Then** UPN terminando em `@aluno.cps.sp.gov.br` é classificado como aluno (checagem antes de professor)
**And** UPN terminando em `@cps.sp.gov.br` é classificado como professor
**And** UPN de outro domínio do tenant CPS cai em aluno (fallback)
**And** testes unitários cobrem os três casos, decididos no servidor

### Story 1.3: Manter sessão e fazer logout

As a usuário autenticado,
I want manter minha sessão ativa e poder sair quando quiser,
So that o acesso às minhas ações permanece protegido.

**Acceptance Criteria:**

**Given** que estou autenticado
**When** acesso um recurso protegido com token válido
**Then** o back-end libera a requisição
**And** ao fazer logout, recursos protegidos passam a retornar 401 sem token novo

### Story 1.4: Aplicar autorização por papel no servidor

As a back-end,
I want exigir o papel (aluno/professor) em toda operação protegida,
So that ninguém execute ações de outro papel adulterando o front.

**Acceptance Criteria:**

**Given** um token de aluno
**When** o aluno tenta uma operação de professor (ex.: listar fila de aprovação)
**Then** o back-end responde 403
**And** a decisão de papel nunca vem do front-end

## Epic 2: Catálogo e Seleção de Instituições

O admin mantém o catálogo oficial de instituições e o professor seleciona de 1 a 4 delas.

### Story 2.1: Admin mantém o catálogo de instituições

As a admin do sistema,
I want incluir, editar, remover e importar instituições do catálogo oficial,
So that a lista disponível aos professores reflita os dados oficiais das instituições CPS.

**Acceptance Criteria:**

**Given** que sou admin autenticado no fluxo administrativo
**When** administro o catálogo
**Then** consigo adicionar, editar, remover e importar instituições
**And** sem papel admin, qualquer alteração de catálogo é negada (403)
**And** as alterações refletem-se na lista votada pelos professores (FR5)

### Story 2.2: Professor lista o catálogo oficial

As a professor,
I want ver a lista oficial de instituições do catálogo,
So that eu escolha somente instituições oficiais, sem digitar nome livre.

**Acceptance Criteria:**

**Given** que sou professor autenticado
**When** abro a seleção de instituições
**Then** vejo a lista do catálogo administrado (FR7)
**And** não existe campo de criação livre de instituição na tela

### Story 2.3: Professor seleciona de 1 a 4 instituições

As a professor,
I want associar de 1 a 4 instituições do catálogo à minha conta,
So that eu gerencie a aprovação apenas das instituições onde atuo.

**Acceptance Criteria:**

**Given** que sou professor autenticado
**When** seleciono entre 1 e 4 instituições e salvo
**Then** o vínculo é salvo com sucesso
**And** tentar adicionar uma 5ª instituição é recusado com mensagem clara (limite reforçado no servidor)
**And** posso alterar a seleção no futuro, sempre dentro do limite

## Epic 3: Postagem de Projetos

O aluno publica um projeto selecionando o professor responsável ao postar; o projeto nasce com dono imediato.

### Story 3.1: Aluno cria e publica um projeto

As a aluno,
I want criar, editar e publicar um projeto da disciplina,
So that meu trabalho fica registrado na plataforma.

**Acceptance Criteria:**

**Given** que sou aluno autenticado
**When** crio um projeto com título, descrição e material e publico
**Then** o projeto persiste e aparece na listagem com estado inicial
**And** consigo editar o rascunho antes de publicar

### Story 3.2: Selecionar o professor responsável é obrigatório

As a aluno,
I want escolher o professor responsável ao postar,
So that todo projeto tenha dono imediato e não fique solto para a instituição.

**Acceptance Criteria:**

**Given** que estou criando um projeto como aluno
**When** tento publicar sem selecionar professor responsável
**Then** a publicação falha e o campo é indicado
**And** todo projeto persistido possui professor responsável não nulo
**And** o professor escolhido enxerga o projeto na sua fila (FR10)

## Epic 4: Aprovação de Projetos

O professor vê a fila de projetos das suas instituições e aprova/rejeita, com o estado aparecendo para o aluno.

### Story 4.1: Professor vê a fila de projetos das suas instituições

As a professor,
I want visualizar os projetos vinculados às instituições que selecionei,
So that eu acompanhe e julgue o que foi produzido nas minhas unidades.

**Acceptance Criteria:**

**Given** que sou professor com 1 a 4 instituições selecionadas
**When** abro a fila de aprovação
**Then** vejo os projetos das minhas instituições
**And** projetos em que sou o professor responsável aparecem destacados

### Story 4.2: Aprovar ou rejeitar projeto com estado visível

As a professor,
I want aprovar ou rejeitar um projeto da minha fila,
So that o aluno saiba o resultado da validação.

**Acceptance Criteria:**

**Given** que há um projeto na minha fila
**When** aprovo
**Then** o estado muda para "aprovado" e fica visível ao aluno
**And** ao rejeitar, o estado muda para "rejeitado" e uma justificativa é registrada
**And** o aluno autenticado vê o estado atualizado na listagem do projeto