---
stepsCompleted: [1, 2, 3, 4]
inputDocuments: []
session_topic: 'Designing project-submission email notifications and an end-to-end professor testing flow'
session_goals: 'Evaluate the Resend notification design and determine how to test or simulate the professor receiving the alert, signing in, and approving a project without relying on a real professor mailbox.'
selected_approach: 'progressive-flow'
techniques_used: ['Question Storming', 'Role Playing', 'Solution Matrix', 'Decision Tree Mapping']
ideas_generated: []
context_file: ''
technique_execution_complete: true
facilitation_notes: 'Focused planning session; decisions were grounded in the existing submission, approval, and test-session flows.'
session_active: false
workflow_completed: true
---

# Brainstorming Session Results

**Facilitator:** Aluno
**Date:** 2026-11-16

## Session Overview

**Topic:** Designing project-submission email notifications and an end-to-end professor testing flow
**Goals:** Evaluate the Resend notification design and determine how to test or simulate the professor receiving the alert, signing in, and approving a project without relying on a real professor mailbox.

### Context Guidance

The project is a Spring Boot API and Angular frontend with Azure AD/MSAL authentication. Professor access can currently be exercised with a generated JWT test session.

### Session Setup

Use a progressive, implementation-focused exploration to map the user journey, identify design decisions and failure cases, then shape a practical testing plan.

## Technique Selection

**Approach:** Progressive Technique Flow
**Journey Design:** Move from clarifying unknowns to stakeholder experience, design comparison, and test planning.

**Progressive Techniques:**

- **Phase 1 - Question Storming:** Surface unknowns about recipient identity, submission timing, email content, and failure handling.
- **Phase 2 - Role Playing:** Walk through the student and professor experience, including blocked or confusing states.
- **Phase 3 - Solution Matrix:** Compare email-delivery and professor-test/simulation combinations against practical criteria.
- **Phase 4 - Decision Tree Mapping:** Map the chosen flow and its success, retry, invalid-recipient, delivery-failure, and approval branches.

**Journey Rationale:** The session is intended to produce a practical implementation and validation plan, so it prioritizes requirements, user experience, trade-off evaluation, and an executable test journey over open-ended idea generation.

## Technique Execution Results

### Question Storming

- The responsible-professor email is both the assigned reviewer identity and the production notification destination.
- The address must be institutional (`@cps.sp.gov.br`); approval remains an authenticated in-platform action.
- Only the professor whose authenticated email matches the address recorded on the project may approve it.
- If sending fails, the project remains submitted and pending review.

### Role Playing

- The student sees a confirmation that the project was submitted and is awaiting review; it does not claim that email was delivered.
- The professor receives minimal metadata (project title, submission date, and institution) and is asked to open the platform and sign in.
- The email does not deep-link to the project or approve it. The professor finds the item in the platform's pending list and takes an explicit approval action.

### Solution Matrix

- Selected first-version delivery: one post-commit Resend attempt, without retries.
- A send failure is recorded in backend logs; the student receives the normal submission confirmation.
- For manual testing, a development-only server-side recipient override sends to the developer's reachable inbox. Production continues to use the project's responsible-professor email.
- No external queue or durable outbox is in scope for the first version.

### Decision Tree Mapping

1. The student submits with `professor.teste@cps.sp.gov.br` as the responsible professor.
2. The backend validates and stores the project as `AGUARDANDO_APROVACAO`.
3. After the transaction commits, the backend makes one Resend request. In development, the configured test recipient redirects delivery to the developer's inbox.
4. On success, the professor opens the platform, signs in as the matching test professor, finds the project in the pending list, and approves it.
5. On send failure, the project remains pending, the failure is logged, and there is no retry or student-facing email warning.
6. A different professor identity must not see the project as assigned or approve it.

## Consolidated Implementation and Test Plan

### Implementation

