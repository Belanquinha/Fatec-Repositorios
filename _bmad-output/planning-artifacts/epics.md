---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - _bmad-output/specs/spec-fatec-repositorios/SPEC.md
  - _bmad-output/specs/spec-fatec-repositorios/roles.md
---

# Fatec-Repositorios - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Fatec-Repositorios, decomposing the requirements from the PRD, the canonical SPEC, and architecture constraints into implementable stories.

## Requirements Inventory

### Functional Requirements

```
FR1: Login with CPS account via MSAL (tenant-restricted); non-CPS accounts cannot authenticate.
FR2: Session and logout; protected resources require valid token.
FR3: Backend classifies role by email domain: UPN ending in @aluno.cps.sp.gov.br -> Aluno (checked first); UPN ending in @cps.sp.gov.br -> Professor; any other CPS tenant member -> default Aluno.
FR4: Role enforced server-side on all authorized operations (403 for wrong role).
FR5: Professor lists the official institution catalog (no free-text creation).
FR6: Professor selects 0 to 4 institutions (optional at first login); a 5th selection is refused.
FR7: Admin maintains the official institution catalog (import/curate).
FR8: Student creates and publishes a project born directly as AGUARDANDO_APROVACAO (no draft state).
FR9: Student must provide emailProfessorResponsavel (autocomplete in front-end + free-text "Professor Convidado" accepted), instituicaoId, complete project attributes, and project Integrantes (nome, linkLinkedin).
FR10: Professor sees an approval queue of projects tied to their selected institutions.
FR11: Professor approves or rejects a project (rejection requires mandatory motivoRejeicao); state change visible to student.
FR12: Unauthenticated visitors search and view details of APROVADO projects without login.
```

### NonFunctional Requirements

```
NFR1 (Security): Login restricted to the single CPS tenant; personal/other-tenant Microsoft accounts are rejected.
NFR2 (Security): Role classification is centralized and authoritative in the back-end; never trusted from the front-end. Classificação por sufixo de domínio: aluno = @aluno.cps.sp.gov.br (checado primeiro), professor = @cps.sp.gov.br; demais membros do tenant -> aluno.
NFR3 (Constraint): Hard cap of 0 to 4 institutions per professor, enforced server-side. Selection optional at first login.
NFR4 (Constraint): Institutions are catalog data maintained by the system admin; no free-text institution input by professors.
NFR5 (Constraint): Project has no RASCUNHO state; starts directly as AGUARDANDO_APROVACAO. Rejection requires mandatory motivoRejeicao.
NFR6 (Deployment): The stack runs via docker-compose (front-end + back-end + database).
```

### FR Coverage Map

FR1: Epic 1 - Login MSAL restrito ao tenant CPS  
FR2: Epic 1 - Sessão e logout  
FR3: Epic 1 - Classificação de papel por domínio  
FR4: Epic 1 - Autorização por papel no servidor  
FR5: Epic 2 - Listar catálogo oficial  
FR6: Epic 2 - Seleção de 0 a 4 instituições  
FR7: Epic 2 - Admin mantém catálogo  
FR8: Epic 3 - Postagem de projeto diretamente como AGUARDANDO_APROVACAO  
FR9: Epic 3 - E-mail do professor responsável, atributos e integrantes no post  
FR10: Epic 4 - Fila de aprovação por instituição  
FR11: Epic 4 - Aprovar/rejeitar com motivoRejeicao obrigatório  
FR12: Epic 5 - Catálogo público para visitantes  

## Epic List

### Epic 1: Autenticação e Classificação de Papel
Usuários do tenant CPS acessam a plataforma via MSAL e o sistema decide corretamente se cada um é aluno ou professor (regra de domínio no back-end).
**FRs covered:** FR1, FR2, FR3, FR4

### Epic 2: Catálogo e Seleção de Instituições
O admin mantém o catálogo oficial de instituições e o professor seleciona entre 0 e 4 delas (opcional no primeiro login).
**FRs covered:** FR5, FR6, FR7

### Epic 3: Postagem de Projetos
O aluno publica um projeto nascendo como `AGUARDANDO_APROVACAO` informando o e-mail do professor responsável, instituição, atributos completos e a lista de integrantes.
**FRs covered:** FR8, FR9

### Epic 4: Aprovação de Projetos
O professor vê a fila de projetos das suas instituições e aprova/rejeita (com `motivoRejeicao` obrigatório na rejeição).
**FRs covered:** FR10, FR11

