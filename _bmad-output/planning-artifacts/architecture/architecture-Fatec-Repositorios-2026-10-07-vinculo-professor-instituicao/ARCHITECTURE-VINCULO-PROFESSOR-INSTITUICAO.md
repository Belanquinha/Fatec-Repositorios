---
name: ARCHITECTURE-Fatec-Repositorios-Vinculo-Professor-Instituicao
type: architecture-decision-record
purpose: build-substrate
altitude: feature
paradigm: layered (Controller-Service-Repository) REST API + Angular SPA + stateless JWT
scope: Persistência do vínculo professor↔instituição (0–4) — o "Salvar vínculos" da aba "Instituições (0–4)" do /perfil (Story 2.3)
status: final
created: 2026-10-07
updated: 2026-10-07
binds: [CAP-3, CAP-8, FR-5, FR-6, FR-13, FR-14, NFR-2, NFR-3, NFR-4, AD-4, AD-5]
sources:
  - ../sprint-change-proposal-2026-10-07.md (§4.2 — gap registrado, dono: bmad-architecture)
  - ../UX-SELECAO-INSTITUICOES-PROFESSOR.md (§§3–5, §10 — comportamento da aba, gap bloqueante)
  - ../DESIGN.md + ../EXPERIENCE.md (spines finais 2026-09-29 — fonte verdade de UX, não mexidos)
  - ../epics.md (Stories 2.2, 2.3, 3.1, 5.1, 6.1)
  - ../../architecture/architecture-Fatec-Repositorios-2026-09-16/ARCHITECTURE-SPINE.md (AD-1, AD-4, AD-5)
  - ../../architecture/architecture-Fatec-Repositorios-2026-09-22-catalogo-instituicoes/ARCHITECTURE-CATALOGO-INSTITUICOES.md
  - back-end/src/main/java/com/fatecrepository/controller/ProfessorController.java (só GET /professores)
  - back-end/src/main/java/com/fatecrepository/controller/InstituicaoController.java (só CRUD /instituicoes)
  - back-end/src/main/java/com/fatecrepository/security/SecurityConfig.java (fail-closed, GET público)
  - back-end/src/main/java/com/fatecrepository/security/CustomUserDetails.java (@AuthenticationPrincipal)
  - back-end/src/main/java/com/fatecrepository/model/User.java + model/Instituicao.java
companions: []
---

# Arquitetura — Vínculo Professor↔Instituição (0–4)

## 1. Contexto e problema

### 1.1 O que é o gap (resposta direta à pergunta)

O **vínculo professor↔instituição** é a relação N:N entre um usuário `PROFESSOR` e até 4 linhas do catálogo oficial de instituições. É ele que diz "o professor Roberto atua nas Fatecs X, Y, Z" — e dele derivam a **fila de aprovação** (Story 4.1 lista `AGUARDANDO_APROVACAO` das *suas* unidades) e a **landing do Perfil** (FR-14: professor zerado → aba "Instituições" em destaque; professor com vínculos → aba "Pendentes").

O gap, verificado no código em 2026-10-07 e confirmado nesta rodada:

- `ProfessorController.java:20-36` — só tem `GET /professores` (lista para autocomplete do professor responsável, Story 3.2). **Nenhum** POST/PUT/DELETE de vínculo.
- `InstituicaoController.java` — só tem CRUD do catálogo (`GET/POST/PUT/DELETE /instituicoes`). **Nenhuma** operação de "vincular professor a N instituições".
- Busca por `ProfessorInstituicao|vinculo|professor_instituicao` no back-end retorna **zero arquivos** — a join table `professor_instituicao` prevista no spine (`ARCHITECTURE-SPINE.md` AD-4 + diagrama ER) **nunca foi implementada** como entidade/tabela.

Na prática:

