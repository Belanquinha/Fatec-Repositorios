---
id: SPEC-exclusao-projetos
companions: []
sources:
  - ../spec-fatec-repositorios/SPEC.md
  - ../spec-fatec-repositorios/roles.md
  - ../../planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - ../../planning-artifacts/epics.md
status: ready
created: 2026-10-07
updated: 2026-11-20
author: Mary (bmad-agent-analyst) · rev 1.2
decisions_locked:
  - nao-publico-hard-delete: IN (somente AGUARDANDO_APROVACAO e REJEITADO, pelo autor)
  - aprovado-soft-delete: IN (REMOVIDO + anonimização imediata, pelo autor)
  - reversao-pelo-autor: IN (aba "Removidos", até 14 dias)
  - retencao-dados-originais-14d: IN (guarda PII para reversão; purge apaga linha + arquivos)
  - admin-fora-do-fluxo: IN (admin não exclui projeto)
  - professora-autor: OUT (registrar que o código hoje permite; não mudar)
  - tombstone-publico: OUT (404 + registro interno)
  - integrante-remove-nome: OUT (fora do MVP)
  - devolucao-reenviado: IN (fase 2; ALTERACOES_SOLICITADAS -> REENVIADO)
  - conta-anonimiza-aprovados: IN (fase 2; manter a obra)
  - email-fora-da-spec: IN (detalhe posterior)
---

# SPEC — Exclusão, Anonimização, Devolução e Exclusão de Conta (rev 1.2)

> **Contrato delta.** Não reescreve o SPEC canônico (`spec-fatec-repositorios/SPEC.md`).
> Acrescenta a saída de projeto após `APROVADO` e o ciclo de devolução/edição, sem violar a LGPD.
> **Status:** pronta para desenvolvimento, em **2 fases** (ver §8). A fase 2 depende da fase 1
> apenas na parte de exclusão de conta; a devolução/edição é independente.

## 1. Why

Hoje o projeto não tem saída depois de público (evidência):

- `back-end/.../controller/ProjetoController.java:37-171` — só `POST`, `GET /publicos`,
  `GET /pendentes`, `GET /{id}`, `GET /meus`, `PATCH /aprovar`, `PATCH /rejeitar`.
  Zero `DELETE`, zero devolução, zero edição.
- `back-end/.../service/ProjetoService.java` — só `criar`, `obterPorId`, `obterMeusProjetos`,
  `listarPublicos`, `listarPendentesParaProfessor`, `aprovar`, `rejeitar`.
- `back-end/.../model/ProjetoEstado.java` — `ALTERACOES_SOLICITADAS` e `REENVIADO` existem no
  enum e no model do front (`front-end/.../core/models/projeto.model.ts:64`), mas **nenhum código
  do back os produz**; o front os trata como "em análise" (`perfil.ts:46-50`).
- `FileStorageService.java` — só `salvarArquivo`; **não existe** método de deleção de arquivo.
- Não existe `@Scheduled` no projeto (só `@EnableAsync` em `AsyncConfig.java:26`).
- O único delete de projeto hoje é o seed em modo `replace`, por título, sem limpar arquivo
  (`ProjetoSeedLoader.java:78`).

Sem isso o autor não consegue "tirar meu projeto do ar", o que colide com a LGPD (Art. 18, VI —
eliminação/bloqueio/anonimização a pedido) e gera pergunta certa em banca. A futura exclusão de
conta depende dessas saídas.

## 2. Decisões travadas (rev 1.2)

1. **Não-público** (`AGUARDANDO_APROVACAO`, `REJEITADO`): hard delete pelo autor, sem curadoria.
2. **Público** (`APROVADO`): soft delete (`REMOVIDO`) + anonimização **imediata** pelo autor,
   com reversão pelo próprio autor em até **14 dias**.
3. **Retenção:** os dados originais (PII) ficam guardados em `dadosOriginais` por 14 dias, para a
   reversão; o **purge** apaga linha **e** arquivos. A PII nunca é exposta em API.
4. **Admin** não exclui projeto (peça secundária). A reversão é do autor.
5. **Integrante** (não tem conta) ficar sem remoção de nome: fora do MVP.
6. **Devolução:** professor devolve com motivo -> `ALTERACOES_SOLICITADAS`; autor edita e reenvia
   -> `REENVIADO` (o professor vê que o projeto voltou). E-mail de aviso fica fora desta spec.
