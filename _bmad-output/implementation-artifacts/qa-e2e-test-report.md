---
comando: bmad-qa-generate-e2e-tests
data: 2026-10-03
escopo: back-end (API E2E) + front-end (contrato HTTP)
---

# QA — Testes E2E de API e priorização de cobertura

## 1. Resumo executivo

Duas perguntas foram respondidas: **o que já existe** e **o que teria prioridade de ser
feito agora**. A resposta curta: a suíte anterior era quase toda de unidade, não havia
**nenhum** teste de API ou de integração, e havia **um teste quebrado** fazendo o build
do back-end falhar.

| | Antes | Depois |
|---|---|---|
| Back-end (total) | 28 testes, **1 erro** (build vermelho) | **107 testes, 0 falhas** |
| Back-end (E2E de API) | **0** | **79** |
| Front-end (Vitest) | 57 testes | **74 testes** |
| Contrato HTTP front↔back | **0** | **17** |

Os 79 testes E2E novos sobem a aplicação Spring completa em porta aleatória
contra um PostgreSQL dedicado e falam HTTP de verdade — o filtro JWT, a matriz de
autorização, os controllers, os services e o banco são exercitados de ponta a ponta.

> **Como a baseline foi medida.** A primeira execução do front-end reportou 46 testes,
> mas o build estava com cache `.angular` desatualizado. A baselineautoritativa foi
> obtida removendo o arquivo novo e reexecutando: **57 testes**. Todos os números
> acima vêm dessa medição. No back-end a baseline (28) veio de uma execução limpa.

## 2. O que já existia (inventário)

### Back-end — Maven + JUnit 5/6, Mockito, AssertJ
| Arquivo | Testes | Tipo | O que cobre |
|---|---|---|---|
| `security/MicrosoftTokenVerifierTest.java` | 9 | unidade | Assinatura RS256, tenant (`tid`), audience, `kid` — **o arquivo mais forte da suíte** |
| `service/AuthServiceTest.java` | 11 | unidade (Mockito) | Classificação de papel, normalização de e-mail, fail-fast do verificador |
| `service/ProjetoServiceTest.java` | 4 | unidade (Mockito) | Criação de projeto, AD-6/AD-7, instituição inativa |
| `seed/InstituicaoCsvParserTest.java` | 3 | unidade | Parser do CSV + integridade das 86 linhas versionadas |
| `service/MicrosoftGraphServiceTest.java` | 2 | unidade | Falhas de rede do Graph |
| `security/JwkProviderAdapterIT.java` | 3 | integração (rede) | JWKS real da Microsoft — **não roda no build** (sufixo `IT` fora do padrão do surefire) |

**Lacuna estrutural:** nenhum `@SpringBootTest`, nenhum teste de controller, nenhum
teste de repositório, nenhum teste da cadeia de segurança. `SecurityConfig` — que decide
quem pode fazer o quê — **não tinha uma única asserção**.

### Front-end — Angular 21 + Vitest 4 (jsdom)
9 arquivos / 57 testes, todos `TestBed`. `auth.service.ts` é o único com profundidade real (22 testes, ciclo de vida do MSAL). **`projeto.service.ts` (194 linhas, 7 endpoints), `auth.interceptor.ts` e `admin.guard.ts` não tinham nenhum teste.**

## 3. Priorização — o que testar primeiro, e por quê

O critério foi: **o que quebra de forma silenciosa e cara**. Um teste de UI que falha
aparece na tela; uma falha de autorização não.