1. A UX da aba "Instituições (0–4)" está **totalmente especificada** (chips, contador "X de 4 unidades", CTA "Salvar vínculos", recusa inline da 5ª, painel neutro de erro), mas o clique em **"Salvar vínculos" não tem para onde chamar** — e a aba não tem de onde **ler** os vínculos salvos ao abrir o `/perfil`.
2. Por isso o gap é **bloqueante só da Story 2.3**. As stories 2.2 (consultar/filtrar), 3.1 (campo single no form) e 5.1 (filtro `mode=filter`) usam o `GET /instituicoes` público, que **já existe** (`SecurityConfig.java:52` — `permitAll`). Só o Save da 2.3 precisa do endpoint novo.
3. Este documento **resolve** o que a correct-course de 2026-10-07 (§4.2) só registrou: método/path, body, leitura, reforço servidor do teto e idempotência do Save com 0 vínculos.

### 1.2 Restrições vigentes (não renegociadas aqui)

| Fonte | Regra preservada |
|---|---|
| FR-6 / NFR-3 / AD-4 | 0–4 vínculos por professor; 0 permitido e não bloqueante; 5ª recusada; **teto reforçado no servidor** |
| FR-5 / NFR-4 / AD-5 | Catálogo fechado: professor **nunca** cria instituição; só referencia `instituicaoId` existente |
| FR-13/FR-14 / CAP-8 | Superfície = **aba "Instituições (0–4)" do `/perfil`**; sem rota avulsa; landing por estado (0 → onboarding; >0 → Pendentes) |
| AD-1 / NFR-2 | Papel decidido no servidor a partir do JWT; front só renderiza |
| Convenções do spine | Endpoints REST em português plural; DTOs `*Request`/`*Response`; PK UUID; envelope de erro `{"codigo": <http>, "mensagem": "..."}`; claim JWT `role` |
| `SecurityConfig` fail-closed | O que não for listado exige sessão (`.anyRequest().authenticated()`); rotas novas precisam de regra explícita |

## 2. Decisões de arquitetura

### AD-VP-1 — Semântica de substituição total (set replacement) via `PUT /professores/me/instituicoes` + leitura via `GET` no mesmo path

**Contrato canônico (implementar exatamente isto):**

```http
GET /professores/me/instituicoes
Authorization: Bearer <JWT de PROFESSOR>
→ 200 [{ InstituicaoResponse ordenado por nome, depois cidade }]
→ 401 sem token / 403 papel ≠ PROFESSOR

PUT /professores/me/instituicoes
Authorization: Bearer <JWT de PROFESSOR>
Content-Type: application/json

{ "instituicaoIds": ["<uuid>", "<uuid>"] }
→ 200 [{ InstituicaoResponse persistido, mesma ordenação }]
→ 400 body malformado / UUID inválido / ids duplicados
→ 404 instituicaoId inexistente (nada é persistido — falha atômica)
→ 409 mais de 4 ids (teto; mensagem "Máximo de 4 instituições por professor")
→ 401 sem token / 403 papel ≠ PROFESSOR
```

**Exemplo — Save com 2 vínculos:**

```json
// PUT /professores/me/instituicoes
{ "instituicaoIds": ["3fa85f64-5717-4562-b3fc-2c963f66afa6", "7cb5d9b1-9a1e-4b2c-8f3a-1d2e5f6a7b8c"] }
```

**Exemplo — Save com 0 (limpar tudo, 1º acesso incluso):**

```json
// PUT /professores/me/instituicoes
{ "instituicaoIds": [] }
```

**Por que `PUT` com substituição total (e não POST/DELETE por vínculo):**

- O front da aba já trabalha com **estado local até o Save** (remoção de chip = local, sem chamada — `UX-SELECAO-*.md` §3.4). O Save envia o **conjunto final**, não um diff. `PUT` com semântica "o corpo é o novo conjunto completo" casa 1:1 com esse comportamento: 1 clique = 1 chamada, idempotente, sem corrida entre adiciona/remove.
- Alternativa rejeitada — `POST /professores/me/instituicoes/{id}` + `DELETE .../{id}` por vínculo: N chamadas por Save, ordens parciais possíveis, remoção de chip viraria chamada imediata (contradiz o spine), e o Save com 0 viraria N deletes. Mais tráfego, mais estados intermediários, zero benefício.
- Alternativa rejeitada — `PATCH` com operações add/remove: expressa diff, mas o cliente não tem diff confiável (ele tem o conjunto); transfere complexidade de reconciliação para o servidor sem necessidade.
- Alternativa rejeitada — `PUT /usuarios/{id}/instituicoes` com id na URL: abre IDOR (professor A escreve nos vínculos do professor B); o `/me` deriva o sujeito do JWT (`@AuthenticationPrincipal CustomUserDetails`, padrão já usado em `GET /projetos/meus` — `ProjetoController.java:85`), eliminando a classe inteira de bugs de autorização.
- Nome do campo: `instituicaoIds` (plural, UUIDs), consistente com `instituicaoId` usado em `ProjetoCreateRequest` (Story 3.1). Nome do path em português plural (`/professores/.../instituicoes`), consistente com a convenção do spine — **não** usar `/vinculos` (anglicismo mecânico banido da microcopy; o recurso REST continua sendo instituições, o vínculo é a relação).

