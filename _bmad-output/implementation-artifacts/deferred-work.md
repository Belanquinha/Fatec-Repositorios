# Deferred Work

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: The canonical CPS tenant GUID is duplicated across the two build stacks, and nothing verifies the copies agree.
  evidence: The GUID lives in `back-end/src/main/resources/application.yml`, `docker-compose.yml` (twice), `front-end/Dockerfile`, both `environment*.ts` fallbacks, `front-end/.env.example` and `AuthServiceTest`. No test reads two of those copies. If the app registration moves tenants and someone updates `application.yml` but not the front-end fallbacks, the front-end requests a token from the old tenant and the back-end 401s every login while all 64 tests stay green. Settling it properly needs a single runtime source of truth (e.g. a served config file), not a test — a test could only re-hardcode the GUID on the other side.

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: The `app.security.msal.tenant-id` property placeholder is never bound by any test.
  evidence: Demonstrated: blanking the value in `application.yml` leaves the whole back-end suite green, but a real boot cannot create `AuthService` and the entire API fails to start. The only real fix is a `@SpringBootTest`, which this repository has none of, and which would require a live Postgres because `spring.sql.init.mode=always` and `ddl-auto=update` run at context load.

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: `MicrosoftLoginRequest` still accepts client-supplied identity fields that the MSAL flow never sends and the back-end never reads.
  evidence: Pre-existing at baseline. The DTO carries `nome`, `email` and `fotoUrl` alongside `accessToken`; `AuthService.loginMicrosoft` reads none of them, taking all identity from the Graph response instead. Leaving them invites a future developer to trust a client-supplied identity on an authentication request.

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: The `no_token_request_cache_error` recovery clears `sessionStorage` but not MSAL's in-memory `BrowserCache`, so a retry within the same page load still fails.
  evidence: Pre-existing at baseline and deliberately carried forward by this spec ("mantém o comportamento atual"). `AuthService.inicializado()` removes `msal.*` keys from `sessionStorage` only. A real recovery would call the instance's cache-clearing API and re-initialize. `Object.keys(sessionStorage)` is also a non-portable way to enumerate a `Storage` object, though it works in every browser this SPA targets.

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: The production `environment.ts` falls back to a localhost redirect URI, so a production build missing `NG_APP_*` ships a bundle pointing at a real tenant with a localhost redirect.
  evidence: The localhost fallback is pre-existing; this change only updated its value. The file makes no distinction between "dev fallback" and "required in production", and `environment.ts` and `environment.development.ts` are now identical for the MSAL fields, which makes the `fileReplacements` entry in `angular.json` pointless for those keys. Worth splitting into required-in-prod versus defaulted-in-dev.

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: Role-based authorization is enforced only client-side, so `isAdmin()` is forgeable.
  evidence: Pre-existing and explicitly out of scope per this spec's `Never` list. `AuthService.isAdmin()` reads `localStorage.usuarioRole`, and `admin.guard.ts` is not even referenced by `app.routes.ts`. A user can set that key and render any admin UI. Server-side enforcement is epic 1, story 1-4, still `backlog` in `sprint-status.yaml`.

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: Changing `MSAL_TENANT_ID` or `MSAL_REDIRECT_URI` requires rebuilding the front-end image, not just restarting the stack.
  evidence: The front-end receives these as Docker build args baked into the bundle at `npm run build` time, so `docker compose up` without `--build` silently serves the previous tenant. The back-end picks the value up from its environment and so restarts correctly, making the mismatch asymmetric and hard to diagnose. Runtime config (a served `config.json`) would remove the asymmetry.

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: The regenerated `mvnw.cmd` supports `distributionSha256Sum` verification but the property is absent, so the downloaded Maven is never integrity-checked.
  evidence: The wrapper script gained the checksum path when it was regenerated to the `only-script` distribution, but `maven-wrapper.properties` sets no `distributionSha256Sum`, making that code a no-op. This is a supply-chain hardening opportunity the regeneration made available, not a regression — the previous wrapper was not checking either.

- source_spec: `_bmad-output/implementation-artifacts/spec-fix-msal-tenant-login.md`
  summary: A domain-scope check is absent, so a CPS-tenant account whose `mail` is off-domain while its UPN is on-domain is classified by the wrong address.
  evidence: `resolverEmail` prefers `mail` whenever it is non-blank, falling back to `userPrincipalName` only when `mail` is null or blank. If `mail` were set to an address outside both CPS domains while the UPN ends in `@cps.sp.gov.br`, the user would be stored as `ALUNO` instead of `PROFESSOR`. Graph does not produce this combination for CPS accounts, and the tenant check already excludes outsiders, so no realistic harm was demonstrated — recorded rather than patched because the obvious fix (sniffing the domain inside `resolverEmail`) duplicates the role rule.