### Epic 5: Catálogo Público para Visitantes
Usuários não autenticados pesquisam e visualizam projetos no estado `APROVADO` sem login.
**FRs covered:** FR12

---

## Epic 1: Autenticação e Classificação de Papel

Usuários do tenant CPS acessam a plataforma via MSAL e o sistema decide corretamente se cada um é aluno ou professor (regra de domínio no back-end).

### Story 1.1: Autenticar via MSAL restrito ao tenant CPS

As a membro do tenant CPS,  
I want entrar na plataforma com meu e-mail institucional via MSAL,  
So that só contas da CPS acessam o sistema.

**Acceptance Criteria:**
- **Given** que não estou autenticado e abro a aplicação
- **When** clico em "Entrar"
- **Then** sou direcionado ao fluxo MSAL da Microsoft do app registrado
- **And** após autenticar como conta do tenant CPS, recebo sessão válida e acesso à aplicação
- **And** um teste com token emitido fora do tenant CPS é rejeitado no back-end (401)

### Story 1.2: Classificar papel por domínio no back-end

As a back-end,  
I want determinar se o usuário autenticado é aluno ou professor pelo UPN/preferred_username,  
So that as telas e regras reflitam o papel certo de cada um.

**Acceptance Criteria:**
- **Given** um usuário do tenant CPS autenticado
- **When** o back-end avalia o UPN
- **Then** UPN terminando em `@aluno.cps.sp.gov.br` é classificado como aluno (checagem antes de professor)
- **And** UPN terminando em `@cps.sp.gov.br` é classificado como professor
- **And** UPN de outro domínio do tenant CPS cai em aluno (fallback)
- **And** testes unitários cobrem os três casos, decididos no servidor

### Story 1.3: Manter sessão e fazer logout

As a usuário autenticado,  
I want manter minha sessão ativa e poder sair quando quiser,  
So that o acesso às minhas ações permanece protegido.

**Acceptance Criteria:**
- **Given** que estou autenticado
- **When** acesso um recurso protegido com token válido
- **Then** o back-end libera a requisição
- **And** ao fazer logout, recursos protegidos passam a retornar 401 sem token novo

### Story 1.4: Aplicar autorização por papel no servidor

As a back-end,  
I want exigir o papel (aluno/professor) em toda operação protegida,  
So that ninguém execute ações de outro papel adulterando o front.

**Acceptance Criteria:**
- **Given** um token de aluno
- **When** o aluno tenta uma operação de professor (ex.: listar fila de aprovação)
- **Then** o back-end responde 403
- **And** a decisão de papel nunca vem do front-end

---

## Epic 2: Catálogo e Seleção de Instituições

O admin mantém o catálogo oficial de instituições e o professor seleciona de 0 a 4 delas.

### Story 2.1: Admin mantém o catálogo de instituições

As a admin do sistema,  
I want incluir, editar e remover instituições do catálogo oficial,  
So that a lista disponível aos professores reflita os dados oficiais das instituições CPS.

**Acceptance Criteria:**
- **Given** que sou admin autenticado no fluxo administrativo (via MSAL)
- **When** administro o catálogo
- **Then** consigo adicionar, editar e remover instituições (CRUD simples no MVP)
- **And** o catálogo oficial é pré-populado no boot a partir dos recursos versionados (seed idempotente, sem importação em runtime)
- **And** sem papel admin, qualquer alteração de catálogo é negada (403)

### Story 2.2: Professor lista o catálogo oficial

As a professor,  
I want ver a lista oficial de instituições do catálogo,  
So that eu escolha somente instituições oficiais, sem digitar nome livre.

**Acceptance Criteria:**
- **Given** que sou professor autenticado
- **When** abro a seleção de instituições
- **Then** vejo a lista do catálogo administrado
- **And** não existe campo de criação livre de instituição na tela

### Story 2.3: Professor seleciona de 0 a 4 instituições

As a professor,  
I want associar de 0 a 4 instituições do catálogo à minha conta,  
So that eu gerencie a aprovação apenas das instituições onde atuo.

**Acceptance Criteria:**
- **Given** que sou professor autenticado
- **When** seleciono entre 0 e 4 instituições e salvo (seleção opcional no primeiro login)
- **Then** o vínculo é salvo com sucesso
- **And** tentar adicionar uma 5ª instituição é recusado com mensagem clara (limite reforçado no servidor)
- **And** posso alterar a seleção no futuro, mantendo entre 0 e 4 instituições