### AD-VP-2 — Join como entidade explícita `ProfessorInstituicao` (não `@ManyToMany`)

```text
back-end/src/main/java/com/fatecrepository/
  model/ProfessorInstituicao.java          # NOVO — entidade da junção
  repository/ProfessorInstituicaoRepository.java  # NOVO
  dto/request/VinculoInstituicoesRequest.java    # NOVO — { List<UUID> instituicaoIds }
  service/ProfessorVinculoService.java            # NOVO — regra 0–4 + replace transacional
  controller/ProfessorController.java             # ESTENDER — GET + PUT /professores/me/instituicoes
  security/SecurityConfig.java                    # ESTENDER — regras do novo path (ver AD-VP-4)
```

Modelo:

```java
// model/ProfessorInstituicao.java (essência — seguir Lombok/JPA do projeto)
@Entity @Table(name = "professor_instituicao",
  uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "instituicao_id"}))
public class ProfessorInstituicao {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "usuario_id", nullable = false) private UUID usuarioId;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;
    @Column(nullable = false, updatable = false) private LocalDateTime criadoEm;
}
// + index em (usuario_id) para a leitura da aba e da fila
```

Repositório:

```java
List<ProfessorInstituicao> findByUsuarioId(UUID usuarioId);
void deleteByUsuarioId(UUID usuarioId);
boolean existsByUsuarioIdAndInstituicaoId(UUID usuarioId, UUID instituicaoId);
long countByUsuarioId(UUID usuarioId);
```

**Por que entidade explícita (e não `@ManyToMany` em `User`/`Instituicao`):**

- É o que o spine já prevê (`ARCHITECTURE-SPINE.md` §Structural Seed + ER: `ProfessorInstituicao (join)` / tabela `PROFESSOR_INSTITUICAO { usuarioId FK, instituicaoId FK }`). Implementar como `@ManyToMany` esconderia a tabela, impediria `countByUsuarioId` eficiente, dificultaria a query da fila (Story 4.1: "projetos das minhas instituições") e bloquearia extensão futura (ex.: `criadoEm` do vínculo, auditoria). Custo: 1 entidade + 1 repository — trivial.
- **Não** adicionar `List<Instituicao>` em `User.java` nem `List<User>` em `Instituicao.java`: evita coleções gigantes em memória (catálogo tem 86 linhas; professor tem no máx. 4 — a leitura é sempre pelo lado do professor).
- Schema: `ddl-auto: update` cria a tabela/colunas sem migração manual (mesmo regime do catálogo — `ARCHITECTURE-CATALOGO-INSTITUICOES.md` AD-CI-4). `unique(usuario_id, instituicao_id)` + FKs para `usuarios(id)` e `instituicoes(id)`; delete de instituição com vínculo existente falha por FK (fail-fast desejado — admin cura o catálogo, não apaga vínculo alheio por cascata; **sem** `ON DELETE CASCADE`).

### AD-VP-3 — Reforço servidor: catálogo fechado + teto 4 + Save com 0, tudo atômico

Regras do `ProfessorVinculoService.substituirVincos(usuarioId, instituicaoIds)` (ordem de validação = ordem de checagem, primeira falha aborta sem persistir nada — método `@Transactional`):

