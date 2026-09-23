---
title: Fatec Repositorios
created: 2026-09-16
updated: 2026-09-20
status: final
---

# PRD: Fatec Repositorios
*Plataforma web full-stack para alunos Fatec/CPS postarem projetos e professores aprovarem, com catálogo público de projetos aprovados.*

## 0. Document Purpose

Este PRD define o produto **Fatec Repositorios** para o time do TCC, o dono do projeto e os fluxos de planejamento a jusante (arquitetura, epics/histórias, UX). Estrutura-se pelo glossário na seção 3, com features agrupadas e requisitos funcionais (FR) numerados globalmente. Baseia-se no contrato canônico já aprovado **SPEC-fatec-repositorios** (`_bmad-output/specs/spec-fatec-repositorios/`), inclusive na matriz de papéis `roles.md`; este PRD não duplica o SPEC, refina-o em requisitos testáveis. Toda decisão de negócio vem do SPEC.

## 1. Vision

Uma aplicação web (front-end Angular + back-end Spring + docker) onde **alunos da Fatec/CPS publicam projetos construídos em sala e no curso**, **professores aprovam** esses projetos junto às suas instituições, e **visitantes não autenticados podem pesquisar e visualizar os projetos aprovados**. O login para alunos e professores é feito pelo e-mail institucional via MSAL da Microsoft, restrito ao tenant único da CPS, e o papel (aluno ou professor) é decidido pelo **domínio do e-mail, centralizado no back-end** — sem cadastro manual de perfis.

O objetivo é dar visibilidade e curadoria leve ao trabalho feito em classe. Por decisão do orientador, o papel de **gestor foi removido**: não existe login institucional de gestor, tela de instituição como área de trabalho, aprovação de professores pelo gestor nem cadastro de instituição pelo gestor. A secretaria/gestor não tem carga de trabalho; quem mantém o catálogo oficial de instituições é um **admin do sistema** (o próprio dono do projeto, identificado por e-mail via MSAL).

## 2. Target User

### 2.1 Jobs To Be Done
- Aluno: publicar o projeto feito na disciplina/unidade curricular com título, descrição, conteúdo rico, integrantes e professor responsável, obtendo validação de um professor da instituição.
- Professor: selecionar de 0 a 4 instituições onde atua (sem obrigatoriedade no primeiro login); visualizar e aprovar/rejeitar os projetos vinculados às suas instituições.
- Visitante (Não Autenticado): pesquisar e consultar a vitrine de projetos aprovados desenvolvidos pelos estudantes da Fatec/CPS.
- Admin do sistema: manter o catálogo oficial de instituições (dados oficiais pré-populados no boot e curados pelo admin).
- Dono do projeto: rodar a plataforma com carga mínima de secretaria/gestor (orientação do orientador).

### 2.2 Non-Users (v1)
- Pessoas fora do tenant da CPS para postagem e aprovação de projetos (não autenticam).
- Instituições/secretarias operando o sistema como área de trabalho administrativa (papel gestor removido).

### 2.3 Key User Journeys

- **UJ-1. Mariana posta o projeto que nasce aguardando aprovação.**
  - **Persona + contexto:** Mariana, aluna da Fatec, terminou o projeto da disciplina.
  - **Entry state:** não autenticada no sistema; entra pelo navegador.
  - **Path:** clica "Entrar" → autentica no tenant CPS via MSAL → cai no papel aluno (e-mail `@aluno.cps.sp.gov.br`) → acessa "Novo projeto" → preenche dados (título, descrição curta, conteúdo EditorJS, link repositório, imagem capa, palavras-chave, ano publicado), insere o e-mail do professor responsável (com autocomplete de professores já cadastrados no tenant e aceite de e-mail livre — "Professor Convidado"), vincula a instituição e registra os integrantes (nome, LinkedIn) → publica.
  - **Climax:** o projeto é criado diretamente no estado `AGUARDANDO_APROVACAO` com o e-mail do professor responsável registrado.
  - **Resolution:** Mariana acompanha o status nas próximas visitas até que mude para `APROVADO`.
  - **Edge case:** se Mariana tentar publicar sem informar o e-mail do professor responsável ou sem instituição, a ação é bloqueada.

