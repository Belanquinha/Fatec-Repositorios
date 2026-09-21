---
id: SPEC-fatec-repositorios
companions: [roles.md]
sources: []
---

> **Contrato canônico.** Este SPEC e os arquivos em `companions:` são o contrato completo, validado por preservação, do que construir, testar e validar. Os documentos-fonte listados no frontmatter existem para rastreabilidade — consulte-os apenas se precisar de narrativa ou contexto que este contrato omite propositalmente.

# Plataforma Fatec Repositorios (postagem e aprovação de projetos)

## Why

Aplicação web full-stack (front-end + back-end + docker, Angular + Spring + MSAL) para **alunos da Fatec/CPS postarem projetos feitos em sala e no curso**, com **professores aprovando os projetos** — visão para realizar (uma plataforma que o projeto de TCC quer fazer existir). A direção "minimizar ao máximo o trabalho da secretaria/gestor" (pedido do orientador) torna o fluxo de aprovação leve: sem gestor institucional, o professor é dono do processo.

## Capabilities

- **CAP-1** — Login via MSAL (tenant único da CPS)
  - **intent:** Aluno e professor de qualquer unidade Fatec que pertença ao tenant da CPS autenticam-se na plataforma via MSAL da Microsoft.
  - **success:** Uma conta CPS válida completa o login e acessa a aplicação; uma conta de outro tenant Microsoft é rejeitada no login.

- **CAP-2** — Classificação de papel por domínio de e-mail, centralizada no back-end
  - **intent:** O back-end classifica cada usuário autenticado como aluno ou professor a partir do domínio do UPN/preferred_username retornado pelo login MSAL.
  - **success:** Um teste que fornece um token com UPN terminando em `@aluno.cps.sp.gov.br` obtém acesso de aluno; um com `@cps.sp.gov.br` obtém acesso de professor; qualquer outro domínio do tenant CPS cai em aluno (fallback) — tudo decidido no servidor, sem depender de regra no front-end.

- **CAP-3** — Seleção de 0 a 4 instituições pelo professor a partir de catálogo oficial
  - **intent:** O professor escolhe de 0 até 4 instituições de uma lista oficial pré-populada (importada ou curada com dados oficiais) de todas as instituições Fatec/CPS. A seleção não é obrigatória no primeiro login e pode ser feita ou alterada a qualquer momento.
  - **success:** Um professor vincula entre 0 e 4 instituições do catálogo e nunca mais que 4; a seleção inicial é opcional (mínimo 0); não existe fluxo de cadastrar instituição em texto livre.

- **CAP-4** — Postagem de projeto com e-mail do professor responsável, instituição e integrantes
  - **intent:** O aluno, ao postar um projeto, informa o e-mail do professor responsável (com autocomplete no front-end para professores do tenant CPS), vincula a instituição e registra a lista de integrantes do projeto. O projeto nasce diretamente no estado AGUARDANDO_APROVACAO (sem estado RASCUNHO).
  - **success:** Todo projeto postado é salvo diretamente como AGUARDANDO_APROVACAO com `emailProfessorResponsavel` persistido, `instituicaoId` vinculado e lista de integrantes associada.

- **CAP-5** — Aprovação de projetos pelos professores vinculados
  - **intent:** O(s) professor(es) aprovam ou rejeitam os projetos vinculados à(s) sua(s) instituição(ões) selecionada(s).
  - **success:** Uma ação de aprovação/rejeição de um professor muda o estado do projeto (APROVADO ou REJEITADO com `motivoRejeicao` obrigatório) e o novo estado fica visível de forma consistente ao aluno e na listagem.

- **CAP-6** — Entidade Integrante do Projeto
  - **intent:** Permitir que o projeto registre múltiplos integrantes com nome, link do LinkedIn e papel desempenhado no projeto.
  - **success:** Cada integrante é persistido associado ao projeto (Projeto 1 ── N Integrante) com seu id (UUID), nome, linkLinkedin e papelNoProjeto.

