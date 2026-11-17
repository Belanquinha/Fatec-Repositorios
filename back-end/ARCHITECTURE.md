# ARCHITECTURE — Fatec-Repositorios (Back-End)

> Contexto para agentes e desenvolvedores. Documenta a arquitetura ativa,
> o que funciona, o que está em planejamento e o que evitar.

## Visão geral

Aplicação Spring Boot 4.1 (Java 21), API REST, com autenticação via Azure AD (MSAL),
banco PostgreSQL, contador de uploads gerenciados por arquivo.

## Pilares da arquitetura

| Camada | Responsabilidade | Notas |
|---|---|---|
| **Controller** | Receber requisição, validar, delegar | Sem lógica de negócio |
| **Service** | Regras de negócio, orquestração | Um método só executa uma operação |
| **Repository** | Persistência JPA | Query methods, não QueryDSL |
| **Security** | Autenticação/MSAL, JWT, filtros | Todo acesso autenticado |
| **Config** | Configuração Spring, OpenAPI, MVC | Centralizado |
| **Seed** | Dados iniciais (CSV, JSON) | Carregados via `data.sql` |

## Fluxos principais

### Login (MSAL)
1. Front-end redireciona para Azure AD (tenant `eabe64c5-68f5-4a76-8301-9577a679e449`).
2. Azure AD retorna código de autorização → backend troca por token (MSAL).
3. Backend extrai UPN (e-mail) → `CustomUserDetailsService` carrega/usa `User`.
4. `JwtTokenProvider` assina token HS256 com `JWT_SECRET` do `.env`.
5. `JwtAuthenticationFilter` injeta `SecurityContext` em cada request.
6. `SecurityConfig` → todos os `POST /projetos` exigem `hasRole(ALUNO)` (ver bug registrada).

### Postagem de projeto
1. Aluno envia `ProjetoCreateRequest` (e-mail do professor responsável, instituição, integrantes).
2. `ProjetoService.criar()` salva com estado `AGUARDANDO_APROVACAO`.
3. `ProjetoRepository.save()` → trigger/DAO insere `data.sql`.
4. **Pós-commit**: `afterCommit` agenda notificação ao professor responsável
   (`NotificacaoProjetoService`, assíncrona, uma tentativa, sem retry). Falha de
   e-mail é logada e não desfaz a submissão. Entrega via Resend
   (`ResendEmailSender`), destino `emailProfessorResponsavel`; override de
   destinatário só em dev (`RESEND_OVERRIDE_DESTINATARIO`) — bloqueado em `prod*`.
5. Resposta `ProjetoResponse` com `id`, `titulo`, `estado`.

### Upload de arquivo (capa, avatar, etc.)
1. `UploadController` recibe multipart → `FileStorageService`.
2. `FileStorageService.store()` salva em `/app/uploads` (volume Docker).
3. Retorna `UploadResponse` com URL para o front.
4. **NOVO**: verificar erros de upload antes de concluir (ver `README.md` no root).

## Modelos essenciais

| Entidade | Estados principais |
|---|---|
| `Projeto` | `AGUARDANDO_APROVACAO`, `APROVADO`, `REJEITADO`, `EM_REMOCAO`, `REMOVIDO` |
| `User` | `ALUNO`, `PROFESSOR`, `ADMIN` (roles) |
| `Instituicao` | Catálogo de instituições Fatec/CPS |
| `Integrante` | Participantes de um projeto |
| `ProfessorInstituicao` | Vinculação professor ↔ instituição |

## Banco

- PostgreSQL 17 (Docker, porta `5433` local)
- Schema: `schema.sql` (cria tabelas)
- Seeds: `data.sql` + `back-end/init/` (SQL + geração de instituições)
- `application.yml` — datasource, jpa, flyway/seeder, multipart uploads

## Configuração de ambiente

`.env` na raiz (nunca versionar). Variáveis essenciais:

```bash
POSTGRES_PASSWORD=<gerado>
JWT_SECRET=<32+ chars>
JWT_ISSUER=fatec-repository-api
JWT_EXPIRATION_HOURS=24
MSAL_TENANT_ID=eabe64c5-68f5-4a76-8301-9577a679e449
MSAL_CLIENT_ID=146c36f9-abf3-48b0-a533-5f462e5e4eed
RESEND_API_KEY=<opcional; vazio = sem alerta por e-mail>
RESEND_REMETENTE=naoresponda@cps.sp.gov.br
RESEND_OVERRIDE_DESTINATARIO=<só dev>
```

## O que está funcionando (confirmados)

- ✅ Login MSAL via Azure AD (rotor, roles baseados em domínio)
- ✅ Classificação de papel por domínio de e-mail (centralizada no backend)
- ✅ Seleção 0–4 instituições pelo professor (catálogo)
- ✅ Postagem de projeto com estado `AGUARDANDO_APROVACAO`
- ✅ Notificação de submissão ao professor (Resend, pós-commit, best-effort)
- ✅ Upload de arquivos (capa, avatar) com volume Docker
- ✅ API Springdoc OpenAPI (Swagger) na rota `/swagger-ui.html`
- ✅ Seed de instituições (CSV parse + SQL)

## O que está em planejamento / bug registrados

- 🔧 `POST /projetos → authenticated()` sem `hasRole` (security bug)
- 🔧 Upload de imagem: retroalimentação de erro não propagada em `upload-projeto.ts:374`/`:619`
- 🔧 Exclusão/remoção de projetos (LGPD) — mais especificado em `_bmad-output/specs/`
- 🔜 Exclusão/anonimização de projetos (LGPD Art.18)

## Regras para agentes

- Nunca alterar `JWT_SECRET` para um valor pré-definido.
- Nunca adicionar regras de role no front-end — tudo decisão do backend.
- Quando adicionar feature, verificar se já existe SPEC no `_bmad-output/specs/`.
- Não criar migrações específicas de banco — usar `schema.sql` + seeds.