7. **Conta:** anonimizar os `APROVADOS` e excluir os não-aprovados; manter a obra; devolver ao
   aluno os pendentes em que o usuário é professor responsável, **exigindo novo responsável**.
8. **Limpeza de arquivos:** implementada (capa, thumb e imagens inline).

## 3. Fase 1 — Exclusão e anonimização de projeto

### 3.1 Capacidades

- **CAP-E1 — Hard delete de não-público (autor).**
  - **intent:** o autor apaga um projeto que nunca foi público.
  - **success:** após o `DELETE`, `GET /projetos/{id}` -> 404, `/meus` não lista, o catálogo
    público não muda (nunca esteve lá), integrantes/palavras-chave e arquivos são removidos.

- **CAP-E2 — Remoção de projeto aprovado (autor), com anonimização imediata.**
  - **intent:** o autor tira o projeto do ar sem apagar a obra para sempre.
  - **success:** no mesmo commit o projeto some de `/publicos` e o detalhe público -> 404; a PII
    é nulificada; o registro fica `REMOVIDO`, com os dados originais guardados para reversão.

- **CAP-E3 — Reversão pelo autor.**
  - **intent:** o autor se arrepende e repõe o projeto no ar.
  - **success:** dentro de 14 dias, `POST /projetos/{id}/reverter` restaura estado e PII; fora do
    prazo, 409.

- **CAP-E4 — Purge.**
  - **intent:** cumprir a LGPD depois da janela de reversão.
  - **success:** job diário apaga a linha e os arquivos de `REMOVIDO` com `removidoEm` > 14 dias.

### 3.2 Máquina de estados (fase 1)

```
AGUARDANDO_APROVACAO --DELETE autor--> (hard delete)
REJEITADO            --DELETE autor--> (hard delete)
APROVADO             --DELETE autor--> REMOVIDO (some do público, anonimiza, guarda original)
REMOVIDO             --POST reverter autor--> APROVADO (restaura)
REMOVIDO             --purge (>14d)--> (hard delete)
```

`SUBMETIDO`, `ALTERACOES_SOLICITADAS` e `REENVIADO` não são produzidos nesta fase.

### 3.3 Contrato de API (fase 1)

Envelope de erro: `{ "codigo": <int>, "mensagem": <string> }` (padrão `GlobalExceptionHandler`).

