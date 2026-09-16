# Papéis

Matriz de papéis da plataforma, derivada do fluxo negocial definitivo. Contrato de referência para CAP-2 (classificação) e para as telas/regras que cada papel enxerga.

| Papel | Quem é | Como entra | O que pode |
|-------|--------|------------|------------|
| **Aluno (estudante)** | Membro do tenant CPS com UPN terminando em `@aluno.cps.sp.gov.br` (regra checada antes da de professor; demais domínios do tenant → aluno por fallback) | Login MSAL (tenant CPS) | Autenticar; postar projetos; selecionar o professor responsável ao postar; acompanhar estado de aprovação |
| **Professor** | Membro do tenant CPS com UPN de domínio `@cps.sp.gov.br` | Login MSAL (tenant CPS); classificado pelo back-end | Selecionar de 1 a 4 instituições do catálogo oficial; aprovar/rejeitar projetos vinculados às suas instituições |
| **Admin do sistema** | Dono do projeto | Cadastro administrativo (fora do fluxo MSAL) | Manter o catálogo oficial de instituições (importar/curar dados oficiais) |
| ~**Gestor**~ | ~~Login institucional de gestor~~ | ~~Papel removido~~ | ~~Tela de instituição como área de trabalho, aprovação de professores, cadastro de instituição~~ — eliminado por decisão (minimizar trabalho de secretaria/gestor) |

Notas:
- **Validação de professor** é a regra de domínio `@cps.sp.gov.br`; **aluno** é `@aluno.cps.sp.gov.br`, checado antes (sufixo termina em `.cps.sp.gov.br`); não existe lista separada de professores — o domínio É a lista.
- **Instituição** é dado de catálogo mantido pelo admin, nunca texto livre do professor.
- Padrão de decisão em aberto: aprovação pode vir só do professor responsável (recomendado) ou de qualquer professor da instituição; e moderação/disputas ainda não tem dono.