1. **Autorização:** papel do JWT == `PROFESSOR`, senão 403 (regra em `SecurityConfig` + `@PreAuthorize("hasRole('PROFESSOR')")` no handler — defesa em profundidade, padrão AD-1). `ALUNO` e `ADMIN` recebem 403 neste endpoint (admin muta o catálogo, não vínculo alheio).
2. **Forma:** body presente; `instituicaoIds` presente (ausente = 400, não confundir com `[]`); cada item UUID válido (senão 400); **sem duplicados** (duplicado = 400 "instituição duplicada" — conta uma vez, não duas; impede bypass do teto via repetição).
3. **Teto:** `size > 4` → **409 Conflict** com `{"codigo": 409, "mensagem": "Máximo de 4 instituições por professor"}`. Por que 409 e não 400: o corpo está bem formado, o conflito é com a regra de negócio (recurso em estado conflitante com o teto). Aceita-se 400 como alias legado **apenas** se o front já tratar 400 da proposta 2026-10-07 — o servidor emite **409 canônico**; o front trata `400|409` como "teto" para robustez (a proposta §4.2 fala em "400/409 na 5ª" exatamente por isso). A 5ª nunca chega aqui vinda da UI conforme (recusa inline), mas deep-link/script/bypass de front **precisam** falhar no servidor (NFR-3).
4. **Catálogo fechado:** cada id deve existir em `instituicaoRepository` (inexistente = 404, nada persistido); opcionalmente checar `ativo == true` (inativa = 422 ou 404 — decidir na implementação; recomendado 422 "unidade fora do catálogo oficial" se o campo `ativo` for usado para units 2027). Nenhum nome/código livre é aceito — só UUID do catálogo.
5. **Substituição:** `deleteByUsuarioId` + `insert` dos N (0–4) **na mesma transação**. `[]` é válido e apaga todos os vínculos (Save com 0 do 1º acesso — FR-6). Repetir o mesmo corpo é no-op observável (idempotente — `PUT`).
6. **Resposta:** `200` com a lista persistida como `List<InstituicaoResponse>` (mesmo DTO do catálogo — reuso, sem DTO novo de resposta), ordenada por `nome` depois `cidade` (mesma ordenação do autocomplete, para a aba hidratar chips/contador sem reordenar).

Casos-limite decididos aqui (para não serem redescobertos no dev):

| Caso | Comportamento servidor |
|---|---|
| `PUT` com `[]` (0 vínculos) | 200 `[]`; apaga vínculos; onboarding reapresentado pelo front (FR-6) |
| `PUT` repetido com mesmo corpo | 200, mesmo resultado (idempotente) |
| `PUT` com id duplicado no array | 400 (não conta duas vezes, não persiste nada) |
| `PUT` com 5+ ids distintos | 409, nada persistido (recusa é do lote inteiro, não "salva 4 e ignora a 5ª") |
| `PUT` com id inexistente misturado a válidos | 404, nada persistido (atomicidade total) |
| `GET` sem vínculos | 200 `[]` (não 404 — zero é estado válido; o front decide onboarding × Pendentes) |
| Concorrência (duas abas salvando) | Último `PUT` vence (last-write-wins); sem versionamento otimista — conjunto ≤4, janela de corrida irrelevante no MVP |
| Professor deletado / instituição deletada | FK falha fail-fast; sem cascata silenciosa |

### AD-VP-4 — Segurança: regra explícita no `SecurityConfig` (fail-closed) + `@PreAuthorize`

```java
// SecurityConfig — inserir ANTES da linha `.requestMatchers(HttpMethod.GET, "/professores/**").permitAll()`
// (ordem importa: primeira regra que casa vence; depois do permitAll amplo o GET novo ficaria público).
// Também necessariamente antes do `.anyRequest().authenticated()` final.
.requestMatchers(HttpMethod.GET, "/professores/me/instituicoes").hasRole("PROFESSOR")
.requestMatchers(HttpMethod.PUT, "/professores/me/instituicoes").hasRole("PROFESSOR")
```

