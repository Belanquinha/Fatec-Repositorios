---
title: Elicitação Avançada — Refinamento de UX/UI e Telas
created: 2026-09-22
updated: 2026-09-22
status: final
requisito-original: "pode ver os documentos e me ajudar?"
metodo: bmad-advanced-elicitation (socrático + pre-mortem)
afeta:
  - DESIGN.md
  - _bmad-output/planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - _bmad-output/specs/spec-fatec-repositorios/SPEC.md
  - _bmad-output/planning-artifacts/epics.md
---

# Elicitação Avançada — Refinamento de UX/UI e Telas

## 1. Requisito refinado

**Declaração (testável):** As documentações BMAD (PRD, SPEC, epics, DESIGN.md) devem especificar, com profundidade suficiente para agentes (Sally/Amelia) implementarem **sem improvisar**, todos os fluxos e telas por papel — incluindo os hoje ausentes: **landing pós-login MSAL, tela de Perfil (aluno e professor) e o fluxo professor (selecionar instituições → fila de aprovação)**, com a jornada de primeiro login diferenciada do acesso recorrente.

**Decisões de elicitação (entrevista 2026-09-22):**
- Foco é **documentação**, não código (o projeto está sendo replanejado e refeito com BMAD; telas antigas foram removidas por não servirem ao projeto).
- Refactor de código está em andamento; build não é prioridade (apenas front por enquanto).
- MSAL pendente (placeholder/mock no código é aceitável por ora); demo próxima, logo as telas precisam estar bem especificadas para serem construídas rápido.

## 2. Achados — lacunas de especificação encontradas

1. **Não há landing/callback pós-login especificado.** O DESIGN.md (§4.1) descreve fluxos por papel mas não define para onde cada papel vai ao retornar do MSAL, nem diferencia **primeiro login** (onboarding) de **retorno**.
2. **Não existe tela de Perfil** (aluno e professor) em nenhum artefato — o usuário espera que, por estar logado/cadastrado, cada usuário tenha um perfil.
3. **Tela de Perfil ausente do mapa de rotas (§5)** — não há rota, nem regra de edição de dados.
4. **Especificações §4 de DESIGN.md são rasas**: cada tela é uma lista de elementos; não há profundidade como a da Story 3.1 absorvida (estados por tela, validações pontuais, micro-interações, cópia de empty state).
5. **Fila de aprovação (4.4.2)** já tem *sketch*: badge "Você é o responsável", modal de rejeição com motivo obrigatório. Falta definir quem pode aprovar (ver Open Question do PRD §8.2).
6. **Onboarding do professor**: DESIGN.md diz que a seleção de instituições "não é obrigatória no 1º login", mas o usuário espera um fluxo claro de "retorna do login → seleciona instituições". As duas leituras precisam ser reconciliadas e tornadas explícitas.
7. **DESIGN.md referencia telas já removidas do código** (`ver-projeto`, `admin-main`, `cadastro-instituicao`) — válidas como especificação do redesign, mas sem nota explícita de que são "a reconstruir".

## 3. Pre-mortem (revisado para o plano de documentação)

> "É a demo/apresentação. O projeto falhou. Por quê?"

1. **Agentes improvisaram telas inconsistentes.** A spec rasa deu margem: duas implementações divergentes da mesma tela, padrões quebrados (estados/validação), retrabalho. Mitigação: padrão de profundidade por tela.
2. **Onboarding do professor quebrou.** Primeiro login sem fluxo claro → professor não associa instituições → fila vazia → nada é aprovado → demo ponta a ponta morre. Mitigação: especificar explicitamente fluxo pós-login + estado vazio orientando.
3. **Tela de Perfil esquecida no backlog.** Usuário descobre na demo que "não tem onde ver meus dados". Mitigação: adicionar às rotas e ao MVP.
4. **Premissa de domínio falha na demo real** (Open Question §8.1/'Assumptions' do SPEC): se professor da Fatec usa outro domínio, classificação falha. Mitigação: validar com orientador **antes** de fechar telas de papel.
5. **"Quem aprova" indefinido** (SPEC Open Question 2): se só o professor responsável pode aprovar, a fila perdida em qualquer instituição gera trava de aprovação. Mitigação: decidir e refletir nas telas/regras.

## 4. Decisões em aberto (socráticas)

~~Registradas na sessão; respostas alimentam as seções 5 e 6.~~ **Resolvidas em sessão (2026-09-22):**

- **D-1 — Landing pós-login:** Professor **sem instituições vinculadas** cai **direto na Seleção de Instituições** (primeiro login = onboarding); professor **já com vínculos** vai para a **home/fila**. **Aluno** vai para **Meus Projetos**. **Admin** vai para `admin-main`. Visitante permanece (não autentica).
- **D-2 — Perfil (MVP):** Dados do MSAL (nome, e-mail institucional, papel) **+ contexto do papel**: aluno vê link para Meus Projetos; professor **vê e edita suas instituições (0 a 4)** — edição de vínculos também disponível no perfil.
- **D-3 — Onboarding do professor:** **Opcional com destaque** — nunca bloqueia; mantém a regra do SPEC ("não obrigatório no 1º login"), mas o 1º acesso sem vínculos cai na seleção com copy orientativa.
- **D-4 — Quem aprova:** **Qualquer professor vinculado à instituição do projeto** pode aprovar/rejeitar (abre a Open Question do PRD/SPEC). O professor com `emailProfessorResponsavel` fica apenas **destacado** na fila.
- **D-5 — Profundidade:** cada tela segue o padrão da Story 3.1 absorvida (propósito, elementos, estados — loading/vazio/erro/sucesso, validação pontual, micro-interações, cópia).

## 5. Plano de atualização (próximos passos do BMAD)

| Artefato | Ação | Comando BMAD |
| :--- | :--- | :--- |
| `DESIGN.md` | Reescrever §4 com profundidade por tela + novo capítulo de Perfil + fluxo pós-login/onboarding | `bmad-ux` |
| `prd.md` | Resolver Open Questions (domínio, quem aprova) e refletir novas telas/perfil | `bmad-prd` |
| `SPEC.md` | Ajustar capacidades/restrições se D-1/D-2/D-4 mudarem contratos | `bmad-spec` |
| `epics.md` | Adicionar stories de Perfil e de fluxo/onboarding do professor | `bmad-create-epics-and-stories` |

## 6. Resultado consolidado

- O requisito original era uma *intenção de ajuda*, não um requisito. Via elicitação: **"documentar as telas e fluxos por papel com profundidade implementável, incluindo Perfil e jornada do professor"**.
- As 4 respostas da entrevista confirmam: **90% do esforço agora é de especificação UX/UI**, não de código.
- As telas de perfil inexistentes e o onboarding do professor são as **duas maiores ameaças de falha na demo** (ver pre-mortem 2 e 3).
- **Decisões fechadas em sessão** (D-1 a D-5): landing pós-login por papel; Perfil = dados MSAL + contexto do papel; onboarding opcional com destaque; qualquer professor da instituição aprova; padrão de profundidade por tela.

> **Aplicação nos artefatos (2026-09-22):** DESIGN.md atualizado (tela de Perfil, fluxo pós-login, profundidade das telas do professor, mapa de rotas); PRD e SPEC tiveram a Open Question "quem aprova" resolvida (D-4). Próximos passos: `bmad-ux` para detalhar demais telas na nova profundidade; `bmad-create-epics-and-stories` para adicionar stories de Perfil e onboarding do professor.