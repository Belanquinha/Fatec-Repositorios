# Sprint Plan — Fatec-Repositorios

- **Sprint goal:** entregar as duas superfícies desenhadas na rodada de UX (Home pública e hub de Perfil), sobre a fundação de autenticação e o catálogo.
- **Data:** 2026-09-23
- **Estado de partida:** correção de curso (correct-course 2026-09-23) aprovada e consolidada em SPEC/PRD/epics/arquitetura/sprint-status. Epics 3/4 desbloqueados pela consolidação.

## Escopo

### Dentro do alcance agora (próxima fase de build)
| Prioridade | Story | FRs | Por quê agora |
|---|---|---|---|
| P0 | Concluir **2-1** (Admin mantém catálogo) | FR7 | Já in-progress; story file existe |
| P0 | **Epic 1** (1-1..1-4: MSAL, papel, sessão, autorização) | FR1-FR4 | Portão: papel do backend é a fonte da renderização do perfil (AD-1); rota `/perfil` protegida exige guard |
| P1 | **6-1** Perfil hub + landing única | FR13, FR14 | Entrega desta rodada UX; superfície de apresentação de Epic 3/4 |
| P1 | **5-1/5-2** Home/vitrine pública | FR12 | Entrega desta rodada UX; sem dependência de autenticação |

### Dentro do alcance logo em seguida (dependem da fase acima)
| Prioridade | Story | FRs | Dependência |
|---|---|---|---|
| P2 | **2-2** Professor lista catálogo oficial | FR5 | backend catálogo (2-1) |
| P2 | **2-3** Professor seleciona 0-4 instituições | FR6 | surface = aba "Instituições (0–4)" do Perfil → **6-1**; catálogo (2-2) |
| P2 | **3-1/3-2** Aluno publica projeto + professor responsável | FR8, FR9 | CTA/surface "Meus projetos" → **6-1**; instituição do catálogo (2-2) |
| P2 | **4-1/4-2** Fila "Pendentes" + aprovar/rejeitar | FR10, FR11 | surface = aba do Perfil → **6-1**; vínculos do professor (2-3) |

### Fora de escopo (pós-MVP)
- Importação de catálogo em runtime, notificações, perfil público, gestor.

## Prontidão (verificada em 2026-09-23)

- Stories com AC, papel e regras definidos em `epics.md`: **implementáveis**.
- Story files existentes: somente `story-2-1-*`. Demais ainda `backlog` (somente no epics.md).
- `sprint-status.yaml` **desatualizado**: faltava **Epic 5** (5-1/5-2) — re-adicionado.
- Rótulo 2-3 corrigido para "0-a-4" (já refletido) e 4-1 re-ancorado para "aba do Perfil" (já refletido).

## Bloqueios e riscos

1. **Endpoints de leitura das abas (arquitetura em aberto):** o hub consome dados server-derived (AD-1) e hoje o SPRINE não mapeia os read-models de `meus-projetos` (dono, 3 estados), `fila-do-professor` (instituições), `aprovados-do-professor` (histórico) e `instituicoes-do-professor`. **Resolver antes de dev de 6.1** (front precisa do contrato).
2. **Epic 1 sem story files:** fundação de autenticação é pré-requisito para rota protegida `/perfil` e para o papel na UI (AD-1). Criar story files ao iniciar dev.
3. **Raiz da SPA:** `app.routes.ts` hoje redireciona `'' → projeto-forms`; a Home substitui isso ([assumption] UX, decisão de implementação da 5-1).
4. **Epic 5 agora no tracking:** re-adicionado ao `sprint-status.yaml` para não ficar órfão da rodada UX.

## Definition of Done (sprint)

- Raiz `/` renderiza a Home (vitrine pública), sem redirect para `projeto-forms`.
- `/perfil` protegido por guard de sessão; retorno do MSAL aterrissa em `/perfil` (landing única, idempotente, onboarding professor não bloqueante).
- Abas do perfil dirigidas por papel (ALUNO/PROFESSOR), identidade MSAL somente leitura; nenhuma superfície como rota paralela (rotas: `/`, `/perfil`, `/projeto-forms`, `/projetos/:id`, `/admin`).
- Fonte Inter aplicada; cantos retos; paleta CPS semântica conforme DESIGN.md.
- Backend: nenhuma mudança de modelo/regras; endpoints de leitura das abas mapeados em arquitetura e cobertos por teste.
- Lint/build verdes (front + back); `docker-compose up` sobe (NFR6).
- `sprint-status.yaml` em dia; retrospectivas dos épicos fechados concluídas (optional).

## Roteiro de execução sugerido

1. Fechar **2-1**; criar story files e dev de **Epic 1** (mínimo 1-2 papel + 1-4 guard) e **5-1/5-2** (Home).
2. Mapa de endpoints das abas → atualizar ARCHITECTURE (sync de CAP-8/read-models) → dev de **6-1**.
3. Na sequência de 6-1: **2-3 → 3-1/3-2 → 4-1 → 4-2** (superfícies já estão prontas como abas).