| # | Prioridade | Alvo | ACs | Por quê |
|---|---|---|---|---|
| 1 | **P0** | Autorização por papel | Story 1.4, NFR2 | Um aluno alterando o catálogo oficial é o pior resultado possível do produto, e `SecurityConfig` não tinha nenhum teste |
| 2 | **P0** | Sessão / token | Stories 1.1, 1.3 | Todo endpoint protegido depende disso; sessão expirada aparecia como erro genérico |
| 3 | **P0** | Postagem de projeto | Stories 3.1, 3.2, NFR5 | NFR5 (sem estado RASCUNHO) é regra EasyJS de errar e é invisível se o projeto simplesmente nunca chega ao professor |
| 4 | **P0** | CRUD do catálogo | Story 2.1 | Story **em andamento no sprint**; nada garantia o ciclo criar→editar→remover |
| 5 | **P0** | Seed idempotente | Story 2.1 | AC explícito; seed duplicando registros em cada boot corromperia o catálogo |
| 6 | **P1** | Upload de imagem | Story 3.1 | Images do EditorJS; validação de tipo e path traversal |
| 7 | **P1** | Catálogo público | Epic 5 / FR12 | Visitante sem login é o principal valor público da plataforma |
| 8 | **P1** | Contrato HTTP front-end | FR12, Story 3.2 | Divergência de URL/verbo falha em silêncio; hoje o front cai no `PROJETOS_MOCK` |

**Ainda não testado e recomendado a seguir:** Epic 4 (fila/aprovação de projetos) não
existe no back-end — não há como testá-lo até a story ser implementada.

## 4. Suíte criada

### Back-end — `back-end/src/test/java/com/fatecrepository/e2e/`

| Arquivo | Testes | ACs cobertas |
|---|---|---|
| `ApiE2ETestSupport.java` | (base) | HTTP + emissão real de token via `JwtTokenProvider` |
| `AutorizacaoPorPapelE2ETest.java` | 15 | Story 1.4, NFR2 |
| `AutenticacaoSessaoE2ETest.java` | 11 | Stories 1.1, 1.3 |
| `PostagemProjetoE2ETest.java` | 22 | Stories 3.1, 3.2, NFR5 |
| `CatalogoInstituicoesE2ETest.java` | 14 | Story 2.1, NFR4 |
| `SeedCatalogoE2ETest.java` | 4 | Story 2.1 (idempotência) |
| `UploadECatalogoPublicoE2ETest.java` | 10 | Story 3.1, Epic 5 / FR12 |
| `SmokeE2ETest.java` | 3 | sanity da infra |
| **Total** | **79** | |

Configuração: `back-end/src/test/resources/application-test.yml` — perfil `test` com
`ddl-auto: create-drop`, seed desligado e conexão parametrizada
(`TEST_DB_HOST`/`TEST_DB_PORT`/`TEST_DB_NAME`, padrão `fatecrepository_test`).

### Front-end
`front-end/src/app/core/services/projeto.service.spec.ts` — 17 testes com
`HttpTestingController`, fixando URL, verbo, payload multipart e propagação de 401.

## 5. Achados

### 5.1 Bloqueante (corrigido nesta execução)
**A suíte existente não passava.** `AuthServiceTest.deveAbortarQuandoVerificadorRecusaToken`
estourava `UnnecessaryStubbingException`: o stub `doNothing()` do `@BeforeEach` é
sobrescrito por `doThrow(...)` no próprio teste, deixando o stub ocioso. `mvn test`
terminava em **BUILD FAILURE**. Corrigido com `lenient()` em `AuthServiceTest.java:66`.

### 5.2 Alto — lacuna de contrato (NÃO corrigido, requer decisão de produto)
**`GET /projetos/publicos` não existe no back-end, mas o front-end chama.**
`ProjetoService.listarProjetosPublicos()` (`projeto.service.ts:21`) consome essa rota e,
não encontrando, cai no `catchError` → `PROJETOS_MOCK`. Ou seja: **a vitrine pública
mostra 6 projetos fictícios em vez de dados reais**, e nada sinaliza isso ao usuário.

Comprovado na aplicação em execução:
```
GET /projetos/publicos  →  HTTP 500 {"codigo":500,"mensagem":"Erro interno do servidor"}
```
Causa: a rota cai em `GET /projetos/**` (`permitAll`) e depois bate em
`/projetos/{id}` com `id="publicos"`, que não parseia como UUID.

