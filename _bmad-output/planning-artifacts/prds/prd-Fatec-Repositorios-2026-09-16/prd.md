---
title: Fatec Repositorios
created: 2026-09-16
updated: 2026-09-16
status: final
---

# PRD: Fatec Repositorios
*Working title — confirm. Plataforma web full-stack para alunos Fatec/CPS postarem projetos e professores aprovarem.*

## 0. Document Purpose

Este PRD define o produto **Fatec Repositorios** para o time do TCC, o dono do projeto e os fluxos de planejamento a jusante (arquitetura, epics/histórias, UX). Estrutura-se pelo glossário na seção 3, com features agrupadas e requisitos funcionais (FR) numerados globalmente. Baseia-se no contrato canônico já aprovado **SPEC-fatec-repositorios** (`_bmad-output/specs/spec-fatec-repositorios/`), inclusive na matriz de papéis `roles.md`; este PRD não duplica o SPEC, refina-o em requisitos testáveis. Toda decisão de negócio ("não reabrir") vem do SPEC.

## 1. Vision

Uma aplicação web (front-end Angular + back-end Spring + docker) onde **alunos da Fatec/CPS publicam projetos construídos em sala e no curso**, e **professores aprovam** esses projetos junto às suas instituições. O login é feito pelo e-mail institucional via MSAL da Microsoft, restrito ao tenant único da CPS, e o papel (aluno ou professor) é decidido pelo **domínio do e-mail, centralizado no back-end** — sem cadastro manual de perfis.

O objetivo é dar visibilidade e curadoria leve ao trabalho feito em classe. Por decisão do orientador, o papel de **gestor foi removido**: não existe login institucional de gestor, tela de instituição como área de trabalho, aprovação de professores pelo gestor nem cadastro de instituição pelo gestor. A secretaria/gestor não tem carga de trabalho; quem mantém o catálogo oficial de instituições é um **admin do sistema** (o próprio dono do projeto).

## 2. Target User

### 2.1 Jobs To Be Done
- Aluno: publicar o projeto que fiz na disciplina/unidade curricular e ter validação de um professor da instituição.
- Professor: mostrar/validar o que é produzido nas suas instituições; aprovar/rejeitar projetos vinculados.
- Admin do sistema: manter o catálogo oficial de instituições (dados importados ou curados).
- Dono do projeto: rodar a plataforma com carga mínima de secretaria/gestor (orientação do orientador).

### 2.2 Non-Users (v1)
- Pessoas fora do tenant da CPS (não autenticam de jeito nenhum).
- Instituições/secretarias operando o sistema como área de trabalho administrativa (papel gestor removido).

### 2.3 Key User Journeys

- **UJ-1. Mariana posta o projeto e já sabe quem vai aprovar.**
  - **Persona + contexto:** Mariana, aluna da Fatec, terminou o projeto da disciplina com a turma.
  - **Entry state:** não autenticada no sistema; entra pelo navegador.
  - **Path:** clica "Entrar" → autentica no tenant CPS via MSAL → cai no papel aluno (e-mail não `@cps`) → acessa "Novo projeto" → preenche dados → seleciona o professor responsável da lista → publica.
  - **Climax:** o projeto aparece publicado com o professor responsável definido; Mariana vê o estado "aguardando aprovação".
  - **Resolution:** Mariana acompanha o status nas próximas visitas até "aprovado".
  - **Edge case:** se Mariana não selecionar professor responsável, a publicação fica bloqueada (dono imediato obrigatório).

- **UJ-2. Prof. Roberto associa as instituições e aprova a fila.**
  - **Persona + contexto:** Roberto, professor com e-mail `@cps`, ministra em duas unidades.
  - **Entry state:** já autenticado via MSAL; papel professor atribuído pelo domínio.
  - **Path:** primeira visita → seleciona de 1 a 4 instituições do catálogo oficial → entra na fila de aprovação → vê projetos vinculados → aprova ou rejeita cada um com justificativa.
  - **Climax:** um projeto muda para "aprovado" e o aluno vê o novo estado.
  - **Resolution:** Roberto continua gerenciando a fila; pode ajustar suas instituições (dentro do limite de 4) quando quiser.
  - **Edge case:** Roberto tenta adicionar uma 5ª instituição → recusado pelo limite.

- **UJ-3. Admin importa a lista oficial de instituições.**
  - **Persona + contexto:** dono do projeto mantendo o catálogo.
  - **Entry state:** acesso administrativo.
  - **Path:** importa/edita a lista oficial das instituições CPS → dados ficam disponíveis aos professores nas seleções.
  - **Climax:** um professor consegue selecionar as instituições corretas recém-atualizadas.
  - **Resolution:** catálogo mantido de forma central; nenhum professor cadastra instituição em texto livre.

## 3. Glossary