- O `GET /professores` (autocomplete, Story 3.2) permanece `permitAll` (`SecurityConfig.java:58`) — **não** mexer. Só o sub-path `/me/**` é fechado para `PROFESSOR`.
- `PUT` exige `PUT` no CORS (`configuration.setAllowedMethods` já inclui `PUT` — `SecurityConfig.java:95` — sem mudança).
- `@PreAuthorize("hasRole('PROFESSOR')")` nos dois handlers novos (requer `@EnableMethodSecurity`; se ainda ausente, adicionar — defesa em profundidade além da regra de URL).
- Sujeito sempre via `@AuthenticationPrincipal CustomUserDetails` → `getId()` (mesmo padrão de `ProjetoController.obterMeusProjetos`). **Nunca** aceitar `usuarioId` no body/URL.

### AD-VP-5 — Leitura que alimenta a aba e a landing (FR-14)

- `GET /professores/me/instituicoes` retorna `List<InstituicaoResponse>` ordenado — o front hidrata chips + contador ("X de 4 unidades") e decide a landing: `[]` → destaca aba "Instituições (0–4)" com onboarding; não-vazio → aba "Pendentes" pré-selecionada. Nenhum endpoint extra de "contagem" ou "tem vínculo" — `length` do array basta (máx. 4 itens, sem paginação).
- A Story 4.1 (fila "Pendentes") reutiliza `findByUsuarioId` no servidor para filtrar `AGUARDANDO_APROVACAO` por `instituicaoId ∈ vínculos` — fora do escopo de implementação deste contrato, mas o método de repositório já atende; registrar como dependência consumidora (não bloqueia 2.3).

### AD-VP-6 — Erros no envelope do projeto; front trata 400|409 como teto

- Todos os erros usam o envelope existente `{"codigo": <http>, "mensagem": "..."}` (convenção do spine; `SecurityConfig` já emite 401/403 nesse formato).
- Mensagem do teto **idêntica** à microcopy da UI: `"Máximo de 4 instituições por professor"` (spine UX — verbo na frente, sem vermelho, sem "Limite excedido — erro").
- Painel neutro de erro do Save (`UX-SELECAO-*.md` §5) é alimentado por qualquer 5xx/429/timeout; 4xx de validação (400/404/409) viram mensagem inline ligada por `aria-describedby`, nunca painel.

## 3. Trade-offs avaliados

| Opção | Prós | Contras | Decisão |
|---|---|---|---|
| `PUT /professores/me/instituicoes` (replace total) | 1 chamada por Save; idempotente; casa com estado-local-até-Save; sem IDOR | Cliente sempre envia conjunto completo (máx. 4 UUIDs — irrelevante) | **Sim — AD-VP-1** |
| POST/DELETE por vínculo | Granularidade | N chamadas; estados parciais; contradiz chip-local; Save-com-0 vira N deletes | **Não** |
| `PUT /usuarios/{id}/instituicoes` | Genérico | IDOR por padrão; foge da convenção `/me` já usada em `/projetos/meus` | **Não** |
| `@ManyToMany` | Menos arquivos | Tabela oculta; queries da fila/landing piores; sem extensão | **Não — AD-VP-2** |
| Teto só no front | Menos código servidor | Bypass trivial; viola NFR-3/FR-6 | **Não — AD-VP-3** (409 servidor) |
| `GET` separado de "meus vínculos" em outro path | — | Dois paths para o mesmo recurso; front com duas fontes | **Não** (mesmo path, GET+PUT) |
| Versionamento otimista / ETag no PUT | Detecta escrita concorrente | Complexidade sem ganho (conjunto ≤4, last-write-wins aceitável no MVP) | **Não** (registrado como extensão futura) |
| Paginação no GET me | Padrão de catálogo | Máx. 4 itens — paginação é ruído | **Não — AD-VP-5** |

## 4. Alinhamento PRD / SPEC / epics / UX

- **PRD** (`FR-6`, `UJ-2`): vincular 0–4, opcional no 1º login, 5ª recusada — preservado; este contrato é o "como persiste".
- **SPEC** (`CAP-3`, `CAP-8`): catálogo fechado + hub `/perfil` — preservado; nenhum texto livre, nenhuma rota nova de UI.
- **Spine** (`AD-4`, `AD-5`): join `professor_instituicao` + teto servidor + escrita de catálogo só por admin — **finalmente implementado** (AD-4 existia só no papel).
- **Catálogo** (`ARCHITECTURE-CATALOGO-INSTITUICOES.md`): `codigoUnidade` unique, seed idempotente, `GET /instituicoes` público — intocado; o vínculo referencia o catálogo por UUID (FK), nunca por código (código é chave de seed, não de vínculo).
- **Epics**: desbloqueia a Story 2.3 (única bloqueada); 2.2/3.1/5.1 seguem no `GET /instituicoes` existente. Story 4.1 (fila) vira consumidora futura do `findByUsuarioId`.
- **UX**: nenhum comportamento de tela muda — chips/contador/Save/recusa inline/painel neutro já especificados passam a ter para onde chamar e de onde ler. Microcopy do 409 = microcopy da UI.

