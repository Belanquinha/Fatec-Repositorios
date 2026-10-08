---
id: SPEC-exclusao-projetos
companions: []
sources:
  - ../spec-fatec-repositorios/SPEC.md
  - ../spec-fatec-repositorios/roles.md
  - ../../planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - ../../planning-artifacts/epics.md
status: draft
created: 2026-10-07
author: Mary (bmad-agent-analyst)
decisions_locked:
  - professor-autor: OUT (pós-MVP, sem confirmação do coordenador)
  - quarentena-7d: OUT
  - tombstone-publico: OUT (404 + log interno)
---

# SPEC — Exclusão e Anonimização de Projetos (Recomendação 1 rev.1.1)

> **Contrato delta.** Não reescreve o SPEC canônico (`spec-fatec-repositorios/SPEC.md`). Adiciona o que falta: saída de projeto após `APROVADO` sem violar LGPD, no caminho mais simples defensável em banca.

## Why

Hoje não existe saída (evidência):

- `back-end/.../controller/ProjetoController.java:32-93` — só `POST`, `GET /publicos`, `GET /{id}`, `GET /meus`. Zero `DELETE`, zero `solicitar-remocao`.
- `back-end/.../service/ProjetoService.java:35-107` — só `criar`, `obterPorId`, `obterMeusProjetos`, `listarPublicos`.
- `back-end/.../model/Projeto.java:48-50` — estados travados em `AGUARDANDO_APROVACAO`, `APROVADO`, `REJEITADO`.
- Docs jogaram para fora: `prd.md:182` — `Moderação/disputas — sem dono definido`; `prd.md:199` — out-of-scope.

Sem isso o aluno/professor não tem como pedir "tira meu nome/projeto do ar", o que colide com LGPD Art.18 (direito de eliminação/bloqueio/anonimização a pedido) e gera pergunta certa da banca.

Solução mínima: **2 regras por estado, 3 endpoints, 2 estados novos, sem tombstone público, sem quarentena, sem professor-autor.**

## Capabilities

- **CAP-E1 — Exclusão direta do não-público (autor, sem curadoria)**
  - **intent:** Autor apaga sozinho projeto que nunca foi público (`AGUARDANDO_APROVACAO` ou `REJEITADO`).
  - **success:** Após `DELETE`, `GET /projetos/meus` não lista, `GET /projetos/{id}` retorna 404, nada mudou no catálogo público (nunca esteve lá).

- **CAP-E2 — Remoção de projeto aprovado via pedido + confirmação docente (LGPD-safe)**
  - **intent:** Autor pede saída de projeto `APROVADO`; professor vinculado à instituição confirma ou nega. Efeito público imediato (bloqueio), anonimização de PII se deferido.
  - **success:** No `T0` do pedido o projeto some de `GET /projetos/publicos` e `GET /projetos/{id}` público passa a 404. Se deferido, PII é nulificada e `estado=REMOVIDO`; se indeferido, volta a `APROVADO` com motivo visível ao autor.

## Constraints

- Postagem segue **só aluno** (sem mudança). `roles.md:7-8` vigente. Professor-autor explicitamente OUT deste SPEC. Brecha atual `SecurityConfig.java:59` (`POST /projetos → authenticated()` sem `hasRole`) deve ser fechada em outro SPEC com `hasRole(ALUNO)` — registrada aqui como dependência, não escopo.
- Máquina de estados passa a: `AGUARDANDO_APROVACAO`, `APROVADO`, `REJEITADO` (existentes) + `EM_REMOCAO` (transitório, invisível no público) + `REMOVIDO` (terminal, invisível no público). Sem `RASCUNHO` (mantido), sem `ARQUIVADO`.
- Transições permitidas:
  ```
  AGUARDANDO_APROVACAO --DELETE autor--> (apagado físico)
  REJEITADO --DELETE autor--> (apagado físico)
  APROVADO --POST solicitar-remocao (autor)--> EM_REMOCAO (bloqueio público imediato)
  EM_REMOCAO --POST julgar-remocao deferido (professor vinculado)--> REMOVIDO + anonimiza
  EM_REMOCAO --POST julgar-remocao indeferido--> APROVADO (com motivoJulgamento visível ao autor)
  ```
- `APROVADO --DELETE direto--> 409`. Nunca hard-delete de público por único ator.
- Julgador nunca é o autor: se `autorId == julgadorId → 403` (trava anti-auto-aprovação, já prepara futuro professor-autor sem custo).
- Julgador é **qualquer professor vinculado à `instituicaoId` do projeto** (mesma regra de `FR-11` — qualquer vinculado aprova). Fallback: `ADMIN` pode julgar se sem professor vinculado disponível.
- Anonimização no deferimento (LGPD Art.18 IV/VI + Art.16 — conserva mínimo não-PII para auditoria):
  - `Integrante.nome → "[removido]"`, `Integrante.linkLinkedin → null`
  - `Projeto.emailProfessorResponsavel → null`
  - `Projeto.imagemCapaUrl → null` (arquivo removido do `FileStorageService`)
  - Mantém: `id`, `titulo`, `instituicaoId`, `anoPublicado`, `estado=REMOVIDO`, `removidoEm`, `motivoRemocaoGenerico` (enum genérico, ex: `PEDIDO_AUTOR`, sem PII)
  - `conteudoEditorJs`, `descricaoCurta`, `linkRepositorio`, `palavrasChave` → nulificados (contêm PII potencial e obra; sem versionamento no MVP não há como republicar parcial com segurança)