Isso viola o AC da Story 5.1 ("visualizo a lista/cards de projetos com estado `APROVADO`").
O teste `UploadECatalogoPublicoE2ETest.LacunaDeContrato` **documenta a lacuna
propositalmente** (assevera 500) e deve ser invertido para 200 quando o endpoint for criado.

### 5.3 Médio — `adminGuard` não está ligado a nenhuma rota
`front-end/src/app/core/auth/admin.guard.ts` existe e funciona, mas **nenhuma rota de
`app.routes.ts` o referencia**. Na prática `/projeto-forms` (tela de submissão) não tem
guard. Hoje o back-end segura o catálogo por papel, então não há brecha de dados — mas
a proteção de UI não existe.

### 5.4 Baixo — riscos estruturais, cobertos por testes
- `SecurityConfig:52` — `anyRequest().permitAll()`: **qualquer controller novo é público por padrão**. Um teste que amarre "toda rota nova exige decisão explícita" preveniria a regressão.
- `JwtAuthenticationFilter:58` — nunca interrompe o fluxo; token inválido degrada para anônimo. Correto (endpoints `permitAll` continuam funcionando), mas torna essential o teste de 401.
- `MicrosoftGraphService` constrói `HttpClient`/`ObjectMapper` no construtor sem DI — **não é testável sem substituição em bytecode**; só a TreatFailure está coberta.

### 5.5 Flakiness observada no front-end (não reproduziu)
Na primeira execução após criar `projeto.service.spec.ts`, o Vitest reportou
`3 failed | 72 passed (75)` — um número de testes maior que o estável. **Não reproduziu
em 6 execuções consecutivas** (`74 passed`), que é o valor autoritativo
(57 preexistentes + 17 novos). As specs de `header` e `microsoft-login-button` usam
`setTimeout` para reproduzir o agendador zoneless e são as prováveis candidatas.
Vale investigar antes de confiar em CI verde.

### 5.6 Informativo
`emailProfessorResponsavel` com espaços nas bordas é **rejeitado com 400** pelo `@Email`
antes de o service aparar. Comportamento correto, mas contraintuitivo; agora está
documentado por teste (`emailComEspacosNosExtremos`).

## 6. Como executar

```bash
# Back-end (requer PostgreSQL)
docker compose up -d postgres
cd back-end && ./mvnw test                       # 107 testes
./mvnw test -Dtest='com.fatecrepository.e2e.*'   # só os 79 E2E de API

# Variáveis de conexão do banco de teste (padrões entre parênteses)
# TEST_DB_HOST=localhost  TEST_DB_PORT=5433
# TEST_DB_NAME=fatecrepository_test  TEST_DB_USER=postgres  TEST_DB_PASSWORD=123456

# Front-end
cd front-end && npm test                          # 74 testes
```

O banco `fatecrepository_test` precisa existir uma vez:
```bash
docker exec fatec-postgres psql -U postgres -c "CREATE DATABASE fatecrepository_test;"
```

> **Nota de ambiente:** os testes E2E exigem PostgreSQL. Onde não houver, a suíte E2E
> falha por indisponibilidade de conexão — os testes unitários continuam independentes.

## 7. Recomendação

1. **Implementar `GET /projetos/publicos`** (achado 5.2) — sem isso a Home exibe dados fictícios como reais. Reverter o teste de lacuna.
2. **Ligar `adminGuard`** em `/projeto-forms` (achado 5.3).
3. **Fechar o `anyRequest().permitAll()`** com um teste que exija matchers explícitos.
4. Quando a Epic 4 entrar, cobrir fila e aprovar/rejeitar com `motivoRejeicao` obrigatório.
5. Ligar o CI a `./mvnw test` + `npm test` — hoje um teste quebrado passou despercebido até esta execução.