## 5. Sequência de implementação (handoff para o dev da Story 2.3)

1. `model/ProfessorInstituicao.java` + `repository/ProfessorInstituicaoRepository.java` (AD-VP-2); subir back e conferir tabela `professor_instituicao` criada (`ddl-auto: update`).
2. `dto/request/VinculoInstituicoesRequest.java` (`{ List<UUID> instituicaoIds }` + `@NotNull` + validação de tamanho máx. 4 como segunda barreira além da regra de negócio que retorna 409).
3. `service/ProfessorVinculoService.java` (`ler` + `substituir` transacional, ordem de validação da AD-VP-3; `InstituicaoResponse` como tipo de retorno).
4. `ProfessorController` — `GET` + `PUT /professores/me/instituicoes` com `@PreAuthorize("hasRole('PROFESSOR')")`, `@AuthenticationPrincipal CustomUserDetails`, `@Valid @RequestBody`; OpenAPI `@Operation`/`@ApiResponses` (200/400/404/409/401/403) no padrão do `InstituicaoController`.
5. `SecurityConfig` — duas linhas de `requestMatchers` (AD-VP-4) antes do `anyRequest()`.
6. Testes: unitários do service (0, 1, 4, 5→409, duplicado→400, inexistente→404, idempotência, atomicidade) + E2E (professor salva/lê/limpa; aluno→403; sem token→401; `GET /professores` segue público). Espelhar o estilo de `AutenticacaoSessaoE2ETest` / `AutorizacaoPorPapelE2ETest`.
7. Front (fora deste contrato, mas desbloqueado): aba chama `GET` ao abrir, `PUT` no Save; trata `400|409` como teto inline; painel neutro em 5xx/timeout.

## 6. Open questions (não bloqueantes da 2.3)

- `GET /instituicoes` hoje retorna o catálogo inteiro sem paginação/filtro servidor — 2.2/5.1 filtram no cliente ou precisam de `?q=` servidor? Dono: dev de 2.2/5.1 (não afeta este contrato).
- Unidade inativada (`ativo=false`, ex. units 2027 futuras) com vínculo existente: filtrar da fila? invalidar vínculo? Dono: Story 4.1.
- Auditoria de vínculo (`criadoEm` já previsto; `quem/quando` além disso) e ETag/`If-Match`: extensões pós-MVP.
- Fila do professor (4.1) com 0 vínculos: vazia com CTA para a aba (landing FR-14) — comportamento de front, já especificado.

## 7. Critério de aceite deste contrato (quando está "resolvido")

- [ ] `PUT /professores/me/instituicoes` com 0–4 UUIDs válidos → 200 com a lista persistida; `GET` seguinte retorna o mesmo conjunto.
- [ ] `PUT` com 5 UUIDs → 409 `{"codigo":409,"mensagem":"Máximo de 4 instituições por professor"}` e nada persistido.
- [ ] `PUT` com id inexistente → 404 e nada persistido; com duplicado → 400 e nada persistido.
- [ ] `PUT` com `[]` → 200 `[]` (limpa tudo; 1º acesso incluso).
- [ ] Aluno/admin no endpoint → 403; sem token → 401; `GET /professores` segue público.
- [ ] Story 2.3 sai de "bloqueada" para "ready-for-dev" (dependência restante: aba do Perfil da 6.1, cf. sprint-plan §Roteiro).

---
*Gerado por bmad-architecture em 2026-10-07. Resolve o gap registrado em sprint-change-proposal-2026-10-07 §4.2 (dono: bmad-architecture). Spines de UX não tocados.*