- Sem tombstone público: `GET /projetos/{id}` e `GET /projetos/publicos` filtram `estado == APROVADO` apenas. `EM_REMOCAO`/`REMOVIDO` → 404 público. Log interno (`removidoEm`, `motivoGenerico`, `julgadoPor`) visível só a `ADMIN`/dono para Art.16 e defesa em banca — não exposto em API pública.
- `motivoSolicitacao` (texto livre do autor) e `motivoJulgamento` (texto do professor) obrigatórios; `motivoJulgamento` indeferido aparece inline ao autor (padrão `motivoRejeicao`).

## Non-goals

- Professor postando projeto (OUT — pós-MVP, aguarda coordenador).
- Quarentena de 7 dias / embargo temporário (OUT).
- Tombstone público `410 + plaquinha` (OUT — trocado por 404 + log interno; débito assumido de citabilidade).
- Desvinculação individual de um `Integrante` em projeto `APROVADO` mantendo obra no ar (OUT — cobre via remoção do projeto inteiro neste MVP; virar `FR-17` futura).
- Notificações por e-mail ao professor/integrante (mantém non-goal do SPEC canônico).
- Versionamento, DOI/handle, auditoria pública (padrão Zenodo/RIC.CPS — pós-TCC).
- Hard-delete físico de `APROVADO` (proibido).

## API Contract (delta)

```
DELETE /projetos/{id} — authenticated (autor)
  204 se AGUARDANDO_APROVACAO | REJEITADO + dono → apaga cascata (projeto, integrantes, palavrasChave) + arquivos capa
  403 se não-dono
  404 se inexistente ou já REMOVIDO
  409 se APROVADO | EM_REMOCAO {codigo:409, mensagem:"Projeto público: use solicitar-remocao"}

POST /projetos/{id}/solicitar-remocao — authenticated (autor)
  body: { "motivo": string(1..2000) obrigatório }
  200 {estado: EM_REMOCAO} se APROVADO + dono → seta solicitadoPor, solicitadoEm, motivoSolicitacao; some do público no mesmo commit
  403 se não-dono
  409 se não-APROVADO (use DELETE direto)
  409 se já EM_REMOCAO (idempotente: retorna estado atual)

POST /projetos/{id}/julgar-remocao — authenticated (PROFESSOR vinculado à instituicaoId OU ADMIN)
  body: { "deferido": boolean, "motivo": string obrigatório }
  200 {estado: REMOVIDO | APROVADO}
  403 se autor == julgador, ou professor sem vínculo com a instituição, ou aluno
  404 se inexistente
  409 se não está EM_REMOCAO
```

`SecurityConfig` delta: `DELETE /projetos/{id} → authenticated()`, `POST /projetos/*/solicitar-remocao → authenticated()`, `POST /projetos/*/julgar-remocao → hasAnyRole(PROFESSOR, ADMIN)` + checagem de vínculo em serviço (não só papel).

## Modelo (delta em `Projeto`)

```java
// novos persistidos (nullable para linhas legadas):
ProjetoEstado estado; // + EM_REMOCAO, REMOVIDO
String motivoSolicitacao;      // TEXT, nullable
UUID solicitadoPor;            // nullable
LocalDateTime solicitadoEm;    // nullable
UUID julgadoPor;               // nullable
LocalDateTime julgadoEm;       // nullable
String motivoJulgamento;       // TEXT, nullable
LocalDateTime removidoEm;      // nullable
String motivoRemocaoGenerico;  // enum-string: PEDIDO_AUTOR | DADOS_PESSOAIS | ERRO_AUTORIA | OUTRO
```

`GET /projetos/publicos` e `GET /projetos/{id}` público: `WHERE estado = APROVADO` (trocar `findByEstado(APROVADO)` já existente — sem novo índice no MVP).

## Success signal

1. Aluno deleta `AGUARDANDO` → some de `/meus`, `GET /{id}` → 404, público inalterado.
2. Aluno tenta `DELETE` em `APROVADO` → 409 com mensagem orientando `solicitar-remocao`.
3. Aluno `solicita` em `APROVADO` → em <1s some de `/publicos` e `GET /{id}` público → 404; professor vê `EM_REMOCAO` na fila.
4. Professor vinculado `defere` → PII nula (nome/linkedin/e-mail/capa), `estado=REMOVIDO`, público segue 404, admin vê log interno.
5. Professor `indefere` → volta `APROVADO` visível + motivo inline ao autor.
6. Conta fora do tenant segue 401; aluno tentando julgar → 403.

## Assumptions

- Fluxo base de aprovação (`Story 4.2`) ainda não codado — este SPEC assume que `APROVADO` existirá via aquele fluxo; ordem de build: aprovação antes de remoção.
- `autor` é sempre `ALUNO` neste MVP (professor-autor OUT).
- Front `/perfil` exibe `EM_REMOCAO` como "Remoção em análise" e `REMOVIDO` como terminal (sem CTA).

## Open Questions

- O coordenador aceita `REMOVIDO` com `titulo` preservado internamente (auditoria Art.16) ou exige purga total da linha (hard-delete após anonimização)? Default aqui: preserva mínimo interno.
- `ADMIN` julga sozinho se nenhum professor vinculado responder? Default aqui: sim (evita carga de secretaria parada).
- Prazo de resposta do professor (SLA)? Default: sem SLA no MVP; `EM_REMOCAO` fica oculto até julgamento (sem auto-defere — auto-defere era da quarentena, removido).