- **UJ-2. Prof. Roberto associa instituições e gerencia a fila.**
  - **Persona + contexto:** Roberto, professor com e-mail `@cps.sp.gov.br`.
  - **Entry state:** autenticado via MSAL; papel professor atribuído pelo domínio.
  - **Path:** acessa a área do professor → pode selecionar de 0 a 4 instituições do catálogo (seleção opcional no primeiro login) → visualiza a fila de projetos das suas instituições vinculadas → aprova ou rejeita com motivo obrigatório.
  - **Climax:** ao aprovar, o projeto muda para `APROVADO`; ao rejeitar, muda para `REJEITADO` registrando obrigatoriamente a justificativa em `motivoRejeicao`.
  - **Resolution:** Roberto pode ajustar suas instituições (de 0 a 4) a qualquer momento.
  - **Edge case:** Roberto tenta vincular a 5ª instituição → recusado pelo limite de 4.

- **UJ-3. Admin cura o catálogo oficial de instituições.**
  - **Persona + contexto:** dono do projeto mantendo o catálogo.
  - **Entry state:** acesso administrativo.
  - **Path:** catálogo pré-populado no boot (seed idempotente a partir do export oficial); admin edita/corrige os dados → disponíveis para seleção.
  - **Climax:** professores conseguem visualizar e selecionar as instituições do catálogo.
  - **Resolution:** catálogo mantido de forma centralizada.

- **UJ-4. Visitante pesquisa projetos no catálogo público.**
  - **Persona + contexto:** visitante externo ou comunidade acadêmica sem login.
  - **Entry state:** não autenticado.
  - **Path:** acessa o portal público → pesquisa por palavra-chave ou filtra por instituição → clica em um projeto.
  - **Climax:** visualiza todos os detalhes do projeto no estado `APROVADO` (título, integrantes, conteúdo rico, imagens, repositório).
  - **Resolution:** consome as informações publicamente sem necessidade de login.

## 3. Glossary

- **Aluno** — Membro do tenant CPS cujo UPN/preferred_username termina em `@aluno.cps.sp.gov.br` (ou outro domínio não-professor do tenant). Pode postar projetos selecionando a instituição e o e-mail do professor responsável.
- **Professor** — Membro do tenant CPS cujo UPN é de domínio `@cps.sp.gov.br`. Pode vincular de 0 a 4 instituições do catálogo e aprovar/rejeitar projetos vinculados a elas.
- **Visitante (Não Autenticado)** — Usuário sem autenticação que pesquisa e visualiza projetos no estado `APROVADO`.
- **Admin do sistema** — Dono do projeto; mantém o catálogo oficial de instituições.
- **Instituição** — Unidade Fatec/CPS do catálogo oficial pré-populado.
- **Projeto** — Publicação com os seguintes atributos: `id` (UUID), `titulo` (string), `descricaoCurta` (string), `conteudoEditorJs` (text/json), `linkRepositorio` (string), `imagemCapaUrl` (string), `palavrasChave` (list de string), `anoPublicado` (int), `estado` (Enum), `motivoRejeicao` (string), `emailProfessorResponsavel` (string), `instituicaoId` (UUID), `criadoEm` e `atualizadoEm` (LocalDateTime). Imagens complementares são **blocos nativos do Editor.js** dentro de `conteudoEditorJs` (não há `imagensExtras`).
- **Integrante** — Membro da equipe do projeto (1 Projeto ── N Integrante) contendo: `id` (UUID), `projetoId` (UUID), `nome` (string) e `linkLinkedin` (string). Sem `papelNoProjeto` no MVP.
- **Professor responsável** — Professor cujo e-mail é indicado no projeto (`emailProfessorResponsavel`). Front-end oferece autocomplete dos professores do tenant; e-mails não cadastrados são aceitos como registro informativo ("Professor Convidado"), sem envio de notificações.
- **Máquina de Estados do Projeto** — Não possui estado RASCUNHO. O projeto nasce diretamente em `AGUARDANDO_APROVACAO`. Estados válidos: `AGUARDANDO_APROVACAO`, `APROVADO` e `REJEITADO`.
- **Aprovação** — Ação do professor que muda o estado para `APROVADO` ou `REJEITADO`. Rejeições exigem obrigatoriamente a justificativa no campo `motivoRejeicao`.