```
DELETE /projetos/{id}
  auth: bearer JWT; só o autor
  body opcional: { "motivoRemocao": "PEDIDO_AUTOR" | "DADOS_PESSOAIS" | "ERRO_AUTORIA" | "OUTRO" }
  200 { "id", "titulo", "estado": "REMOVIDO", "removidoEm" }   — quando APROVADO (soft)
      ou { "id", "titulo", "estado": "EXCLUIDO" }             — quando não-público (hard)
  401 sem token
  403 autenticado que não é o autor
  404 inexistente ou já REMOVIDO
  efeitos:
    - não-público: hard delete (linha, integrantes, palavras_chave) + arquivos
    - APROVADO: estado=REMOVIDO, removidoEm, removidoPorId, motivoRemocaoGenerico,
      estadoAnterior=APROVADO, anonimiza (§3.5), original em dadosOriginais; arquivos mantidos
      (precisam para a reversão)

GET /projetos/removidos
  auth: bearer JWT
  200 [ { "id", "titulo", "estadoAnterior", "removidoEm", "motivoRemocaoGenerico",
          "diasParaExpirar" } ]   — nunca PII
  401 sem token
  note: declarar esta rota como autenticada ANTES do matcher genérico
       `GET /projetos/**` -> permitAll em SecurityConfig, senão vaza lista.

POST /projetos/{id}/reverter
  auth: bearer JWT; só o autor
  200 projeto restaurado (mesmo payload de ProjetoResponse)
  401 sem token
  403 não é o autor
  404 inexistente ou não está REMOVIDO
  409 removidoEm com mais de 14 dias: { "codigo": 409,
       "mensagem": "Prazo de reversão expirado; o projeto será removido definitivamente" }
```

### 3.4 Modelo (delta em `Projeto`)

```java
// enum
ProjetoEstado + REMOVIDO

// campos novos (nullable para linhas legadas)
LocalDateTime removidoEm;
UUID removidoPorId;
String motivoRemocaoGenerico;      // PEDIDO_AUTOR | DADOS_PESSOAIS | ERRO_AUTORIA | OUTRO
@Enumerated(EnumType.STRING) ProjetoEstado estadoAnterior;
@Column(columnDefinition = "TEXT") String dadosOriginais;  // JSON interno, NUNCA exposto
```

`dadosOriginais` guarda, para a reversão: `integrantes` (nome + linkLinkedin),
`emailProfessorResponsavel`, `imagemCapaUrl`, `conteudoEditorJs`, `descricaoCurta`,
`linkRepositorio`, `palavrasChave` e `estadoAnterior`.

### 3.5 Anonimização em T0 (exato)

Nulificar / limpar:

- `integrantes`: `nome = "[removido]"`, `linkLinkedin = null` (todos)
- `emailProfessorResponsavel = null`
- `imagemCapaUrl = null`
- `conteudoEditorJs = null`
- `descricaoCurta = null`
- `linkRepositorio = null`
- `palavrasChave = []`

Manter: `id`, `titulo`, `instituicao`, `curso`, `autor`, `anoPublicado`, `estado = REMOVIDO`,
`removidoEm`, `removidoPorId`, `motivoRemocaoGenerico`, `estadoAnterior`, `criadoEm`,
`atualizadoEm`, `versao`.

Regras de visibilidade: `REMOVIDO` não aparece em `/publicos`, em `GET /{id}` (404, inclusive para
o autor) nem em `/meus`; só em `/removidos`. Os perfis públicos (`InstituicaoService`,
`UsuarioService`) já filtram `APROVADO`, então refletem a remoção sem mudança.

### 3.6 Limpeza de arquivos

- **Hard delete (não-público):** apagar os arquivos na mesma transação.
- **Soft delete (APROVADO):** **não** apagar (a reversão precisa deles).
- **Purge:** apagar os arquivos de `REMOVIDO` expirados.
- **Capa:** derivar de `imagemCapaUrl` (`/uploads/<nome>`); apagar também o thumb `<nome>-thumb`.
- **Inline:** extrair de `conteudoEditorJs` (e de `conteudoHtml` quando o épico 7 chegar) todas as
  ocorrências de `src="/uploads/..."`, deduplicar e apagar.
- **Regra de proteção:** só apagar arquivo cujo nome case com
  `^[0-9a-fA-F-]{36}(-thumb)?\.(png|jpg|jpeg|webp)$`. Isso preserva o seed
  (`/uploads/golden-maker-capa.png`, nome não-UUID) e qualquer arquivo externo.
- **Falha:** se um arquivo não existir, ignorar; se a deleção falhar, logar e seguir (o purge
  pode tentar de novo; o faxineiro de órfãos do épico 8 é rede de segurança, mas **não** é
  pré-requisito desta spec).

### 3.7 Front (fase 1)

- Card do projeto ganha ação **Excluir**, visível só quando o usuário é o autor e está no `/perfil`.
- Confirmação destrutiva; para `APROVADO`, o texto informa que o projeto sai do ar e pode ser
  revertido por 14 dias.
- Nenhuma aba nova é obrigatória nesta fase; se a remoção for de `APROVADO`, a lista já atualiza.
- Ver fase 2 para a aba "Removidos" (a aba só existe quando houver `REMOVIDO`).

### 3.8 Segurança (fase 1)

- `SecurityConfig`: `DELETE /projetos/**` autenticado; `POST /projetos/*/reverter` autenticado;
  `GET /projetos/removidos` autenticado (antes do `GET /projetos/**` permitAll).
- Autoridade no servidor: `projeto.getAutor().getId().equals(userDetails.getId())`. Nunca confiar
  em campo de autor vindo do cliente.

### 3.9 Testes / critérios de aceite (fase 1)

- **AC1:** autor faz `DELETE` de `AGUARDANDO_APROVACAO` -> 200, `GET /{id}` 404, `/meus` sem o
  item, arquivos removidos, nenhum resíduo em `/publicos`.
- **AC2:** autor faz `DELETE` de `REJEITADO` -> idem.
- **AC3:** não-autor (outro aluno/professor) -> 403; sem token -> 401; inexistente -> 404.
- **AC4:** `DELETE` de `APROVADO` -> 200 `REMOVIDO`; some de `/publicos`; `GET /{id}` 404;
  `GET /removidos` lista; `GET /removidos` não expõe PII.
- **AC5:** reversão dentro de 14 dias -> 200, volta a `APROVADO` e ao `/publicos`.
- **AC6:** reversão com `removidoEm` > 14 dias -> 409.
- **AC7:** purge remove linha **e** arquivos de `REMOVIDO` com > 14 dias; não remove com < 14.
- **AC8:** arquivo de seed (`golden-maker-capa.png`) nunca é apagado.
- **AC9:** `DELETE` de `APROVADO` com thumb e imagem inline remove os três no purge.
- **AC10:** `mvn test` e a suíte E2E existente passam; novos testes E2E cobrem AC1-AC9.

### 3.10 DoD (fase 1)

- [x] `DELETE /projetos/{id}` implementado com as duas semânticas por estado.
      O motivo **é** obrigatório no body (`motivoRemocao`, única chave aceita), como
      especificado no contrato acima; quem omite recebe 400.
- [x] Aba "Removidos" do autor com **Reverter** (entregue na fase 2 — §4.5; a fase 1
      já expunha `GET /projetos/removidos` e `POST /projetos/{id}/reverter`).
- [x] Sessão carrega o `id` da conta (`AuthResponse.id`) e `UsuarioLogado.id` é preenchido;
      sem ele o `podeExcluir()` é falso e a ação fica oculta.
- [x] `GET /projetos/removidos` tem teste de segurança próprio (401 sem token).
- [x] `GET /projetos/removidos` e `POST /projetos/{id}/reverter`.
- [x] `REMOVIDO` + campos `removidoEm`, `removidoPorId`, `motivoRemocaoGenerico`, `estadoAnterior`,
      `dadosOriginais`.
- [x] Anonimização exata da §3.5; `dadosOriginais` nunca em DTO/response.
- [x] Limpeza de arquivos (capa/thumb/inline) com a regra de proteção do seed.
- [x] Purge agendado (habilitar `@EnableScheduling`) com propriedade de liga/desliga.
- [x] `REMOVIDO` invisível em `/publicos`, `/{id}` e `/meus`.
- [x] Testes da §3.9 verdes.

## 4. Fase 2 — Devolução/edição e exclusão de conta

### 4.1 Capacidades

- **CAP-D1 — Devolução pelo professor.**
  - **intent:** o professor manda o projeto de volta com justificativa, sem rejeitar.
  - **success:** o projeto sai da fila, o autor vê o motivo e um estado acionável.

- **CAP-D2 — Edição e reenvio pelo autor.**
  - **intent:** o autor ajusta e reenvia.
  - **success:** o projeto volta à fila como `REENVIADO`, com o histórico da devolução.

- **CAP-D3 — Exclusão da própria conta (futuro).**
  - **intent:** o usuário elimina sua conta e seus dados.
  - **success:** conta anonimizada; `APROVADOS` mantidos com autor anonimizado; não-aprovados
    excluídos; pendentes em que é responsável devolvidos ao aluno, com novo responsável exigido.

### 4.2 Máquina de estados (fase 2)

```
AGUARDANDO_APROVACAO --PATCH devolver (prof. responsável)--> ALTERACOES_SOLICITADAS
ALTERACOES_SOLICITADAS --PUT (autor)--> REENVIADO
REENVIADO --PATCH aprovar/rejeitar--> APROVADO | REJEITADO
```

- `aprovar`/`rejeitar` passam a aceitar `AGUARDANDO_APROVACAO` **e** `REENVIADO`
  (hoje só `AGUARDANDO_APROVACAO`; `ProjetoService.java:246` e `:267`).
- `/projetos/pendentes` passa a incluir `REENVIADO` (além de `AGUARDANDO_APROVACAO`).
- `ALTERACOES_SOLICITADAS` **não** entra na fila do professor (saiu dela).
- `SUBMETIDO` segue legado, não produzido.

### 4.3 Contrato de API (fase 2)

```
PATCH /projetos/{id}/devolver
  auth: bearer JWT; professor responsável (emailProfessorResponsavel == e-mail da conta)
  body: { "motivo": string(1..2000) obrigatório }
  200 { "id", "estado": "ALTERACOES_SOLICITADAS", "motivoDevolucao", "devolvidoEm" }
  400 motivo vazio/ausente
  401 sem token
  403 professor não é o responsável indicado
  404 inexistente
  409 estado que não permite devolução (não é AGUARDANDO_APROVACAO)
  efeitos: estado=ALTERACOES_SOLICITADAS, motivoDevolucao, devolvidoEm, devolvidoPorId

PUT /projetos/{id}
  auth: bearer JWT; só o autor
  body: ProjetoCreateRequest (mesma validação da criação)
  200 { "id", "estado": "REENVIADO", "reenviadoEm" }
  400 validação (integrantes, e-mail @cps.sp.gov.br, etc.)
  401 sem token
  403 não é o autor
  404 inexistente
  409 não está ALTERACOES_SOLICITADAS
  efeitos: aplica os campos, estado=REENVIADO, reenviadoEm, atualizadoEm;
           o e-mail do responsável passa a ser o informado no body
```

`GET /projetos/{id}` já devolve o projeto inteiro ao autor quando não aprovado; a edição carrega
por ele (garantir `ALTERACOES_SOLICITADAS` e `REENVIADO` na visibilidade do autor).

### 4.4 Modelo (delta da fase 2)

```java
String motivoDevolucao;     // TEXT, nullable
LocalDateTime devolvidoEm;
UUID devolvidoPorId;
LocalDateTime reenviadoEm;
```

### 4.5 Front (fase 2)

- Aluno, aba "pendentes": separar **"em análise"** (`AGUARDANDO_APROVACAO`, `REENVIADO`) de
  **"devolvidos"** (`ALTERACOES_SOLICITADAS`), com CTA **"Editar e reenviar"** e o motivo visível.
- Professor, aba "pendentes": `REENVIADO` exibe selo **"reenviado após devolução"**.
- Formulário em modo edição (preenche as 4 seções e salva via `PUT`); não cria novo projeto.
- Aba **"Removidos"** no `/perfil`, visível só quando houver `REMOVIDO`: lista com título,
  `removidoEm`, contagem regressiva ("restam N dias") e botão **Reverter**. Usa
  `GET /projetos/removidos`.

### 4.6 Exclusão de conta (futuro — fase 2)

Endpoint (a definir o nome exato na implementação): `DELETE /usuarios/me`, autenticado, só a
própria conta.

- **Conta:** anonimizar a linha — `nome = "[conta removida]"`, `email = null`, `senhaHash = null`,
  `fotoUrl = null`. `email = null` derruba os tokens antigos (`JwtAuthenticationFilter` recarrega
  por e-mail).
- **Projetos `APROVADOS` do usuário:** autor vira a conta anonimizada; **obra mantida** (não
  nulificar `conteudoEditorJs`, `titulo`, `descricaoCurta`, integrantes nem
  `emailProfessorResponsavel` — são dados de terceiros/obra).
- **Projetos não-aprovados do usuário:** hard delete (reusa a fase 1).
- **Pendentes em que o usuário é `emailProfessorResponsavel`:** devolução com motivo
  `"responsável removido"`, **limpando `emailProfessorResponsavel`**; ao reenviar, o autor é
  obrigado a informar novo responsável (a validação do `PUT` já exige o campo).
- **Vínculos** (`professor_instituicao`) e **`codigos_verificacao_email`**: remover.
- **E-mail de aviso:** fora desta spec.

### 4.7 Segurança (fase 2)

- `PATCH /projetos/*/devolver` -> `PROFESSOR` + regra de responsável em serviço (não só papel).
- `PUT /projetos/{id}` -> autenticado + autor.
- `DELETE /usuarios/me` -> autenticado + próprio usuário.
- Nenhuma dessas regras depende de campo enviado pelo cliente.

### 4.8 Testes / critérios de aceite (fase 2)

- **AC11:** professor responsável devolve com motivo -> `ALTERACOES_SOLICITADAS`, sai da fila,
  autor vê o motivo.
- **AC12:** professor não-responsável -> 403; aluno -> 403; sem token -> 401.
- **AC13:** autor faz `PUT` em `ALTERACOES_SOLICITADAS` -> `REENVIADO`, volta à fila com selo.
- **AC14:** `PUT` em estado diferente -> 409; `PUT` de não-autor -> 403.
- **AC15:** aprovar/rejeitar aceitam `REENVIADO`; reavaliar `APROVADO` -> 409.
- **AC16:** devolução sem motivo -> 400.
- **AC17:** exclusão de conta: conta anonimizada; aprovados mantidos com autor anonimizado;
  não-aprovados excluídos; pendentes como responsável devolvidos com e-mail limpo; vínculos e
  códigos removidos; token antigo -> 401.
- **AC18:** aba "Removidos" aparece só com `REMOVIDO` e some depois do purge.

### 4.9 DoD (fase 2)

- [x] Devolução, edição e reenvio; `REENVIADO` na fila e na aprovação.
- [x] Aba "Removidos" com reversão.
- [x] Exclusão de conta anonimizando/excluindo conforme §4.6.
- [x] Testes da §4.8 verdes (`Fase2DevolucaoE2ETest`, `ExclusaoContaE2ETest`; suíte
      completa: 304 testes, 0 falhas).

Observação: a exclusão de conta foi implementada no back-end (`DELETE /usuarios/me`).
A §4.5 não define UI para ela, então nenhum botão novo foi adicionado ao `/perfil`;
o front consome devolução/edição/reversão normalmente.

## 5. Non-goals

- E-mail (devolução, remoção, exclusão de conta) — detalhe posterior.
- Remoção de nome de integrante (sem conta) — fora do MVP.
- Exclusão de projeto por admin.
- Tombstone público (`410` + plaquinha) — mantido 404 + registro interno.
- Quarentena/embargo.
- Edição de projeto fora de `ALTERACOES_SOLICITADAS`.
- Migração TipTap/HTML (épico 7) — dependência, não escopo.
- Hard delete de `APROVADO` por qualquer ator.

## 6. Riscos

| Risco | Mitigação verificável |
|---|---|
| Guardar PII em `dadosOriginais` por 14 dias | Purge apaga linha + arquivos; campo nunca exposto; logar acesso à reversão |
| Cache immutable (`/uploads/`) retém imagem | Aceito; registrado em release note |
| Seed `golden-maker-capa.png` ser apagado | Regra de nome UUID da §3.6 + teste AC8 |
| `GET /projetos/removidos` cair no `permitAll` genérico | Declarar a rota antes do matcher genérico + teste de segurança |
| `REMOVIDO` vazar em `/meus` ou `/{id}` | Filtros no repositório/serviço + testes AC4 |
| Conta de professor deletada deixar fila órfã | Devolução com e-mail limpo + novo responsável (§4.6) |
| Fila do professor não mostrar `REENVIADO` | Query de pendentes inclui os dois estados + teste AC13 |

## 7. Dependências e sequenciamento

| Fase | Escopo | Pré-requisito |
|---|---|---|
| 1 | Exclusão + anonimização + reversão + purge + limpeza de arquivos | — |
| 2a | Devolução/edição (`ALTERACOES_SOLICITADAS`, `REENVIADO`) | — (independente da fase 1) |
| 2b | Exclusão de conta | Fase 1 (delete/anonimização) + 2a (devolução) |

Recomendação: **Fase 1 primeiro** (fecha a LGPD do projeto, sozinha já entrega valor), depois
**2a**, então **2b**. 2a e 2b podem ser um único PR.

## 8. Delta no SPEC canônico (aplicar junto)

- `spec-fatec-repositorios/SPEC.md`, Constraint "Projeto não possui estado RASCUNHO": acrescentar que
  a máquina passa a incluir `REMOVIDO` (fase 1) e `ALTERACOES_SOLICITADAS`/`REENVIADO` (fase 2),
  mantidos `AGUARDANDO_APROVACAO`, `APROVADO`, `REJEITADO`; `RASCUNHO` segue inexistente.
- Incluir CAP-E1..E4 e CAP-D1..D3 no inventário de capabilities.
- Ajustar a documentação que ainda fala em Editor.js/`conteudoEditorJs` quando o épico 7 landing
  (a deleção de arquivo já nasce preparada para `conteudoHtml`).
- **Schema:** `usuarios.email` passa de `NOT NULL` para nullable (exclusão de conta anonimiza
  com `email = null`). O schema é gerenciado pelo Hibernate (`ddl-auto: update`), que altera a
  coluna; em ambiente com migração versionada, acrescentar `ALTER TABLE usuarios ALTER COLUMN
  email DROP NOT NULL`.

## 9. Open questions

1. `PUT /projetos/{id}` aceita `conteudoEditorJs` (contrato atual) ou já `conteudoHtml` (pós-épico 7)?
   Default: implementar contra o contrato vigente e isolar a tradução em um ponto único.
2. Purge ligado por padrão (`app.projeto.purgacao.enabled=true`) ou desligado até validação?
   Default: ligado, com log de contagem.
3. Formulário de edição em rota própria (`/projeto-forms/:id`) ou query (`/projeto-forms?id=`)?
   Default: query, para reusar a rota lazy existente.