---

## Epic 3: Postagem de Projetos

O aluno publica um projeto nascendo diretamente como `AGUARDANDO_APROVACAO`, informando e-mail do professor responsável, instituição, atributos completos e integrantes.

### Story 3.1: Aluno publica um projeto (diretamente AGUARDANDO_APROVACAO)

As a aluno,  
I want publicar um projeto preenchendo todos os atributos e integrantes,  
So that meu trabalho é submetido diretamente para aprovação.

**Acceptance Criteria:**
- **Given** que sou aluno autenticado
- **When** crio um projeto com título, descrição curta, conteúdo EditorJS, link do repositório, imagem de capa, palavras-chave, ano de publicação, instituicaoId e integrantes
- **Then** o projeto é salvo diretamente com estado `AGUARDANDO_APROVACAO` (não existe estado rascunho)
- **And** os integrantes são salvos associados ao projeto (Projeto 1 ── N Integrante) com `nome` e `linkLinkedin`
- **And** imagens complementares são blocos nativos do Editor.js (não existe `imagensExtras`)

### Story 3.2: Informar e-mail do professor responsável com autocomplete

As a aluno,  
I want selecionar/digitar o e-mail do professor responsável ao postar,  
So that o projeto registre o responsável de forma consistente, inclusive fora do tenant.

**Acceptance Criteria:**
- **Given** que estou criando um projeto como aluno
- **When** digito/seleciono o e-mail do professor responsável
- **Then** o front-end oferece autocomplete dos e-mails de professores cadastrados no tenant
- **And** e-mails não cadastrados são aceitos como "Professor Convidado" (registro informativo, sem notificação no MVP)
- **And** o atributo `emailProfessorResponsavel` é persistido obrigatoriamente
- **And** tentar publicar sem `emailProfessorResponsavel` falha

---

## Epic 4: Aprovação de Projetos

O professor vê a fila de projetos das suas instituições e aprova/rejeita (com motivo obrigatório).

### Story 4.1: Professor vê a fila de projetos das suas instituições

As a professor,  
I want visualizar os projetos vinculados às instituições que selecionei,  
So that eu acompanhe e julgue o que foi produzido nas minhas unidades.

**Acceptance Criteria:**
- **Given** que sou professor com instituições selecionadas (de 0 a 4)
- **When** abro a fila de aprovação
- **Then** vejo os projetos vinculados às minhas instituições em estado `AGUARDANDO_APROVACAO`
- **And** projetos em que meu e-mail é o `emailProfessorResponsavel` aparecem destacados

### Story 4.2: Aprovar ou rejeitar projeto (motivoRejeicao obrigatório na rejeição)

As a professor,  
I want aprovar ou rejeitar um projeto da minha fila,  
So that o aluno saiba o resultado da validação.

**Acceptance Criteria:**
- **Given** que há um projeto na minha fila em `AGUARDANDO_APROVACAO`
- **When** aprovo, o estado muda para `APROVADO`
- **When** rejeito, sou obrigado a informar a justificativa e o estado muda para `REJEITADO` com `motivoRejeicao` preenchido
- **Then** o aluno visualiza o novo estado atualizado

---

## Epic 5: Catálogo Público para Visitantes

Usuários não autenticados pesquisam e visualizam projetos no estado `APROVADO` sem necessidade de login.

### Story 5.1: Pesquisar e listar projetos aprovados na vitrine pública

As a visitante não autenticado,  
I want pesquisar e filtrar projetos aprovados no portal público,  
So that eu conheça os trabalhos desenvolvidos pelos alunos da Fatec/CPS.

**Acceptance Criteria:**
- **Given** que não estou autenticado na aplicação
- **When** aceso a página de catálogo público
- **Then** visualizo a lista/cards de projetos com estado `APROVADO`
- **And** posso pesquisar por palavras-chave e filtrar por instituição sem requerer token JWT

### Story 5.2: Visualizar detalhes completos de um projeto aprovado

As a visitante não autenticado,  
I want abrir o detalhe de um projeto aprovado,  
So that eu veja todo o conteúdo rico, imagens, integrantes e link do repositório.

**Acceptance Criteria:**
- **Given** que estou navegando no catálogo público
- **When** seleciono um projeto aprovado
- **Then** vejo o título, descrição curta, conteúdo EditorJS, integrantes, ano, palavras-chave e link do repositório
- **And** o acesso ao endpoint público de detalhes não exige autenticação