## 4. Features

### 4.1 Autenticação MSAL (tenant único da CPS)

**Description:** Login via MSAL da Microsoft restrito ao tenant da CPS. Realiza UJ-1, UJ-2.

**Functional Requirements:**

#### FR-1: Login com conta CPS
Membro do tenant CPS pode se autenticar na plataforma via MSAL.

#### FR-2: Sessão e logout
Usuário autenticado mantém sessão na aplicação e pode encerrá-la.

### 4.2 Classificação de papel por domínio (regra no back-end)

**Description:** No login, o back-end lê o UPN/preferred_username e classifica: `@aluno.cps.sp.gov.br` → Aluno (checado primeiro); `@cps.sp.gov.br` → Professor; demais → Aluno.

**Functional Requirements:**

#### FR-3: Classificação por domínio de e-mail
O back-end classifica o papel do usuário autenticado pela regra de domínio.

#### FR-4: Papel aplicado em todas as autorizações
Todas as operações protegidas verificam o papel no servidor.

### 4.3 Seleção de instituições pelo professor (0 a 4)

**Description:** O professor pode se vincular a entre 0 e 4 instituições do catálogo oficial. A seleção é opcional no primeiro login e alterável a qualquer momento.

**Functional Requirements:**

#### FR-5: Listar catálogo oficial
O professor vê a lista de instituições disponíveis no catálogo.

#### FR-6: Selecionar de 0 a 4 instituições
O professor associa de 0 a 4 instituições à sua conta. Tentativa de 5ª instituição é rejeitada.

### 4.4 Catálogo de instituições (admin do sistema)

**Description:** O admin cura o catálogo com dados oficiais das instituições CPS, pré-populado no boot via seed idempotente.

**Functional Requirements:**

#### FR-7: Manter catálogo de instituições
O admin pode adicionar/editar/remover instituições do catálogo oficial (CRUD simples no MVP). O catálogo é pré-populado no boot a partir de recursos versionados oficiais (seed idempotente); não há importação em runtime.

### 4.5 Postagem de projeto com e-mail do professor e integrantes

**Description:** Aluno publica projeto com dados completos, e-mail do professor responsável e lista de integrantes. O projeto nasce no estado `AGUARDANDO_APROVACAO`.

**Functional Requirements:**

#### FR-8: Publicar projeto diretamente como AGUARDANDO_APROVACAO
Aluno cria e publica o projeto, que nasce diretamente como `AGUARDANDO_APROVACAO` (sem rascunho).

#### FR-9: Persistir e-mail do professor responsável e integrantes
O projeto exige e persiste `emailProfessorResponsavel` (autocomplete no front-end com aceite de e-mail livre), `instituicaoId`, atributos completos do projeto e a lista de `Integrantes` (nome, linkLinkedin).

### 4.6 Aprovação de projetos

**Description:** Professores aprovam ou rejeitam projetos das suas instituições. Rejeição exige `motivoRejeicao`.

**Functional Requirements:**

#### FR-10: Fila de aprovação do professor
O professor visualiza os projetos vinculados às instituições que selecionou. `emailProfessorResponsavel` do projeto destacado, sem exclusividade de ação.

#### FR-11: Aprovar/rejeitar projeto
**Qualquer professor vinculado à instituição do projeto** altera o estado para `APROVADO` ou `REJEITADO` (decisão de elicitação 2026-09-22, resolução da Open Question §8.2). Quando rejeitado, `motivoRejeicao` é obrigatório.

### 4.7 Acesso Público para Visitantes

