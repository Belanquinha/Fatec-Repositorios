# Papéis

Matriz de papéis da plataforma, derivada do fluxo negocial definitivo. Contrato de referência para CAP-2 (classificação) e para as telas/regras que cada papel enxerga.

| Papel | Quem é | Como entra | O que pode |
|-------|--------|------------|------------|
| **Aluno (estudante)** | Membro do tenant CPS com UPN terminando em `@aluno.cps.sp.gov.br` (regra checada antes da de professor; demais domínios do tenant → aluno por fallback) | Login MSAL (tenant CPS) | Autenticar; postar projetos (que nascem diretamente como `AGUARDANDO_APROVACAO`); informar o e-mail do professor responsável (autocomplete + e-mail livre); registrar integrantes (nome, LinkedIn) e instituição; acompanhar estado de aprovação |
| **Professor** | Membro do tenant CPS com UPN de domínio `@cps.sp.gov.br` | Login MSAL (tenant CPS); classificado pelo back-end | Selecionar entre 0 e 4 instituições do catálogo oficial (opcional no 1º login, alterável a qualquer momento); aprovar/rejeitar projetos vinculados às suas instituições (rejeição exige `motivoRejeicao`) |
| **Admin do sistema** | Dono do projeto; e-mail semeado no banco | Login MSAL (tenant CPS); papel `ADMIN` persistido quando o e-mail está no seed | Manter o catálogo oficial de instituições (CRUD simples no MVP; importação fica pós-MVP) |
| **Visitante (Não Autenticado)** | Qualquer usuário externo da web | Acesso livre sem login | Pesquisar e visualizar os detalhes completos dos projetos com estado `APROVADO` |
| ~**Gestor**~ | ~~Login institucional de gestor~~ | ~~Papel removido~~ | ~~Tela de instituição como área de trabalho, aprovação de professores, cadastro de instituição~~ — eliminado por decisão (minimizar trabalho de secretaria/gestor) |

Notas:
- **Validação de professor** é a regra de domínio `@cps.sp.gov.br`; **aluno** é `@aluno.cps.sp.gov.br`, checado antes (sufixo termina em `.cps.sp.gov.br`); não existe lista separada de professores — o domínio É a lista.
- **Vínculo do professor com instituições** é de no mínimo 0 e teto 4 (opcional no primeiro login e alterável livremente).
- **Máquina de estados de projeto**: estado `RASCUNHO` removido. Estados válidos: `AGUARDANDO_APROVACAO`, `APROVADO` e `REJEITADO` (com `motivoRejeicao` obrigatório).
- **Integrante**: sem `papelNoProjeto` (MVP). Contém `nome` e `linkLinkedin`.
- **Professor responsável**: autocomplete de professores do tenant CPS + aceite de **e-mail livre** ("Professor Convidado") como registro informativo. **Sem envio de notificações no MVP.**
- **Instituição** é dado de catálogo mantido pelo admin, nunca texto livre do professor.
- **Acesso público**: Visitantes não autenticados têm acesso de leitura e busca para o catálogo de projetos `APROVADO`.