1. Add a backend-only Resend sender and server-side configuration for the API key, verified sender address, and frontend base URL. Never expose the Resend key to Angular or commit it.
2. Send only after project persistence commits, so a mail-provider failure cannot roll back a successful submission. Use a single asynchronous attempt; explicitly log failures with the project ID and useful provider error details, without logging secrets.
3. In production, send to the stored `emailProfessorResponsavel`. Allow a separate test-recipient override only in a local/development profile; do not accept it from a request, and reject or ignore it in production configuration.
4. Keep the existing authorization rule: the signed-in professor must have the `PROFESSOR` role and an email matching the project's responsible-professor email. A domain check alone is not sufficient.
5. Keep the student confirmation independent of email delivery: the project is submitted and awaits review, whether or not Resend succeeds.
6. Send only minimal project metadata and instructions to open the platform. Do not include an approval token or a project-specific deep link.

### End-to-End Test

1. Configure the development-only recipient override to the developer's reachable inbox and start the local stack.
2. Submit a project as the test student with `professor.teste@cps.sp.gov.br` as the responsible professor.
3. Confirm the project is saved as `AGUARDANDO_APROVACAO` and the one Resend attempt reaches the override inbox with the expected minimal content.
4. Open the platform manually and use `python scripts/gerar-sessao.py professor` to authenticate as the fixed test professor.
5. Confirm the project appears in the professor's pending list, approve it, and verify its state changes to `APROVADO`.
6. Exercise a non-assigned professor and a send failure: approval must be denied for the other professor; a Resend failure must not undo the project or trigger another attempt.

The existing project service already validates and stores the responsible-professor email and checks the signed-in professor against it. Existing end-to-end authorization coverage is in [AvaliacaoProjetoE2ETest.java](../../back-end/src/test/java/com/fatecrepository/e2e/AvaliacaoProjetoE2ETest.java). The test session script [gerar-sessao.py](../../scripts/gerar-sessao.py) uses a fixed allowlisted professor identity (`professor.teste@cps.sp.gov.br`), rather than accepting arbitrary email identities.

### First-Version Limitation

With one attempt and no durable queue, a transient Resend failure or an application stop before the send can leave the professor without an alert. Backend logs are the only failure record in this version. If reliable delivery becomes a requirement, revisit a PostgreSQL outbox, retries, and an operational way to inspect or resend failed notifications.

## Idea Organization and Prioritization

### Theme 1: Preserve the existing review authorization

The project's responsible-professor email already identifies the reviewer. Keep that identity separate from the development-only delivery override. The signed-in professor must remain the assigned professor.

### Theme 2: Add a best-effort notification

Resend is a backend integration, triggered only after project persistence commits. The first version makes one attempt, does not retry, and records failures in backend logs without changing the project's submitted state.

### Theme 3: Test the real journey without a professor mailbox

Use the fixed CPS test professor identity and redirect only the development email delivery to the developer's reachable inbox. Then sign into the application as the test professor and complete the normal review and approval path.

### Priorities

- **Top priority:** Keep submission and approval authorization correct; never let email delivery failure undo a submitted project.
- **Quick win:** Reuse the existing fixed professor test account and current approval tests rather than making the JWT script accept arbitrary identities.
- **Test priority:** Verify the one-send behavior, failure logging, dev-only recipient override, and successful approval by the assigned test professor.
- **Deferred:** Durable notification tracking, retries, admin resend, and a database outbox.

## Action Plan

1. Configure a verified sender and backend-only Resend credentials for a non-production environment.
2. Add a server-side recipient override restricted to local/development configuration; keep the project email and authenticated professor identity unchanged.
3. Implement a post-commit, one-attempt Resend notification with explicit error logging. The submission response remains successful and pending on a send failure.
4. Test with a mocked sender that successful submission attempts one email, failed sending does not undo the project, and no retry occurs.
5. Run the manual end-to-end flow: submit as the test student, receive the alert in the override inbox, sign into the platform as `professor.teste@cps.sp.gov.br`, locate the pending project, and approve it.
6. Confirm that a different professor cannot approve the project and that production cannot use the development recipient override.