**Description:** Visitantes não autenticados podem pesquisar e visualizar detalhes de projetos `APROVADO`.

**Functional Requirements:**

#### FR-12: Pesquisa e visualização pública de projetos aprovados
Visitantes navegam e pesquisam projetos no estado `APROVADO` sem necessidade de login.

### 4.8 Perfil de usuário (aluno e professor)

**Description:** Todo usuário autenticado visualiza seus dados vindos da sessão MSAL (nome, e-mail institucional, papel) e o contexto do seu papel: aluno acessa Meus Projetos; professor visualiza/edita suas instituições vinculadas (0 a 4) e a Fila. Landing pós-login: 1º acesso do professor sem vínculos → Seleção de Instituições; retorno → Fila; aluno → Meus Projetos. (decisão de elicitação 2026-09-22)

**Functional Requirements:**

#### FR-13: Visualizar perfil e contexto do papel
Usuário autenticado visualiza dados do MSAL (somente leitura) e o contexto do papel (aluno: Meus Projetos; professor: instituições vinculadas com edição até 4).

### 4.9 Landing pós-login (fluxo)

**Description:** Direcionamento automático após o retorno do MSAL, diferenciando primeiro acesso (professor sem vínculos) de retorno.

**Functional Requirements:**

#### FR-14: Direcionar papel após login MSAL
Após autenticar, aluno → Meus Projetos; professor sem instituições vinculadas → Seleção de Instituições; professor com vínculos → Fila de Aprovação; admin → Admin Main.

## 5. Non-Goals (Explicit)

- **Papel gestor**: removido por decisão.
- Cadastro/uso de contas fora do tenant CPS para criar/aprovar projetos.
- Estado RASCUNHO para projetos.
- Moderação/disputas (projeto indevido, vinculação errada) — sem dono definido.
- **Notificações/convites por e-mail** (MVP).
- **`papelNoProjeto`** no Integrante (MVP).
- **Upload/importação em runtime** do catálogo de instituições no MVP (endpoint de importação). A carga do catálogo oficial é feita no **boot via seed idempotente** dos recursos versionados; o admin mantém o CRUD simples.

## 6. MVP Scope

### 6.1 In Scope
- Login MSAL restrito ao tenant CPS (FR-1, FR-2).
- Classificação por domínio no back-end (FR-3, FR-4).
- Catálogo de instituições + seleção de 0 a 4 instituições pelo professor (FR-5, FR-6, FR-7).
- Postagem de projeto nascendo em `AGUARDANDO_APROVACAO` com `emailProfessorResponsavel`, atributos completos e `Integrantes` (FR-8, FR-9).
- Fila e aprovação/rejeição com `motivoRejeicao` obrigatório (FR-10, FR-11).
- Acesso público para visitantes pesquisarem e visualizarem projetos `APROVADO` (FR-12).
- Perfil do usuário com contexto do papel e landing pós-login por papel (FR-13, FR-14).

### 6.2 Out of Scope for MVP
- Moderação/disputas com workflow automatizado de arbitragem.

## 7. Success Metrics

- **SM-1**: Fluxo ponta a ponta demonstrável — aluno posta projeto (nasce em `AGUARDANDO_APROVACAO`), professor (com 0 a 4 instituições) aprova, estado passa a `APROVADO` e visitante não autenticado pesquisa/visualiza o projeto no catálogo público.
- **SM-2**: Isolamento do tenant no login.
- **SM-3**: Todo projeto no estado `REJEITADO` possui `motivoRejeicao` preenchido.

## 8. Open Questions

1. O domínio `@cps.sp.gov.br` é exclusivo de professores e `@aluno.cps.sp.gov.br` exclusivo de alunos?
2. ~~Quem aprova: somente o professor com o e-mail responsável ou qualquer professor vinculado à instituição?~~ **Resolvida (elicitação 2026-09-22):** qualquer professor vinculado à instituição do projeto aprova/rejeita; o professor com `emailProfessorResponsavel` fica apenas destacado na fila (FR-10/FR-11).