- **Aluno** — Membro do tenant CPS cujo UPN/preferred_username termina em `@aluno.cps.sp.gov.br` (ou outro domínio não-professor do tenant). Pode postar projetos e selecionar o professor responsável. (Papel determinado pelo back-end; regra de aluno checada antes da de professor.)
- **Professor** — Membro do tenant CPS cujo UPN é de domínio `@cps.sp.gov.br` (funcional/coorporativo). Pode selecionar até 4 instituições do catálogo e aprovar/rejeitar projetos vinculados a elas. Não há lista separada de professores — o domínio É a lista.
- **Admin do sistema** — Dono do projeto; mantém o catálogo oficial de instituições (importar/curar). Não faz login via fluxo de aluno/professor.
- **Instituição** — Unidade Fatec/CPS do catálogo oficial pré-populado. Dado de catálogo; nunca texto livre do usuário.
- **Projeto** — Publicação do aluno (título, descrição, material/links, instituição) com um **professor responsável** obrigatório escolhido no post.
- **Professor responsável** — Professor que o aluno seleciona ao postar; dono imediato do projeto e default para aprovação (decisão de emitente em aberto — ver §8).
- **Aprovação** — Ação do professor que muda o estado do projeto para Aprovado/Rejeitado; quando Rejeitado, espera-se motivo.
- **Catálogo de instituições** — Lista oficial de instituições CPS, mantida pelo admin do sistema.

## 4. Features

### 4.1 Autenticação MSAL (tenant único da CPS)

**Description:** Login via MSAL da Microsoft restrito ao tenant da CPS. Qualquer conta de outro tenant/contas pessoais não acessam. Realiza UJ-1, UJ-2. `[ASSUMPTION: o gerenciamento de sessão/logout segue o padrão padrão do MSAL Angular (SPA) com token no back-end]`.

**Functional Requirements:**

#### FR-1: Login com conta CPS
Membro do tenant CPS pode se autenticar na plataforma via MSAL. Realiza UJ-1, UJ-2.

**Consequences (testable):**
- Um teste com conta CPS autentica e retorna token/sessão válido.
- Conta de outro tenant Microsoft é rejeitada sem estabelecer sessão.

**Out of Scope:**
- Login com contas pessoais Microsoft / outros tenants.

#### FR-2: Sessão e logout
Usuário autenticado mantém sessão na aplicação e pode encerrá-la.

**Consequences (testable):**
- Após logout, acesso autenticado é revogado; recurso protegido retorna 401 sem token válido.

### 4.2 Classificação de papel por domínio (regra no back-end)

**Description:** No login, o back-end lê o UPN/preferred_username e classifica por sufixo de domínio, **nesta ordem**: termina em `@aluno.cps.sp.gov.br` → Aluno; termina em `@cps.sp.gov.br` → Professor; demais domínios do tenant → Aluno (fallback). A regra é autoritativa no back-end, nunca confiada no front. Realiza UJ-1, UJ-2. `[ASSUMPTION: o token MSAL expõe o UPN no campo preferred_username mesmo para usuários de domínio @aluno.cps.sp.gov.br e @cps.sp.gov.br — confirmar formato real na integração]`.

**Functional Requirements:**

#### FR-3: Classificação por domínio de e-mail
O back-end classifica o papel do usuário autenticado pela regra de domínio, com aluno checado antes de professor. Realiza UJ-1, UJ-2.

**Consequences (testable):**
- UPN terminando em `@aluno.cps.sp.gov.br` recebe papel Aluno (mesmo que termine em `.cps.sp.gov.br`).
- UPN terminando em `@cps.sp.gov.br` (sem prefixo `aluno.`) recebe papel Professor.
- UPN de outro domínio do tenant CPS recebe papel Aluno (fallback).
- A resposta de classificação é gerada no servidor; um front adulterado não altera o papel.

**Out of Scope:**
- Lista/CRUD separado de professores; classificação baseada em outra fonte.

#### FR-4: Papel aplicado em todas as autorizações
Todas as operações protegidas verificam o papel no servidor.

**Consequences (testable):**
- Aluno tentando ação de professor (ex.: aprovar projeto) recebe 403.

### 4.3 Seleção de instituições pelo professor (1 a 4)

**Description:** O professor escolhe de 1 até 4 instituições a partir do catálogo oficial pré-populado. Não digita instituição em texto livre. O sistema reforça o limite. Realiza UJ-2.

**Functional Requirements:**

#### FR-5: Listar catálogo oficial
O professor vê a lista de instituições disponíveis no catálogo.

**Consequences (testable):**
- A lista retornada corresponde ao catálogo administrado; não há campo de criação livre.

#### FR-6: Selecionar até 4 instituições
O professor associa 1 a 4 instituições e pode alterar a seleção.

**Consequences (testable):**
- Seleção entre 1 e 4 salva com sucesso.
- Tentativa de 5ª instituição é recusada com mensagem clara.

**Out of Scope:**
- Instituições fora do catálogo; cadastro de instituição pelo professor.

### 4.4 Catálogo de instituições (admin do sistema)

**Description:** O admin importa ou cura o catálogo com dados oficiais de todas as instituições CPS. Realiza UJ-3. `[ASSUMPTION: importação pode ser em lote (ex.: arquivo) e edição manual; formato concreto fica para arquitetura]`.