**Success indicators:** the project remains `AGUARDANDO_APROVACAO` regardless of email outcome; the email is attempted at most once; failures are visible in backend logs; the assigned test professor can approve; another professor cannot.

## Session Summary and Insights

The repository already has the core assigned-professor workflow and authorization checks, so the email feature can be added without redesigning project approval. The cleanest first version is a server-side, post-commit, one-shot notification with a development-only recipient override. Its trade-off is explicit: a failed or interrupted send can leave the professor without an alert, and recovery is not available until durable notification tracking is added later.

### Review Findings (code review 2026-11-16)

Revisão adversarial em 3 camadas (Blind Hunter, Edge Case Hunter, Acceptance Auditor)
sobre as mudanças não commitadas (22 arquivos, 1.250 linhas), com o diff e este
documento como entrada. 1 decisão, 11 patches e 3 deferrals — todos os itens abaixo
foram tratados ou registrados.

- [x] [Review][Decision] A trava do override de produção era inalcançável no deploy real (compose/Dockerfile não ativam profile). Resolvida como fail-closed: o override só é aceito com profile `dev`/`local` ativo, e o compose passou a declarar `SPRING_PROFILES_ACTIVE`. [ResendEmailConfig.java, docker-compose.yml]
- [x] [Review][Patch] `RestClient` do Resend sem connect/read timeout + `DiscardPolicy` que descartava sem log. [NotificacaoEmailConfig.java, AsyncConfig.java]
- [x] [Review][Patch] Bean `applicationTaskExecutor` sequestraria o executor do Boot (qualquer `@Async` sem qualifier dividiria o pool de 1-2 threads). Renomeado para `notificacaoExecutor`; `spring.task.execution.thread-name-prefix` morto removido. [AsyncConfig.java, NotificacaoProjetoService.java, application.yml]
- [x] [Review][Patch] E2E "exatamente uma notificação" usava `timeout()` (não pegava duplicidade); trocado para `after()` e adicionado teste de que rollback não dispara notificação. [NotificacaoProjetoE2ETest.java]
- [x] [Review][Patch] Status HTTP do provedor era capturado mas não logado. [NotificacaoProjetoService.java]
- [x] [Review][Patch] `url-base-front-end` interpolada sem escape no `href`/texto do link. [ResendEmailSender.java]
- [x] [Review][Patch] `remetente` com default divergente (`""` no Properties vs yml) e sem validação de blank. [ResendProperties.java, ResendEmailConfig.java]
- [x] [Review][Patch] Trava do override e seleção do bean sender passaram a ter asserções de causa e tipo de bean. [ResendEmailConfigTest.java, NotificacaoEmailConfigContextTest.java]
- [x] [Review][Patch] README voltou a documentar o bug de upload (data: URL Base64) que o AGENTS.md cita. [README.md]
- [x] [Review][Patch] `mvnw-local.sh` portátil (sem paths absolutos de IDE/wrapper) e executável. [mvnw-local.sh]
- [x] [Review][Defer] Passos 4-6 do teste E2E manual (professor.teste aprova / outro professor negado) seguem sem cobertura automatizada — o spec delega a `AvaliacaoProjetoE2ETest`.
- [x] [Review][Defer] E-mail do professor em log INFO/ERROR (LGPD; o resto da API faz scrub do campo).
- [x] [Review][Defer] `NotificacaoEmailSenderDesligado.enviar` é dead code e o boot sem key emite dois WARNs.

Verified clean: key nunca exposta ao front-end nem commitada; envio pós-commit com uma
tentativa e sem retry; falha de e-mail não desfaz a submissão; regra de autorização do
professor responsável preservada; corpo sem deep link de aprovação.
