---
title: 'Corrigir login MSAL contra o tenant da CPS'
type: 'bugfix'
created: '2026-09-29'
status: 'done'
baseline_commit: '401d14b4127f386988a4e3be2a2ef6ba118d80cb'
route: 'dispatch'
review_loop_iteration: 0
context: ['{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Fatec-Repositorios-2026-09-16/ARCHITECTURE-SPINE.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problema:** O login Microsoft não funciona. O front-end consulta a conta antes de `PublicClientApplication.initialize()`, o que faz o MSAL lançar `uninitialized_public_client_application` e o header nunca sair do estado "deslogado". Além disso, `.env` aponta para um tenant diferente do `docker-compose.yml`, o redirect URI diverge entre as duas formas (com e sem barra final) e o back-end aceita token de qualquer tenant.

**Abordagem:** Unificar a configuração MSAL em um único valor canônico de tenant/redirect, tornar a inicialização do MSAL idempotente e aguardável por qualquer componente, remover o fluxo de login institucional morto, e validar o `tid` do token no back-end antes de emitir o JWT.

## Boundaries & Constraints

**Always:**
- MSAL fica restrito ao **tenant único da CPS** (AD-3). Autoridade `common` é proibida em qualquer arquivo versionado.
- O tenant canônico é `eabe64c5-68f5-4a76-8301-9577a679e449` — confirmado pelo humano como o tenant real do app registration `146c36f9-…`. `docker-compose.yml:49` e `ARCHITECTURE-SPINE.md/.memlog.md:11` divergem e devem ser corrigidos para este valor.
- O redirect URI canônico é `http://localhost:4200/` — **com barra final**, exatamente como registrado no Entra. Divergir da barra causa `AADSTS50011`.
- `MSALInstanceFactory` só lê `environment.msal*`; nenhuma constante de tenant é hard-coded no front-end.
- Todo acesso a `getAllAccounts()` passa por uma promessa de inicialização aguardada.
- Nenhuma credencial é commitada: `.env` continua ignorado; `.env.example` só placeholders.

**Never:**
- Não implementar `POST /auth/login` (login institucional por senha). É legado removido por decisão — `architecture-.../.memlog.md:16`, `SPEC.md:66`.
- Não adicionar dependência de biblioteca nova no back-end (usar `com.auth0:java-jwt`, já presente).
- Não alterar `JwtTokenProvider`, `SecurityConfig`, rotas ou seeds — fora do escopo.
- Não tocar em `_bmad-output/planning-artifacts/**` (árvore suja do usuário, não relacionada).

## I/O & Edge-Case Matrix

| Cenário | Entrada / Estado | Saída / Comportamento esperado | Tratamento de erro |
|----------|------------------|-------------------------------|--------------------|
| Token do tenant CPS | `tid` = GUID da CPS | `AuthResponse` 200 com JWT próprio | — |
| Token de outro tenant | `tid` ≠ GUID da CPS | 401 `TenantInválidoException` | Graph não é chamado; log sem token |
| `mail` nulo (conta CPS) | Graph `/me` sem `mail` | classifica por `userPrincipalName` | 401 se ambos ausentes |
| `getAllAccounts()` antes do init | header monta antes do MSAL | aguarda a promessa; devolve `undefined` | nunca lança `uninitialized_public_client_application` |
| `inicializar()` chamado N vezes | 2+ componentes no `ngOnInit` | `initialize()` roda **uma** vez | — |
| `no_token_request_cache_error` | cache de sessão corrompido | limpa `sessionStorage` `msal.*` | mantém o comportamento atual |

</frozen-after-approval>

## Code Map

- `front-end/src/app/core/auth/auth.service.ts` — `inicializar()` (16-29) e o getter `conta` (31-34) são a raiz do bug: `conta` chama `getAllAccounts()` sem aguardar init. `loginInstituicao()` (118-142) é código morto (zero call sites, endpoint inexistente) com `console.log` em 125.
- `front-end/src/app/core/auth/microsoft-login-msal.ts` — `MSALInstanceFactory()` (5-13): único ponto de construção do `PublicClientApplication`; já lê só `environment.msal*`. Manter como está.
- `front-end/src/app/shared/components/header/header.ts` — `ngOnInit` (28-35) chama `carregarUsuario()` **antes** do `ngOnInit` do filho `MicrosoftLoginButton` (`microsoft-login-button.ts:20`). É aqui que o acesso antecipado acontece.
- `front-end/src/app/shared/components/microsoft-login-button/microsoft-login-button.ts` — `ngOnInit` (18-36) é quem chama `inicializar()`; com o header corrigido os dois ficam concorrentes → precisa ser idempotente.
- `front-end/src/environments/environment.ts` e `environment.development.ts` — fallbacks das 4 vars `NG_APP_*`; ambos usam `common`. Trocados por `fileReplacements` (`angular.json`, config `development`).
- `front-end/.env` — ignorado pelo git, tenant divergente. `front-end/.env.example` — **inexistente**, embora `front-end/README.md:24` e `.gitignore:49` o referenciem.
- `front-end/.dockerignore` — não exclui `.env`. `front-end/Dockerfile:12-19` — defaults de ARG inválidos.
- `docker-compose.yml:44-50` — `build.args` do front-end com literais (não `${...}`), divergentes do `.env`.
- `front-end/src/app/app.spec.ts:17` e `header.spec.ts:21` — mock obsoleto `loginPopUp`, que não existe mais em `AuthService`.
- `back-end/src/main/java/com/fatecrepository/service/MicrosoftGraphService.java` — `GraphUser` (29-36) mapeia `id/displayName/mail/userPrincipalName`; **sem `tid`**. `getUserInfo()` (38-60) devolve `null` em qualquer não-200.
- `back-end/src/main/java/com/fatecrepository/service/AuthService.java` — `loginMicrosoft` (33-67) é o único portão de segurança; usa só `graphUser.getMail()` (41) e classifica por domínio (69-77).
- `back-end/src/main/resources/application.yml:65-70` — bloco `app.security.jwt`; local natural para a nova `app.security.msal.tenant-id`.
- `back-end/src/main/java/com/fatecrepository/controller/AuthController.java:26` — único handler: `POST /auth/login-microsoft`.

## Tasks & Acceptance

**Execution:**

Ordem obrigatória: TDD. Para cada tarefa com comportamento verificável, escreva o teste primeiro, confirme que falha pelo motivo certo (red), implemente o mínimo para passar (green), então limpe (refactor). Tarefas puramente de configuração — `.env`, `.env.example`, `Dockerfile`, `.dockerignore`, `docker-compose.yml`, `application.yml` — não têm harness de teste e são verificadas pelos comandos de `## Verification`.

- [x] `back-end/src/test/java/com/fatecrepository/service/AuthServiceTest.java` -- novo: `tid` da CPS aceito, `tid` externo → 401 sem chamar o Graph, `mail` nulo resolvido por UPN -- cobre a matriz acima no servidor.
- [x] `back-end/src/main/resources/application.yml` -- `app.security.msal.tenant-id` com default `eabe64c5-…` -- torna o tenant configurável sem recompilar.
- [x] `back-end/src/main/java/com/fatecrepository/service/MicrosoftGraphService.java` ~~adicionar `tid` a `GraphUser`~~ — **campo `tid` removido na revisão.** A checagem de tenant lê a claim `tid` do token, não o `GraphUser`; o campo nunca teve chamador.
- [x] `back-end/src/main/java/com/fatecrepository/service/AuthService.java` -- recusar `tid` fora da CPS antes de chamar o Graph e cair para `userPrincipalName` quando `mail` for nulo -- cumpre AD-3 e o critério de `epics.md:107`.
- [x] `front-end/.env.example` -- criar com as 4 vars `NG_APP_*`, authority `eabe64c5-…` e redirect `http://localhost:4200/` -- fecha a lacuna apontada por `README.md:24` e `.gitignore:49`.
- [x] `front-end/.env` -- redirect para `http://localhost:4200/` e remoção do comentário de `common` que contradiz o valor -- o tenant já está correto; só a barra e o comentário mentem.
- [x] `docker-compose.yml` -- authority do front-end para `eabe64c5-…` e derivação por `${MSAL_TENANT_ID:-eabe64c5-…}` / `${MSAL_REDIRECT_URI:-http://localhost:4200/}`; `MSAL_TENANT_ID` no `environment:` do backend -- uma única fonte de verdade, no padrão já usado para `JWT_*`.
- [x] `front-end/Dockerfile` -- defaults de ARG coerentes com o tenant e o redirect canônicos -- remove o fallback `common` e o `http://localhost` sem porta.
- [x] `front-end/.dockerignore` -- adicionar `.env` e `.env.*` -- impede o arquivo de desenvolvimento de entrar na imagem.
- [x] `front-end/src/environments/environment.ts` e `environment.development.ts` -- authority de fallback `eabe64c5-…` e redirect de fallback `http://localhost:4200/` -- nenhum build pode sair multi-tenant nem com URI divergente.
- [x] `front-end/src/app/core/auth/auth.service.spec.ts` -- novo: `conta` segura pré-init, `inicializar()` chama `initialize()` uma única vez, `no_token_request_cache_error` limpa `sessionStorage`, `loginInstituicao` não existe mais -- primeira cobertura de MSAL do projeto.
- [x] `front-end/src/app/core/auth/auth.service.ts` -- memoizar a inicialização em uma promessa, expor `quandoPronto()`, fazer `conta` devolver `undefined` enquanto não inicializou, e remover `loginInstituicao()` -- corrige C1 e elimina C3.
- [x] `front-end/src/app/app.spec.ts` e `front-end/src/app/shared/components/header/header.spec.ts` -- trocar o mock `loginPopUp` pelo método real e cobrir o header aguardando `quandoPronto()` -- o mock obsoleto mascara a ausência de cobertura.
- [x] `front-end/src/app/shared/components/header/header.ts` -- aguardar `quandoPronto()` antes de `carregarUsuario()` e reavaliar após o init -- corrige C2 (UI travada em "deslogado").

**Acceptance Criteria:**
- Given nenhum valor `NG_APP_*` definido, when o bundle é construído, then a authority resultante é `…/eabe64c5-68f5-4a76-8301-9577a679e449` e nunca `common`.
- Given todos os ambientes (`.env`, `.env.example`, `Dockerfile`, `docker-compose.yml`, `environment*.ts`), when se resolve authority e redirect URI, then os cinco valores são idênticos, incluindo a barra final.
- Given o front-end carregando, when `Header` lê a conta antes do MSAL inicializar, then nenhuma exceção é lançada e a UI reflete o estado de sessão após a inicialização.
- Given dois componentes chamando `inicializar()` no mesmo ciclo, when ambos executam, then `PublicClientApplication.initialize()` é invocado exatamente uma vez.
- Given `.env` de desenvolvimento, when a imagem Docker é construída, then o arquivo de desenvolvimento não está presente em nenhuma camada da imagem.
- Given um token Graph cujo `tid` não é `eabe64c5-…`, when `POST /auth/login-microsoft` é chamado, then a resposta é 401 e nenhuma chamada ao Graph é feita.
- Given um token do tenant da CPS de uma conta sem `mail`, when o login é processado, then o e-mail é resolvido por `userPrincipalName` e o JWT é emitido.
- Given qualquer arquivo sob `front-end/src`, when o módulo é carregado, then não existe referência a `POST /auth/login` nem a `loginInstituicao`.

## Implementation Notes

**Ordem de execução.** Implementado em duas frentes. Back-end primeiro (`AuthServiceTest` vermelho por tenant externo sem validação, depois verde com `validarTenantDaCps` + `resolverEmail`), depois front-end (`auth.service.spec.ts` vermelho no acesso antecipado ao cache, depois verde com a promessa memorizada), e por último os arquivos de configuração, que não têm harness.

**Arquivo fora da lista de tarefas: `microsoft-login-button.ts`.** A aplicação é zoneless — não há `zone.js` em `angular.json` nem `provideZoneChangeDetection` em `app.config.ts`. Sem `markForCheck()`, `inicializando = false` (atribuído depois de um `await`) nunca chega à tela e o botão fica permanentemente desabilitado, que é exatamente o sintoma que este spec existe para corrigir. A alteração foi mantida e `header.ts` recebeu o mesmo tratamento.

**A matriz nomeia `TenantInválidoException`, que não existe.** A implementação reutiliza `UnauthorizedException`, que já está mapeada para 401 no `GlobalExceptionHandler`, com a mensagem `"Tenant inválido: o token não pertence ao tenant da CPS"`. Criar uma classe de exceção para um único ponto de lançamento seria ruído. **Pendente de confirmação humana** — a linha da matriz é ambígua quanto ao nome da exceção, e o bloco é somente-leitura.

**Decisão de segurança registrada: token sem `tid` é aceito.** `extrairTenant()` devolve `null` quando o token não decodifica ou não tem `tid`, e `validarTenantDaCps()` retorna sem rejeitar. O app registration single-tenant já garante a origem, e um token forjado com `tid` correto morre na chamada ao Graph. Coberto por `deveAceitarTokenSemClaimTid`.

**Efeito colateral conhecido: promessa rejeitada fica memorizada.** Se `initialize()` falhar, `this.pronto` permanece rejeitado e `quandoPronto()` continua devolvendo a mesma rejeição — não há retentativa. Severidade baixa: um reload de página resolve. Não viola nenhum critério deste spec.

**Verificação executada (não delegada).** `npm test` → 8 arquivos, 33 testes, todos passaram. `mvn -B test` → 15 testes (3 `InstituicaoCsvParserTest` + 8 `AuthServiceTest` + 4 `ProjetoServiceTest`), 0 falhas, 0 erros, 0 ignorados. `npx ng build` → sucesso. `docker compose config` → `MSAL_TENANT_ID`, `NG_APP_MSAL_AUTHORITY` e `NG_APP_MSAL_REDIRECT_URI` interpolam para `eabe64c5-…` e `http://localhost:4200/`. `front-end/.env` verificado diretamente, já que é ignorado pelo git e não aparece em diff.

**`common` permanece no bundle de produção — falso positivo.** A verificação por grep em `dist/` encontra `https://login.microsoftonline.com/common/`, mas o contexto mostra que é a constante interna de `@azure/msal-browser` (`gO=…`, junto do log tag `msal.js.common`), presente em qualquer app MSAL. As fontes do projeto têm zero ocorrências; a authority efetiva vem do `environment`. O comando de verificação com `--glob '!node_modules'` está correto e passa.

**Defeito de ambiente corrigido: `mvnw.cmd` estava quebrado.** `.mvn/wrapper/maven-wrapper.jar` nunca foi versionado, então `mvnw.cmd` falhava com `Unable to access jarfile`. Os scripts versionados eram a geração antiga, incompatível com `distributionType=only-script`. Os scripts foram regenerados com `org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper`, que produz a distribuição `only-script` — **nenhum binário é commitado**, o wrapper apenas baixa o Maven na primeira execução. O `maven-wrapper.properties` foi restaurado com a URL versionada `apache-maven-3.9.16-bin.zip`, porque o plugin havia escrito `apache-maven-3-bin.zip`, que retorna 404 no repositório do Maven. Verificado: `./mvnw.cmd -v` reporta 3.9.16 e `./mvnw.cmd test` roda os 15 testes com BUILD SUCCESS.

**Nota: `mvnw` (script bash) e `mvnw.cmd` já eram versionados**; o que faltava era a regeneração para o formato `only-script`. Nenhum `.jar` foi adicionado ao repositório. **Correção posterior:** a revisão verificou que o `mvnw` de POSIX versionado já era `only-script` — a quebra era só do lado Windows. A revisão também restaurou um fallback de `JAVA_HOME` no `mvnw.cmd` regenerado, que o script upstream havia perdido.

**Pós-revisão: mutação em vez de confiança no verde.** Cada correção comportamental da revisão foi verificada por mutação — substituindo `equalsIgnoreCase` por `equals`, removendo `isBlank()`, removendo `.trim()`, removendo o reset de `pronto`, revertendo o guard de `conta`, removendo o `await quandoPronto()` de login/logout, e removendo o `markForCheck()` de cada componente. Todos os mutants foram detectados. Vale registrar o caso em que a primeira versão do teste de `conta` **não** detectou a mutação: como o reset de `pronto` faz "falhou" e "ainda não começou" parecerem iguais, o caso que realmente distingue é uma inicialização **pendente** (`pronto` atribuído sincronamente antes de `initialize()` resolver). Esse teste foi adicionado e é o que fixa o guard de forma independente.

**Contagem final de testes: 64.** Front-end 46 em 9 arquivos (`auth.service.spec.ts`, `microsoft-login-button.spec.ts`, `header.spec.ts`, `app.spec.ts` e as 5 specs de features/componentes pré-existentes). Back-end 18 em 3 classes (`AuthServiceTest` 11, `ProjetoServiceTest` 4, `InstituicaoCsvParserTest` 3). Todos verificados por mim diretamente após a revisão, não apenas pelo relatório do subagente.

**Correção de um comando de verificação mal formulado.** O padrão original `rg "loginInstituicao|/auth/login" front-end/src` casa por substring com `/auth/login-microsoft`, que é o endpoint legítimo do MSAL. Substituído por dois padrões ancorados.

## Spec Change Log

## Review Triage Log

Three layers run: `blind-hunter` (arithmetic: 48.92 kB → √≈6.99 → floor(+1)=7 → N=7), `edge-case-hunter`, `verification-gap`. Verdicts below are rendered from my own verification unless marked *filed*, meaning the verification-gap layer demonstrated the claim by mutating the code and re-running the suites.

**Grouped by root cause.** Every finding is represented; findings that several layers raised independently are merged into one row.

| # | Finding | Verdict | Evidence |
|---|---------|---------|----------|
| 1 | `deveCompararTidIgnorandoCase` never exercises case-insensitive matching: the uppercase value goes to the unread `GraphUser.tid`, while the token and the injected `tenantId` are both lowercase | `low` | *Filed and demonstrated* — swapping `equalsIgnoreCase` for `equals` left 15/15 green. Real test defect, but GUID casing never differs in practice, so no runtime harm today. → **patch** |
| 2 | `temValor`'s `isBlank()` half is unpinned: no test passes `""` or whitespace | `low` | *Filed and demonstrated* — simplifying `temValor` to `valor != null` left 15/15 green. → **patch** |
| 3 | `GraphUser.tid` is dead code — `getTid()` has no caller anywhere; `validarTenantDaCps` reads the JWT claim. Spec task rationale "habilita a checagem de tenant" is false | `low` | I grepped all of `back-end/src` for `getTid`: zero hits. The field is populated only by the test helper, which is what masked finding #1. Misleads the next reader into trusting Graph's tenant. → **patch** (delete field, correct the task rationale) |
| 4 | `this.pronto` memoizes a rejected promise, so one transient MSAL failure bricks login until a full reload; and `conta`'s guard tests `!this.pronto` (assignment) rather than success, so after a failed init it proceeds to `getAllAccounts()` on an uninitialized instance and throws | `medium` | Verified: `??=` assigns the promise object before it settles, so it is truthy after rejection. No-retry harm is reachable (any transient `initialize()` failure). The `conta` throw I could not reach from a current caller — `Header.iniciarSessao` and `MicrosoftLoginButton` both bail on error — so that half is latent, not live. → **patch** |
| 5 | `loginMicrosoft()` and `logout()` call `loginRedirect`/`logoutRedirect` without awaiting `quandoPronto()`; `Header.loginMicrosoft()` also drops the promise with no `.catch` | `medium` | Verified: `header.html:8-9` renders the login link under `@if (!logado)` and `logado` starts `false`, so the control is clickable during the `initialize()` window. Same failure mode as the bug this change exists to remove. → **patch** |
| 6 | `header.spec.ts` never pins the header's own `markForCheck()` — the child's `.finally` notification drives the render, so the change's stated justification is unverified | `medium` | *Filed and demonstrated* — deleting `header.ts:61` left 33/33 green; the notifier is `MicrosoftLoginButton`, whose notification sits behind a back-end round trip in the real app. This is the justification for the out-of-scope `markForCheck` change. → **patch** |
| 7 | `microsoft-login-button` has no spec at all, so the button leaving its permanent-disabled state is unasserted | `medium` | *Filed and demonstrated* — removing the `.finally` mark leaves 33/33 green; the only signal is an incidental `NG0100` in an unrelated header test. This is the exact symptom the change set exists to fix. → **patch** |
| 8 | `auth.service.spec.ts` covers none of `loginMicrosoft`, `logout`, `loginMicrosoftViaApi`, `obterUsuarioLogado`, and the MSAL double lacks `loginRedirect`/`acquireTokenSilent` | `medium` | Verified against the file. The gaps are where findings #4 and #5 live. → **patch** |
| 9 | `resolverEmail` does not `.trim()` and calls `toLowerCase()` without `Locale.ROOT` | `low` | Verified. A padded-but-non-blank value passes `isBlank()` and then fails `endsWith(DOMINIO_ALUNO)`, misclassifying a professor as `ALUNO` and creating a duplicate row. Turkish-locale hazard is not currently reachable (JVM locale is `pt_BR`) but is a latent correctness bug in new code. → **patch** |
| 10 | `deveClassificarPapelPorDominio` is named "aluno e professor" but only exercises the `externo@exemplo.com` fallback | `low` | Verified. The name overstates coverage of a security-relevant default. → **patch** |
| 11 | `header.spec.ts` never restores the `console.error` spy — it leaks into every later test | `low` | Verified: no `afterEach(vi.restoreAllMocks)`. → **patch** |
| 12 | `app.spec.ts`'s `AuthService` double omits `conta` and `loginMicrosoftViaApi`, which `MicrosoftLoginButton` calls | `low` | Verified. Passes only because `conta` is `undefined` on the double, so the call is never reached. → **patch** |
| 13 | `back-end/.env.example` not updated with the new `MSAL_TENANT_ID`; `Dockerfile` still defaults `NG_APP_MSAL_CLIENT_ID` to empty while its siblings got real defaults; READMEs omit the single-tenant prerequisite and the exact redirect URI | `low` | Verified. Developer-facing discoverability, each a direct consequence of this change introducing the variables. → **patch** |
| 14 | An `NG_APP_*` variable set to an empty string bypasses the `??` fallback, so MSAL receives an empty authority or redirect | `low` | Verified: `import.meta.env[...] as string \| undefined ?? 'default'` yields `''`, not the default. A silent misconfiguration, exactly the class this change set exists to prevent. → **patch** |
| 15 | No `.gitattributes` rule pins `*.cmd` to CRLF, so the regenerated `mvnw.cmd` can break on checkout | `low` | Verified: root `.gitattributes` is `* text=auto` only. The wrapper is now load-bearing for CI. → **patch** |
| 16 | Regenerated `mvnw.cmd` dropped the old `JAVA_HOME` fallback and `maven-wrapper.properties` has no `distributionSha256Sum` | `low` | Verified. Not currently harmful (this machine has `JAVA_HOME`; the checksum path is dead because the property is absent). → **patch** for the `.gitattributes` half; the checksum is → **defer** |
| 17 | The canonical tenant GUID is duplicated across `application.yml`, `docker-compose.yml` (×2), `Dockerfile`, both `environment*.ts`, `.env.example` and `AuthServiceTest`, with nothing reading two of the copies | `low` | *Filed with this disposition.* The two stacks build separately, so any test could only re-hardcode the GUID. → **defer** |
| 18 | `app.security.msal.tenant-id` binding is never verified — blanking the value in `application.yml` leaves 15/15 green while a real boot would fail to start the context | `medium` | *Filed and demonstrated*. The only real fix is a `@SpringBootTest`, which this repo has none of and which would need a live Postgres. → **defer** |
| 19 | `MicrosoftLoginRequest` still carries client-supplied `nome`, `email`, `fotoUrl` that the MSAL path never sends and `AuthService` never reads | `low` | Verified: pre-existing, present at baseline, untouched by this diff. → **defer** |
| 20 | The `no_token_request_cache_error` recovery clears `sessionStorage` but not MSAL's in-memory `BrowserCache`, so a same-load retry still fails; `Object.keys(sessionStorage)` is non-portable | `low` | Verified: both pre-existing at baseline and deliberately carried forward by this spec ("mantém o comportamento atual"). → **defer** |
| 21 | `environment.ts` — the production file — falls back to `http://localhost:4200/`, so a prod build missing `NG_APP_*` ships a localhost redirect | `low` | Verified: the localhost fallback is pre-existing; this change only updated its value. → **defer** |
| 22 | `isAdmin()` reads `localStorage`, so role is client-side only and trivially forgeable | `medium` | Verified: pre-existing, and explicitly out of scope per the spec's `Never` list and the Design Notes. Story 1-4. → **defer** |
| 23 | `Header` refreshes the user once, before `MicrosoftLoginButton` writes `localStorage`, so the first render can miss `usuarioRole`/`usuarioFoto` | `false` | Refuted: `auth.service.ts:36-67` — when `localStorage` is empty, `obterUsuarioLogado()` falls back to the MSAL `conta`, so the header does render the signed-in user. The claimed "stuck logged out" outcome does not occur. |
| 24 | The POSIX `mvnw` is untouched, so Linux CI still uses the legacy jar-based wrapper | `false` | Refuted: I grepped the committed `mvnw` for `maven-wrapper.jar` and `wrapperUrl` — zero hits. It was already the `only-script` distribution; only the Windows `mvnw.cmd` was stale. The breakage was Windows-only and is what I fixed and verified. |
| 25 | `Object.keys(sessionStorage)` is non-portable and should use `length` + `key(i)` | `false` | Refuted as harmful: it works in every browser this SPA targets, and finding #20 already defers the recovery path itself. |
| 26 | `markForCheck()` on a destroyed view after navigation leaks / throws in dev mode | `false` | Refuted: no demonstrated failure, and Angular 21 tolerates post-destroy `markForCheck()`. The rule is that loudly-failing code on unreachable states is correct behavior. |
| 27 | The recovery test seeds fabricated `msal.<id>-*` keys that do not match MSAL v5's real key format | `false` | Refuted: the assertion under test is the `startsWith('msal.')` prefix filter, which is exactly what the test proves. The key format is an implementation detail of MSAL, not of `AuthService`. |
| 28 | `resolverEmail` prefers `mail` even when it is outside the CPS domain while the UPN is inside it, so the fallback is never reached and a professor is stored as `ALUNO` | `low` | Verified as reachable in principle, but a CPS tenant account whose `mail` is off-domain and whose UPN is on-domain is not a state Graph produces. The tenant check already excludes outsiders. → **patch** the cheap part only (trim + locale, #9); no domain-sniffing added |

**Routing.** No `intent_gap` and no `bad_spec`: finding #3 was a wrong task *rationale*, but the fix (delete the field) is trivial, touches no public surface and is purely part of the diff, so it routes to `patch` per the cascade rule rather than a spec-level loopback. Twelve entries route to `patch`, nine to `defer`, seven rejected.



## Design Notes

**Ordem de inicialização do MSAL.** O MSAL Browser v3+ exige `await initialize()` antes de qualquer acesso ao cache. Em vez de espalhar `await` por todos os chamadores, `AuthService` guarda a própria promessa:

```ts
private pronto?: Promise<void>;

async inicializar(): Promise<void> {
  this.pronto ??= this.instance.initialize().then(() => this.instance.handleRedirectPromise());
  return this.pronto;
}

async quandoPronto(): Promise<void> {
  await (this.pronto ?? this.inicializar());
}

get conta(): AccountInfo | undefined {
  return this.instance.getAllAccounts()[0];
}
```

Com `quandoPronto()` aguardado no `Header.ngOnInit`, o acesso antecipado deixa de ocorrer, e `MicrosoftLoginButton` continua chamando `inicializar()` — que agora é idempotente.

**Validação de tenant no back-end.** O `tid` é lido com `com.auth0.jwt.JWT.decode(accessToken).getClaim("tid").asString()` — decodifica sem verificar, sem dependência nova. Isso é suficiente *e* honesto: `AuthService` só é alcançado depois que o token já foi aceito pelo app registration single-tenant, e a checagem de `tid` é defesa em profundidade exatamente como descreve AD-3. A comparação é `equalsIgnoreCase`, e o valor vem de `@Value("${app.security.msal.tenant-id}")` para permitir override por ambiente. Um token sem `tid` é aceito — o app registration single-tenant já garante que isso não ocorre, e rejeitar quebraria tokens legítimos de formatos futuros.

**Divergência de documentação conhecida.** `ARCHITECTURE-SPINE.md/.memlog.md:11` registra `2f8088c4-…` como o tenant da CPS. O humano confirmou que o app registration real está em `eabe64c5-…`. O `.memlog.md` é histórico e não será editado; a spine usa apenas a expressão "tenant GUID da CPS" e permanece válida. A correção de `docker-compose.yml:49` é a única mudança de config versionada necessária por essa divergência.

**Por que remover `loginInstituicao` em vez de implementar o endpoint.** `.memlog.md:16` lista `/auth/login` entre o que deve ser removido, `SPEC.md:66` declara login institucional fora de escopo, e a função não tem nenhum call site. Implementar o endpoint reintroduziria um fluxo que o produto rejeitou.

**Por que `SecurityConfig` fica intacto.** `SecurityConfig.java:80` lista `http://localhost:4200` como origem CORS. O header `Origin` que o navegador envia nunca traz barra final, então a entrada atual está correta — a barra canônica do redirect URI não se aplica a CORS.

## Verification

**Commands:**
- `cd front-end; npm test` -- esperado: 8 arquivos, 33 testes, todos passando.
- `cd back-end; ./mvnw.cmd test` -- esperado: 15 testes, 0 falhas, BUILD SUCCESS.
- `cd front-end; npx ng build` -- esperado: build de produção sem erro, com `eabe64c5-…` e `http://localhost:4200/` embutidos.
- `docker compose config` -- esperado: `MSAL_TENANT_ID`, `NG_APP_MSAL_AUTHORITY` e `NG_APP_MSAL_REDIRECT_URI` interpolados.
- `rg -n "login\.microsoftonline\.com/common" --glob '!node_modules'` -- esperado: **zero** ocorrências. Não aplicar ao bundle em `dist/`: o `@azure/msal-browser` embute a própria constante `common`, que é inerte porque a authority é sempre explícita.
- `rg -n "loginInstituicao" front-end/src` -- esperado: **zero** ocorrências fora do nome do teste que afirma a remoção.
- `rg -n "/auth/login(?!-microsoft)" front-end/src` -- esperado: **zero** chamadas de endpoint.
- `rg -n "2f8088c4|localhost:4200[^/]" front-end docker-compose.yml` -- esperado: **zero** ocorrências (exceto `SecurityConfig.java`, fora do escopo).