- **CAP-7** — Acesso público ao catálogo de projetos para visitantes
  - **intent:** Permitir que usuários não autenticados (visitantes) pesquisem e visualizem detalhes de projetos que estejam no estado APROVADO sem necessidade de login.
  - **success:** Um visitante não autenticado pesquisa projetos aprovados na busca/catálogo público e visualiza seus detalhes completos sem requerer autenticação ou token JWT.

## Constraints

- Login MSAL restrito ao **tenant único da CPS** — se aberto a qualquer conta Microsoft, qualquer pessoa de fora logaria como aluno. (descarta: login multi-tenant/contas pessoais)
- A regra de classificação por domínio é **centralizada e autoritativa no back-end** — nunca confiada no front-end; aluno é quem tem UPN terminando em `@aluno.cps.sp.gov.br` (checado antes), professor quem termina em `@cps.sp.gov.br`; demais membros do tenant → aluno; não há lista separada de professores (o domínio é a lista). (descarta: CRUD de professores, classificação no cliente)
- Limite flexível de **0 a 4 instituições por professor** (mínimo 0, teto 4) — seleção opcional no primeiro login e alterável a qualquer momento. (descarta: vínculo obrigatório no onboarding / vínculo superior a 4)
- Instituições são **dado de catálogo** (lista oficial pré-populada), mantidas por um **admin do sistema**, não por secretaria; professor não cadastra instituição em texto livre. (descarta: cadastro livre de instituição)
- Projeto **não possui estado RASCUNHO** — nasce diretamente como `AGUARDANDO_APROVACAO`. Estados válidos na máquina de estados: `AGUARDANDO_APROVACAO`, `APROVADO`, `REJEITADO`. No estado `REJEITADO`, o campo `motivoRejeicao` é obrigatório.
- O projeto persiste o e-mail do professor responsável (`emailProfessorResponsavel: string`), além de `instituicaoId`, `titulo`, `descricaoCurta`, `conteudoEditorJs`, `linkRepositorio`, `imagemCapaUrl`, `imagensExtras`, `palavrasChave`, `anoPublicado`, `criadoEm` e `atualizadoEm`.
- Visitantes não autenticados têm acesso somente leitura aos projetos no estado `APROVADO`.

## Non-goals

- Papel **gestor**: não existe login institucional de gestor, tela de instituição como área de trabalho de papel, aprovação de professores pelo gestor nem cadastro de instituição pelo gestor — papel removido por decisão (minimizar trabalho de secretaria/gestor).
- Cadastro/suporte de usuários fora do tenant da CPS para postagem ou aprovação de projetos.
- Edição ou criação de projetos por visitantes não autenticados.
- Estado RASCUNHO para projetos.

## Success signal

Um professor real da CPS faz login e associa de 0 a 4 instituições; um aluno real do mesmo tenant posta um projeto informando o e-mail do professor responsável; o projeto nasce como `AGUARDANDO_APROVACAO`; o professor aprova o projeto na fila da sua instituição e o estado muda para `APROVADO`. Um visitante não autenticado acessa o catálogo público, pesquisa e visualiza os detalhes do projeto aprovado. Tentativas de login de conta fora do tenant falham. Isso é demonstrável de ponta a ponta sobre a pilha (front + back + docker) do repositório.

## Assumptions

- O domínio de classificação é lido do `preferred_username`/UPN retornado pelo MSAL; se a fonte real do e-mail diferir, a regra de CAP-2 deve ser ajustada.
- Sufixos de domínio confirmados pelo usuário: aluno `@aluno.cps.sp.gov.br`, professor `@cps.sp.gov.br` (checagem de aluno antes de professor).

## Open Questions

- Os domínios `@cps.sp.gov.br` (professor) e `@aluno.cps.sp.gov.br` (aluno) são **exclusivos** na prática — nenhum aluno/estagiário/secretaria no primeiro, nenhum professor no segundo? Se quebrar, a regra de classificação precisa de segunda validação.
- Quem aprova o projeto: **somente o professor cujo e-mail é o responsável ou qualquer professor da instituição vinculada**?