**Functional Requirements:**

#### FR-7: Manter catálogo de instituições
O admin pode adicionar/editar/remover instituições do catálogo oficial.

**Consequences (testable):**
- Alterações do admin refletem-se na seleção dos professores (FR-5/FR-6).
- Sem papel admin, alteração de catálogo é negada.

### 4.5 Postagem de projeto com professor responsável

**Description:** O aluno publica um projeto (dados + material) e **seleciona o professor responsável na hora do post** — o projeto nasce com dono imediato. Realiza UJ-1.

**Functional Requirements:**

#### FR-8: Publicar projeto
Aluno autenticado pode criar/editar um projeto de disciplina e publicá-lo.

**Consequences (testable):**
- Projeto publicado fica visível na listagem com estado inicial.

#### FR-9: Selecionar professor responsável obrigatório
Ao publicar, o aluno seleciona um professor responsável; sem ele o projeto não publica.

**Consequences (testable):**
- Tentativa de publicar sem professor responsável falha e indica o campo.
- Todo projeto persistido possui professor responsável não nulo.

**Out of Scope:**
- Projeto "solto para a instituição" sem responsável.

### 4.6 Aprovação de projetos

**Description:** Professor(es) aprovam/rejeitam projetos vinculados às suas instituições selecionadas. O estado muda e fica visível ao aluno. Realiza UJ-2. Decisão de emitente da aprovação (somente responsável vs. qualquer professor da instituição) fica em aberto — §8 (recomendado: somente o professor responsável).

**Functional Requirements:**

#### FR-10: Fila de aprovação do professor
O professor vê os projetos vinculados às suas instituições, destacando os que têm ele como responsável.

**Consequences (testable):**
- A fila lista projetos das instituições selecionadas; projeto com responsável = professor aparece em primeiro plano.

#### FR-11: Aprovar/rejeitar projeto
O professor altera o estado do projeto para Aprovado ou Rejeitado (com motivo).

**Consequences (testable):**
- Estado do projeto persiste e muda de "aguardando" para "aprovado"/"rejeitado".
- Rejeição registra justificativa.

## 5. Non-Goals (Explicit)

- **Papel gestor**: login institucional de gestor, tela de instituição como área de trabalho, aprovação de professores pelo gestor, cadastro de instituição pelo gestor — removido por decisão.
- Cadastro/uso por pessoas fora do tenant da CPS.
- Cadastro textual livre de instituições por professores.
- Moderação/disputas (projeto indevido, vinculação errada) — sem dono definido (decisão consciente).
- Mecanismo de comunicação/mensagens entre aluno e professor.

## 6. MVP Scope

### 6.1 In Scope
- Login MSAL restrito ao tenant CPS (FR-1, FR-2).
- Classificação por domínio no back-end (FR-3, FR-4).
- Catálogo de instituições + seleção 1–4 (FR-5, FR-6, FR-7).
- Postagem com professor responsável (FR-8, FR-9).
- Fila e aprovação de projetos (FR-10, FR-11).

### 6.2 Out of Scope for MVP
- Moderação/disputas — decisão de dono pendente (§8).
- Aprovação por qualquer professor da instituição — aguarda decisão de emitente (§8).
- Exibição pública/portfólio aberto para não autenticados — decidir depois.

## 7. Success Metrics

**Primary**
- **SM-1**: Fluxo ponta a ponta demonstrável — professor CPS autentica, associa até 4 instituições, aluno posta com professor responsável, professor aprova e o estado aparece ao aluno. (Valida FR-1, FR-3, FR-5, FR-9, FR-11.)
- **SM-2**: Isolamento do tenant — conta fora da CPS não autentica (teste automatizado). (Valida FR-1.)

**Secondary**
- **SM-3**: Todo projeto persistido tem professor responsável não nulo (invariante verificado em teste). (Valida FR-9.)

**Counter-metrics (do not optimize)**
- **SM-C1**: Reduzir passos/tempo de aprovação não é meta — aprovação é curadoria leve e fundamentada; otimizar velocidade poderia encorajar aprovações sem análise.

## 8. Open Questions

1. O domínio `@cps.sp.gov.br` é exclusivo de professores na prática (nenhum aluno/estagiário/secretaria usa)? E `@aluno.cps.sp.gov.br` é exclusivo de alunos? Se quebrar, a classificação precisa de segunda validação.
2. Quem aprova: somente o professor responsável (recomendado) ou qualquer professor da instituição?
3. Dono da moderação/disputas (projeto indevido, vinculação errada)? Hoje "ninguém", decisão consciente.

## 9. Assumptions Index

- §4.1 (seção Autenticação) — gerenciamento de sessão/logout segue padrão MSAL Angular (SPA) com token no back-end.
- §4.2 (FR-3) — UPN disponível em `preferred_username` do token MSAL; sufixos confirmados pelo usuário: aluno `@aluno.cps.sp.gov.br`, professor `@cps.sp.gov.br`.
- §4.4 (FR-7) — importação em lote + edição manual; formato concreto fica para arquitetura.