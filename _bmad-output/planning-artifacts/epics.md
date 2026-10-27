---
stepsCompleted: [1, 2, 3, 4]
updated: 2026-10-20
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - _bmad-output/specs/spec-fatec-repositorios/SPEC.md
  - _bmad-output/specs/spec-fatec-repositorios/roles.md
  - _bmad-output/planning-artifacts/architecture/architecture-Fatec-Repositorios-2026-10-20-editor-tiptap-e-midia/ARCHITECTURE-EDITOR-TIPTAP-E-MIDIA.md
---

# Fatec-Repositorios - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Fatec-Repositorios, decomposing the requirements from the PRD, the canonical SPEC, and architecture constraints into implementable stories.

**Atualização 2026-10-20:** o Épico 7 (Editor TipTap e documentação em HTML) e o Épico 8 (pipeline de mídia, limites e performance) decompõem a arquitetura `ARCHITECTURE-EDITOR-TIPTAP-E-MIDIA.md`. As stories 3.1 e 5.2 foram reconciliadas com o novo contrato `conteudoHtml`; o histórico de numeração dos Épicos 1–6 permanece intacto.

**Convenção de identificadores:** `ST-<epic>.<story>` (ex.: `ST-7.1`). As stories legadas `Story n.m` equivalem a `ST-n.m` (`Story 3.1` = `ST-3.1`). As stories novas trazem o ID explícito no heading.

**Prioridades:** `P0` = bloqueia a migração/publicação; `P1` = necessária para o Definition of Done, pode entrar logo após o P0; `P2` = adiada/opcional, registrada para não se perder.

## Requirements Inventory

### Functional Requirements

```
FR1: Login with CPS account via MSAL (tenant-restricted); non-CPS accounts cannot authenticate.
FR2: Session and logout; protected resources require valid token.
FR3: Backend classifies role by email domain: UPN ending in @aluno.cps.sp.gov.br -> Aluno (checked first); UPN ending in @cps.sp.gov.br -> Professor; any other CPS tenant member -> default Aluno.
FR4: Role enforced server-side on all authorized operations (403 for wrong role).
FR5: Professor lists the official institution catalog (no free-text creation).
FR6: Professor selects 0 to 4 institutions (optional at first login); a 5th selection is refused.
FR7: Admin maintains the official institution catalog (import/curate).
FR8: Student creates and publishes a project born directly as AGUARDANDO_APROVACAO (no draft state).
FR9: Student must provide emailProfessorResponsavel (free-text e-mail — no front-end autocomplete since the 2026-10-26 decision; non-registered addresses accepted as "Professor Convidado"), instituicaoId, complete project attributes, and project Integrantes (nome, linkLinkedin).
FR10: Professor sees an approval queue of projects tied to their selected institutions.
FR11: Professor approves or rejects a project (rejection requires mandatory motivoRejeicao); state change visible to student.
FR12: Unauthenticated visitors search and view details of APROVADO projects without login.
FR13: User sees profile as hub: MSAL identity (read-only) and role-driven tabs/shortcuts (aluno: Meus Projetos; professor: Pendentes, Aprovados, Instituições 0–4; admin: Admin Main).
FR14: After MSAL login, every role lands on the Perfil with tab/shortcut preselected by role state (aluno → Meus Projetos; professor with 0 links → Instituições onboarding; professor with links → Pendentes; admin → Admin Main); idempotent first access.
```

### NonFunctional Requirements

```
NFR1 (Security): Login restricted to the single CPS tenant; personal/other-tenant Microsoft accounts are rejected.
NFR2 (Security): Role classification is centralized and authoritative in the back-end; never trusted from the front-end. Classificação por sufixo de domínio: aluno = @aluno.cps.sp.gov.br (checado primeiro), professor = @cps.sp.gov.br; demais membros do tenant -> aluno.
NFR3 (Constraint): Hard cap of 0 to 4 institutions per professor, enforced server-side. Selection optional at first login.
NFR4 (Constraint): Institutions are catalog data maintained by the system admin; no free-text institution input by professors.
NFR5 (Constraint): Project has no RASCUNHO state; starts directly as AGUARDANDO_APROVACAO. Rejection requires mandatory motivoRejeicao.
NFR6 (Deployment): The stack runs via docker-compose (front-end + back-end + database).
```

### Architecture Requirements — Editor TipTap e mídia (2026-10-20)

Inventário das decisões que os Épicos 7–8 precisam satisfazer. A fonte normativa é `ARCHITECTURE-EDITOR-TIPTAP-E-MIDIA.md`.

| ID | Decisão | Efeito verificável |
|---|---|---|
| AD-ED-1 | TipTap (ProseMirror) substitui o Editor.js | Paste de `.docx` preserva títulos, listas, parágrafos e imagens; `@editorjs/*` removido |
| AD-ED-2 | Conteúdo persistido como HTML sanitizado (`conteudoHtml`) | jsoup na escrita; coluna física legada `conteudo_editor_js`; leitura via HTML |
| AD-ED-3 | Imagens são nós `<img>` do documento; sem `imagensExtras` | Teto rígido de 18 imagens distintas; 0 permitido; deduplicação por `src` |
| AD-ED-4 | Compressão no cliente + normalização no servidor | Entrada 5MB; saída JPEG 1600px/q0.82 (capa) ou q0.80 (inline); thumb 480px; saída ≤1MB; WebP suportado |
| AD-ED-5 | Tetos de transporte coerentes | nginx `15m`; Spring multipart `12MB`; payload de projeto `2MB`; imagem 5MB entrada/1MB saída |
| AD-ED-6 | Cache imutável para `/uploads/` | `Cache-Control: public, max-age=31536000, immutable` |
| AD-ED-7 | Formulário em rota lazy e remoção do Editor.js | `projeto-forms` via `loadComponent`; TipTap fora do bundle inicial |
| AD-ED-8 | Autosave local em IndexedDB, sem estado `RASCUNHO` | Chave `rascunho-projeto:<email>`, versão de schema, restauração e limpeza |
| AD-ED-9 | Faxineiro de arquivos órfãos com carência de 7 dias | Remove upload não referenciado por capa ou `conteudoHtml` e com mtime > 7 dias |
| AD-ED-10 | Limites anunciados pelo servidor (recomendado) | `GET /uploads/policy`; se ausente, teste de paridade é obrigatório |

**Requisitos transversais da arquitetura:** §3.2 (Safelist jsoup), §4.1 (fluxo de submissão), §4.2 (paste de `.docx`), §4.3 (restauração de rascunho), §5 (seed/reset do banco) e §7 (Definition of Done).

### FR Coverage Map

FR1: Epic 1 - Login MSAL restrito ao tenant CPS  
FR2: Epic 1 - Sessão e logout  
FR3: Epic 1 - Classificação de papel por domínio  
FR4: Epic 1 - Autorização por papel no servidor  
FR5: Epic 2 - Listar catálogo oficial  
FR6: Epic 2 - Seleção de 0 a 4 instituições  
FR7: Epic 2 - Admin mantém catálogo  
FR8: Epic 3 - Postagem de projeto diretamente como AGUARDANDO_APROVACAO + Epic 7 - Conteúdo HTML sanitizado  
FR9: Epic 3 - E-mail do professor responsável, atributos e integrantes no post + Epic 7 - Documentação rica  
FR10: Epic 4 - Fila de aprovação por instituição  
FR11: Epic 4 - Aprovar/rejeitar com motivoRejeicao obrigatório  
FR12: Epic 5 - Catálogo público para visitantes + Epic 7 - Render HTML + Epic 8 - Performance do detalhe  
FR13: Epic 6 - Perfil hub (identidade MSAL somente leitura + abas por papel)  
FR14: Epic 6 - Landing única no Perfil por estado do papel  

NFR5: Epic 7 - Autosave local não cria `RASCUNHO` no servidor; projeto só nasce na publicação  
NFR6: Epic 8 - Limites coerentes entre nginx, Spring, front e docker-compose  

## Epic List

### Epic 1: Autenticação e Classificação de Papel
Usuários do tenant CPS acessam a plataforma via MSAL e o sistema decide corretamente se cada um é aluno ou professor (regra de domínio no back-end).
**FRs covered:** FR1, FR2, FR3, FR4

### Epic 2: Catálogo e Seleção de Instituições
O admin mantém o catálogo oficial de instituições e o professor seleciona entre 0 e 4 delas (opcional no primeiro login).
**FRs covered:** FR5, FR6, FR7

### Epic 3: Postagem de Projetos
O aluno publica um projeto nascendo como `AGUARDANDO_APROVACAO` informando o e-mail do professor responsável, instituição, atributos completos e a lista de integrantes.
**FRs covered:** FR8, FR9

### Epic 4: Aprovação de Projetos
O professor vê a fila de projetos das suas instituições e aprova/rejeita (com `motivoRejeicao` obrigatório na rejeição).
**FRs covered:** FR10, FR11

### Epic 5: Catálogo Público para Visitantes
Usuários não autenticados pesquisam e visualizam projetos no estado `APROVADO` sem login.
**FRs covered:** FR12

### Epic 6: Perfil — Hub do Usuário Autenticado
Todo usuário autenticado aterrissa no Perfil (`/perfil`, protegido): hub único com layout compartilhado entre ALUNO e PROFESSOR, identidade MSAL somente leitura e abas/atalhos dirigidos por papel; superfícies avulsas ("Meus Projetos", "Fila", "Seleção de Instituições") viram abas.
**FRs covered:** FR13, FR14

### Epic 7: Editor TipTap e documentação em HTML
O aluno cola a documentação `.docx` que já escreveu e publica texto e imagens intercalados; o visitante lê o mesmo conteúdo como HTML sanitizado, sem carregar o editor. O Editor.js é removido.
**FRs covered:** FR8, FR9, FR12 (leitura)
**Architecture:** AD-ED-1, AD-ED-2, AD-ED-3, AD-ED-7, AD-ED-8; §3.2, §4.1–4.3, §5

### Epic 8: Pipeline de mídia, limites e performance do catálogo
Toda imagem tem pior caso conhecido: 5MB de entrada, 1MB de saída, thumb de capa, teto de 18 imagens por projeto, limites coerentes entre nginx/Spring/front, cache imutável e faxina de órfãos. O detalhe do catálogo abre em rede de laboratório.
**FRs covered:** FR8, FR9, FR12 (performance)
**Architecture:** AD-ED-4, AD-ED-5, AD-ED-6, AD-ED-9, AD-ED-10

---

## Epic 1: Autenticação e Classificação de Papel

Usuários do tenant CPS acessam a plataforma via MSAL e o sistema decide corretamente se cada um é aluno ou professor (regra de domínio no back-end).

### Story 1.1: Autenticar via MSAL restrito ao tenant CPS

As a membro do tenant CPS,  
I want entrar na plataforma com meu e-mail institucional via MSAL,  
So that só contas da CPS acessam o sistema.

**Acceptance Criteria:**
- **Given** que não estou autenticado e abro a aplicação
- **When** clico em "Entrar"
- **Then** sou direcionado ao fluxo MSAL da Microsoft do app registrado
- **And** após autenticar como conta do tenant CPS, recebo sessão válida e acesso à aplicação
- **And** um teste com token emitido fora do tenant CPS é rejeitado no back-end (401)

### Story 1.2: Classificar papel por domínio no back-end

As a back-end,  
I want determinar se o usuário autenticado é aluno ou professor pelo UPN/preferred_username,  
So that as telas e regras reflitam o papel certo de cada um.

**Acceptance Criteria:**
- **Given** um usuário do tenant CPS autenticado
- **When** o back-end avalia o UPN
- **Then** UPN terminando em `@aluno.cps.sp.gov.br` é classificado como aluno (checagem antes de professor)
- **And** UPN terminando em `@cps.sp.gov.br` é classificado como professor
- **And** UPN de outro domínio do tenant CPS cai em aluno (fallback)
- **And** testes unitários cobrem os três casos, decididos no servidor

### Story 1.3: Manter sessão e fazer logout

As a usuário autenticado,  
I want manter minha sessão ativa e poder sair quando quiser,  
So that o acesso às minhas ações permanece protegido.

**Acceptance Criteria:**
- **Given** que estou autenticado
- **When** acesso um recurso protegido com token válido
- **Then** o back-end libera a requisição
- **And** ao fazer logout, recursos protegidos passam a retornar 401 sem token novo

### Story 1.4: Aplicar autorização por papel no servidor

As a back-end,  
I want exigir o papel (aluno/professor) em toda operação protegida,  
So that ninguém execute ações de outro papel adulterando o front.

**Acceptance Criteria:**
- **Given** um token de aluno
- **When** o aluno tenta uma operação de professor (ex.: listar fila de aprovação)
- **Then** o back-end responde 403
- **And** a decisão de papel nunca vem do front-end

---

## Epic 2: Catálogo e Seleção de Instituições

O admin mantém o catálogo oficial de instituições e o professor seleciona de 0 a 4 delas.

### Story 2.1: Admin mantém o catálogo de instituições

As a admin do sistema,  
I want incluir, editar e remover instituições do catálogo oficial,  
So that a lista disponível aos professores reflita os dados oficiais das instituições CPS.

**Acceptance Criteria:**
- **Given** que sou admin autenticado no fluxo administrativo (via MSAL)
- **When** administro o catálogo
- **Then** consigo adicionar, editar e remover instituições (CRUD simples no MVP)
- **And** o catálogo oficial é pré-populado no boot a partir dos recursos versionados (seed idempotente, sem importação em runtime)
- **And** sem papel admin, qualquer alteração de catálogo é negada (403)

### Story 2.2: Professor consulta o catálogo oficial (tipo-para-revelar)

As a professor,  
I want digitar e ver o catálogo oficial filtrar pela minha consulta,  
So that eu escolha somente instituições oficiais, sem digitar nome livre e sem percorrer a lista inteira.

**Acceptance Criteria:**
- **Given** que sou professor autenticado na aba **"Instituições (0–4)"** do Perfil (`/perfil`, `institution-autocomplete mode=multi`)
- **When** o campo está vazio (inclusive no foco/clique)
- **Then** **nenhum popup/lista é exibido** — vejo apenas o texto de ajuda "Buscamos no catálogo oficial do Centro Paula Souza (86 unidades em operação)."
- **When** digito uma consulta (ex.: "ipe")
- **Then** o popup filtra o catálogo oficial por `nome`, `cidade`, `codigoUnidade` e `regiaoAdministrativa` (accent-insensitive), ordenado por `nome` e depois `cidade`, com no máximo 20 linhas + "… e mais N unidades. Refine a busca." (contagem anunciada = nº real de correspondências)
- **And** sem correspondência, o popup mostra o empty state "Nada encontrado por nome, cidade, código ou região." + "Limpar busca" (cinza, sem vermelho)
- **And** catálogo indisponível mostra no popup "Catálogo indisponível no momento. [Tentar novamente]", mantendo a consulta
- **And** não existe campo de criação livre de instituição em superfície nenhuma (sem texto livre, sem auto-cadastro)

### Story 2.3: Professor vincula de 0 a 4 instituições na aba do Perfil

As a professor,  
I want associar de 0 a 4 instituições do catálogo na aba **"Instituições (0–4)"** do meu Perfil,  
So that eu gerencie a aprovação apenas das instituições onde atuo, sem sair do meu hub.

**Acceptance Criteria:**
- **Given** que sou professor autenticado no `/perfil`, aba **"Instituições (0–4)"** (título "Vincule as unidades onde você atua", `institution-autocomplete mode=multi` — Frame B de `mockups/institution-autocomplete.html`)
- **When** confirmo unidades (`Enter`/clique)
- **Then** cada confirmação **acrescenta um chip e reabre a consulta**; o contador mostra "X de 4 unidades" por extenso (no teto, "4 de 4 unidades" em `accent-ink`); a remoção de chip é **estado local imediato**, sem chamada ao servidor, até o Save
- **And** com 0 vínculos vejo o onboarding inline "Sem instituições vinculadas ainda — escolha até 4 unidades." + "0 de 4" (mínimo 0 permitido, não bloqueante — FR-6)
- **When** tento vincular a 5ª unidade
- **Then** ela é **recusada inline** com "Máximo de 4 instituições por professor", popup continua aberto, sem modal e sem vermelho; para trocar, removo um chip e adiciono outro — **e** o servidor reforça o teto (`PUT /professores/me/instituicoes` com 5+ ids → 409, nada persistido)
- **When** clico em **"Salvar vínculos"**
- **Then** os vínculos persistem via `PUT /professores/me/instituicoes` (`{ "instituicaoIds": [...] }`, `[]` válido — contrato em `architecture/architecture-Fatec-Repositorios-2026-10-07-vinculo-professor-instituicao/`); sair/trocar de aba com alterações não salvas pede confirmação; falha no Save mostra painel neutro **acima** do conteúdo da aba **mantendo chips e contagem**, com Save reabilitado
- **And** posso alterar a seleção no futuro, mantendo entre 0 e 4 instituições

Nota (correção de curso 2026-09-23, mantida): a superfície de seleção é a aba **"Instituições (0–4)"** do Perfil (FR-13/FR-14), não rota avulsa; as regras de vínculo 0–4 permanecem inalteradas no servidor.

---

## Epic 3: Postagem de Projetos

O aluno publica um projeto nascendo diretamente como `AGUARDANDO_APROVACAO`, informando e-mail do professor responsável, instituição, atributos completos e integrantes.

### Story 3.1: Aluno publica um projeto (diretamente AGUARDANDO_APROVACAO)

As a aluno,  
I want publicar um projeto preenchendo todos os atributos e integrantes,  
So that meu trabalho é submetido diretamente para aprovação.

**Acceptance Criteria:**
- **Given** que sou aluno autenticado
- **When** crio um projeto com título, descrição curta, conteúdo HTML rico (`conteudoHtml`), link do repositório, imagem de capa, palavras-chave, ano de publicação, instituicaoId e integrantes
- **Then** o projeto é salvo diretamente com estado `AGUARDANDO_APROVACAO` (não existe estado rascunho)
- **And** os integrantes são salvos associados ao projeto (Projeto 1 ── N Integrante) com `nome` e `linkLinkedin`
- **And** a capa (`imagemCapaUrl`) é **obrigatória**: publicar sem capa falha no cliente e o servidor responde `400`
- **And** imagens complementares são nós `<img>` intercalados com texto dentro de `conteudoHtml` (não existe `imagensExtras`)
- **And** o servidor sanitiza o HTML e recusa a 19ª imagem distinta (ST-7.1/ST-7.2); a capa é obrigatória e **não entra nessa contagem**
- **And** a instituição (`instituicaoId`) é escolhida **na Seção 2 do `/projeto-forms`** via `institution-autocomplete mode=single`: nada renderiza antes de digitar; a confirmação (`Enter`/clique) fixa o valor e exibe metadado ("Cód. 291 · São Paulo · Região Litoral") + ação **"Alterar"** (cancelar a edição preserva o valor confirmado); sem texto livre
- **And** enviar sem confirmar a instituição **não submete**: o foco volta ao campo com borda de erro neutra + mensagem ligada por `aria-describedby`
- **And** `?instituicaoId=` válido na URL **pré-preenche** o campo já confirmado (deep-link retrocompatível, não produzido por nenhuma UI); id inválido/inativo volta o campo a vazio sem erro

### Story 3.2: Informar e-mail do professor responsável

As a aluno,  
I want informar o e-mail do professor responsável ao postar,  
So that o projeto registre o responsável de forma consistente, inclusive fora do tenant.

**Acceptance Criteria:**
- **Given** que estou criando um projeto como aluno
- **When** informo o e-mail do professor responsável
- **Then** o campo aceita apenas endereço de e-mail válido (validação no envio)
- **And** e-mails não cadastrados (inclusive fora do tenant) são aceitos como "Professor Convidado" (registro informativo, sem notificação no MVP)
- **And** o atributo `emailProfessorResponsavel` é persistido obrigatoriamente
- **And** tentar publicar sem `emailProfessorResponsavel` falha

Nota (correção de curso 2026-09-23, mantida): o CTA "Novo Projeto" e o acompanhamento do estado vivem na aba **"Meus projetos"** do Perfil (FR-13/FR-14); esta story cobre apenas a criação e as regras da postagem, não superfície própria de listagem. **Não há passo prévio de instituição** — a rota `/selecionar-instituicao` foi removida (2026-09-29).

Nota (decisão 2026-10-26, aprovada): o **autocomplete por nome foi removido** do front-end — o campo é somente e-mail (`type=text` + `inputmode=email`). Motivo: o aluno que não sabe o e-mail do professor perde o caminho de busca, mas o valor final já era sempre e-mail e a busca por nome exigia ~10 testes de front + guarda anti-race. O endpoint `GET /professores` permanece no back-end (testado pelo E2E LGPD `ProfessorAutocompleteLgpdE2ETest`), pronto para reintegração se a busca por nome for reavaliada.

---

## Epic 4: Aprovação de Projetos

O professor vê a fila de projetos das suas instituições e aprova/rejeita (com motivo obrigatório).

### Story 4.1: Professor vê a fila ("Pendentes") na aba do Perfil

As a professor,  
I want acessar a fila dos projetos pelos quais sou responsável pela aba **"Pendentes"** do Perfil,  
So that eu acompanhe e julgue o que me foi submetido no meu hub.

**Acceptance Criteria:**
- **Given** que sou professor autenticado e estou no Perfil
- **When** aciono a aba "Pendentes"
- **Then** vejo os projetos em estado `AGUARDANDO_APROVACAO` em que meu e-mail é o `emailProfessorResponsavel` (sem barreira de vínculo), como conteúdo da aba (não como rota paralela)
- **And** só aparecem projetos de instituição ativa (`ativo=true`; inativa some da fila)
- **And** o vínculo 0–4 segue como organização da fila no front (aviso para revisar a instituição quando não vinculado), não como autorização

Decisão 2026-10-10 (sprint-planning + code-review, aprovada pelo usuário): **responsável-somente**.
O vínculo é auto-declarado pelo próprio professor (ele escolhe as 0–4 unidades), logo
exigi-lo como barreira não adiciona segurança — só fricção. A identidade/autorização
real vem do MSAL (`emailProfessorResponsavel` == e-mail da conta). Implementado em
`ProjetoService.listarPendentesParaProfessor` / `verificarPermissaoAvaliacao`; coberto por
`AvaliacaoProjetoE2ETest` (12 testes). A leitura antiga de "interseção" está superada.

Rationale (correção de curso 2026-09-23): a "fila de aprovação" deixa de ser superfície/rota avulsa e vira aba do hub `/perfil` (CAP-8/FR-14).

### Story 4.2: Aprovar ou rejeitar projeto (motivoRejeicao obrigatório na rejeição)

As a professor,  
I want aprovar ou rejeitar um projeto da minha fila,  
So that o aluno saiba o resultado da validação.

**Acceptance Criteria:**
- **Given** que há um projeto na minha fila em `AGUARDANDO_APROVACAO`
- **When** aprovo, o estado muda para `APROVADO`
- **When** rejeito, sou obrigado a informar a justificativa e o estado muda para `REJEITADO` com `motivoRejeicao` preenchido
- **Then** o aluno visualiza o novo estado atualizado

---

## Epic 5: Catálogo Público para Visitantes

Usuários não autenticados pesquisam e visualizam projetos no estado `APROVADO` sem necessidade de login.

### Story 5.1: Pesquisar e listar projetos aprovados na vitrine pública

As a visitante não autenticado,  
I want pesquisar e filtrar projetos aprovados no portal público,  
So that eu conheça os trabalhos desenvolvidos pelos alunos da Fatec/CPS.

**Acceptance Criteria:**
- **Given** que não estou autenticado na aplicação
- **When** acesso a página de catálogo público (a raiz `/` renderiza a Home — o redirect `'' → projeto-forms` deixa de existir)
- **Then** visualizo a lista/cards de projetos com estado `APROVADO` (hero tipográfico + busca + filtros + grade; shell header/footer existentes)
- **And** posso pesquisar por palavras-chave e filtrar por instituição **sem requerer token JWT**, sendo o filtro de instituição o `institution-autocomplete mode=filter` (mesmo campo dos outros usos, Frame C de `mockups/institution-autocomplete.html`): digito e o catálogo público filtra (nome, cidade, código, região); a confirmação vira um `institution-chip` removível; a grade filtra **por `instituicaoId`** (o `<select>` antigo com `value = inst.nome` deixa de existir, e com ele o bug de binding); "Limpar" aparece só com filtro ativo; remover o chip restaura o recorte anterior
- **And** quando **logado**, a Home ganha uma faixa discreta de atalho para o Perfil — mas permanece a voz pública da plataforma (vitrine sem barreira de login)

### Story 5.2: Visualizar detalhes completos de um projeto aprovado

As a visitante não autenticado,  
I want abrir o detalhe de um projeto aprovado,  
So that eu veja todo o conteúdo rico, imagens, integrantes e link do repositório.

**Acceptance Criteria:**
- **Given** que estou navegando no catálogo público
- **When** seleciono um projeto aprovado
- **Then** vejo o título, descrição curta, conteúdo HTML sanitizado (`conteudoHtml`, com títulos, listas, código, links e imagens intercaladas), integrantes, ano, palavras-chave e link do repositório
- **And** o acesso ao endpoint público de detalhes não exige autenticação
- **And** as imagens usam URLs `/uploads/...`, são carregadas com `loading="lazy"` e o render não usa `bypassSecurityTrustHtml` (ST-7.6)

Nota (2026-10-20): o renderizador de blocos do Editor.js foi removido. O detalhe passa a renderizar um único container `[innerHTML]` com o HTML sanitizado no servidor; a especificação de leitura está em ST-7.6 e o contrato em `ARCHITECTURE-EDITOR-TIPTAP-E-MIDIA.md` §3.4.

---

## Epic 6: Perfil — Hub do Usuário Autenticado

Todo usuário autenticado aterrissa no Perfil (`/perfil`, rota protegida): hub único com layout compartilhado entre ALUNO e PROFESSOR, identidade MSAL somente leitura e abas/atalhos dirigidos por papel; superfícies avulsas ("Meus Projetos", "Fila", "Seleção de Instituições") viram abas.

### Story 6.1: Hub de Perfil com identidade MSAL e landing única

As a usuário autenticado,  
I want acessar meu Perfil como hub (bloco de identidade + atalhos + abas por papel) e aterrissar nele após o login,  
So that eu encontre meu contexto e ações do meu papel em um só lugar, sem rotas paralelas.

**Acceptance Criteria:**
- **Given** que autentiquei via MSAL
- **When** o login retorna
- **Then** sou direcionado ao `/perfil` (rota **protegida**; só o próprio usuário vê o próprio perfil)
- **And** a aba/atalho pré-selecionada segue o estado do papel: aluno → "Meus projetos"; professor zerado → destaque "Instituições (0–4)" (onboarding não bloqueante); professor com vínculos → "Pendentes"; admin → atalho Admin Main
- **And** o bloco de identidade exibe avatar, nome (`h2`), e-mail institucional e papel — dados do MSAL **somente leitura** (FR-13)
- **And** ALUNO e PROFESSOR compartilham o mesmo layout de hub (abas/atalhos dirigidos por papel — padrão canal)
- **And** "Meus Projetos"/"Pendentes"/"Instituições" **não existem como rotas paralelas** (rotas: `/`, `/perfil`, `/projeto-forms`, `/projetos/:id`, `/admin`)

---

## Epic 7: Editor TipTap e documentação em HTML

**Goal:** o aluno cola a documentação `.docx` que já escreveu e publica texto e imagens intercalados; o visitante lê o mesmo conteúdo como HTML sanitizado, sem carregar o editor. O Editor.js é removido.

**FRs covered:** FR-8, FR-9, FR-12 (leitura)  
**Architecture:** AD-ED-1, AD-ED-2, AD-ED-3, AD-ED-7, AD-ED-8; §3.2, §4.1–4.3, §5  
**Binds:** CAP-4, FR-8, FR-9, NFR-5  
**Non-goals:** `imagensExtras`/galeria, edição colaborativa em tempo real, conversão de PDF/DOCX em anexo, estado `RASCUNHO` no servidor.

### Story 7.1: Backend — contrato `conteudoHtml` e sanitização na escrita

**ID:** `ST-7.1` · **Priority:** P0 · **Depends on:** —

As a back-end,  
I want aceitar e persistir a documentação como HTML sanitizado,  
So that qualquer consumidor (catálogo, export futuro) leia o mesmo conteúdo sem depender do editor.

**Acceptance Criteria:**
- **Given** um `POST /projetos` com `conteudoHtml` válido
- **When** o projeto é persistido
- **Then** `ProjetoCreateRequest`, `ProjetoResponse`, o modelo e o mapper expõem `conteudoHtml`
- **And** a coluna física continua `conteudo_editor_js` via `@Column(name = "conteudo_editor_js", columnDefinition = "TEXT")`
- **And** nenhum DTO de request/response, modelo ou mapper expõe `conteudoEditorJs`; o seed é migrado em ST-7.8
- **Given** HTML contendo `<script>`, `<iframe>`, `<object>`, `<embed>`, `<style>`, `<form>`, `<input>` ou atributos `on*`
- **When** o servidor sanitiza com jsoup
- **Then** essas tags e atributos são removidos antes da persistência
- **And** o Safelist permite exatamente: `p, h2, h3, h4, strong, em, s, code, pre, blockquote, ul, ol, li, a, img, br, hr, figure, figcaption`
- **And** `a` aceita apenas `href`, `target`, `rel` com protocolos `http`/`https`, e recebe `rel="noopener noreferrer"`
- **And** `img` preserva `src`, `alt`, `title` e recebe `loading="lazy"` e `decoding="async"` na escrita
- **And** `href` com `javascript:` é removido, e a sanitização é idempotente (sanitizar duas vezes produz o mesmo HTML)
- **And** `conteudoHtml` vazio ou nulo é aceito (0 imagens) e não causa erro
- **And** um teste unitário cobre `script`, `onerror`, `javascript:`, tags permitidas e `rel`; um E2E faz `POST` com payload malicioso e `GET` comprova que ele volta inerte
- **And** `mvn test` passa com os testes novos

### Story 7.2: Backend — teto de 18 imagens distintas e allowlist de `src`

**ID:** `ST-7.2` · **Priority:** P0 · **Depends on:** ST-7.1

As a back-end,  
I want contar e validar as imagens do documento antes de persistir,  
So that o armazenamento e o peso da página tenham um pior caso conhecido.

**Acceptance Criteria:**
- **Given** um `conteudoHtml` com 18 `src` distintos e válidos
- **When** o projeto é criado
- **Then** a resposta é `201` e as 18 imagens são persistidas
- **Given** um `conteudoHtml` com 19 `src` distintos
- **When** o projeto é criado
- **Then** a resposta é `400` com o envelope `{ "codigo": 400, "mensagem": "..." }` e mensagem acionável citando o limite de 18
- **And** nenhum projeto é persistido (a contagem anterior não muda)
- **And** ocorrências repetidas do mesmo `src` contam uma única vez (18 `src` distintos podem aparecer 19 vezes)
- **And** 0 imagens é válido e publica normalmente
- **And** cada `src` casa com `^/uploads/[0-9a-fA-F-]+\.(png|jpg|jpeg|webp)$`
- **And** `data:`, `blob:`, URL `http(s)` externa, caminho relativo ou `<img>` sem `src` resultam em `400` — nunca em remoção silenciosa
- **And** a validação ocorre sobre o HTML recebido (ou sobre o DOM sanitizado com `src` preservado), de modo que um `src` inválido não seja apenas descartado
- **And** a capa (`imagemCapaUrl`) não entra na contagem; só os `<img>` de `conteudoHtml` contam
- **And** testes cobrem 18/19, duplicata, `data:`, URL externa, `<img>` sem `src` e ausência de persistência no 400

### Story 7.3: Frontend — editor TipTap com HTML e remoção do Editor.js

**ID:** `ST-7.3` · **Priority:** P0 · **Depends on:** ST-7.1 (contrato), ST-8.1 (upload)

As a aluno,  
I want escrever e colar a documentação em um editor que preserve a formatação,  
So that eu não precise reescrever o `.docx` no formulário.

**Acceptance Criteria:**
- **Given** a Seção 3 do `/projeto-forms`
- **When** o editor inicializa
- **Then** usa TipTap com `@tiptap/core`, `@tiptap/starter-kit`, `@tiptap/extension-image`, `@tiptap/extension-link` e `@tiptap/extension-placeholder`; nenhuma extensão adicional é incluída sem necessidade demonstrada
- **And** a barra oferece títulos `h2`/`h3`/`h4`, negrito, itálico, riscado, código, listas com e sem ordem, citação, link, imagem e desfazer/refazer
- **And** a extensão Heading está configurada com `levels: [2, 3, 4]`; conteúdo colado com `h1`, `h5` ou `h6` é normalizado para `h2`/`h3`/`h4` ou parágrafo antes de salvar
- **And** o editor nunca produz `h1`, `h5` ou `h6` (fora do Safelist)
- **Given** um texto formatado colado do Word ou Google Docs
- **When** o paste termina
- **Then** títulos, parágrafos, listas, negrito e itálico são preservados na medida do schema
- **And** `editor.getHTML()` é a fonte do conteúdo enviado em `conteudoHtml`
- **And** carregar o HTML salvo (`setContent`) e salvar novamente preserva as tags permitidas (round-trip)
- **And** `Image.configure({ allowBase64: false })` está ativo e nenhum `data:` é inserido no documento
- **And** toda imagem inserida tem `alt` editável, com fallback para o nome do arquivo
- **And** links inseridos são normalizados para `http`/`https`
- **And** `@editorjs/*` foi removido de `package.json` e `package-lock.json`, junto com imports, tipos, `editorjs.d.ts` e `styles/editorjs.css`
- **And** `rg '@editorjs|editorjs' front-end/src` retorna zero
- **And** os testes do formulário foram migrados para TipTap e `npm test` + `npm run build` passam

### Story 7.4: Frontend — paste/drop de `.docx` com upload automático de imagens

**ID:** `ST-7.4` · **Priority:** P0 · **Depends on:** ST-7.3, ST-8.1

As a aluno,  
I want colar uma documentação com screenshots e ter as imagens enviadas automaticamente,  
So that o caso de uso principal (copiar o `.docx` e colar) funcione sem 413.

**Acceptance Criteria:**
- **Given** um `.docx` real com 5 screenshots copiado do Word
- **When** eu colo no editor
- **Then** os 5 arquivos são extraídos de `clipboardData.files`/`items` e enviados a `POST /uploads` (no máximo 2 em paralelo)
- **And** cada `src` temporário/base64 é substituído pela URL `/uploads/...` retornada
- **And** `editor.getHTML()` contém 5 `<img src="/uploads/...">` e zero `data:image`
- **And** a ordem do documento é preservada (a imagem permanece na posição em que foi colada)
- **Given** um paste em que o HTML do clipboard traz apenas base64
- **When** não há `File` em `files`/`items`
- **Then** o base64 é convertido em `Blob` e enviado, sem nunca inserir `data:` no documento
- **Given** um drop de arquivos de imagem sobre o editor
- **When** os arquivos são soltos
- **Then** o mesmo pipeline de upload e substituição é aplicado
- **Given** que um upload falha
- **When** o erro retorna
- **Then** a imagem não é inserida, as demais permanecem e uma mensagem acionável oferece tentar novamente
- **Given** que o documento já tem 17 imagens e o paste traria 2
- **When** o limite é ultrapassado
- **Then** apenas a imagem que cabe é inserida e a excedente é recusada com mensagem citando o limite de 18
- **And** uma documentação de 5 páginas com 5 imagens gera payload ≤ 2MB e não recebe `413`
- **And** existe teste automatizado com fixture de clipboard e um roteiro manual com `.docx` real

### Story 7.5: Frontend — compressão, limite de 5MB e contador 0–18

**ID:** `ST-7.5` · **Priority:** P0 · **Depends on:** ST-7.3

As a aluno,  
I want que a capa e as imagens inline sejam comprimidas e contadas antes do envio,  
So that eu veja o limite na hora e o projeto não estoure o orçamento de bytes.

**Acceptance Criteria:**
- **Given** a capa ou uma imagem inline selecionada
- **When** o arquivo é validado
- **Then** só são aceitos `image/png`, `image/jpeg`, `image/jpg` e `image/webp`
- **And** arquivos acima de 5MB são recusados antes do upload com mensagem citando 5MB (não 10MB)
- **And** arquivos de até 5MB são aceitos para compressão
- **Given** uma imagem válida
- **When** a compressão roda
- **Then** usa `createImageBitmap(file, { imageOrientation: 'from-image' })` (com fallback para `Image`/canvas quando indisponível) e canvas
- **And** o lado maior é reduzido para no máximo 1600px, sem upscale de imagens menores
- **And** a saída é JPEG com qualidade 0.82, preservando a proporção
- **And** a orientação EXIF é aplicada (foto de celular em pé não sai girada)
- **And** a capa é enviada com `finalidade=capa` e as imagens inline com `finalidade=inline` (default)
- **And** se a compressão falhar e o original tiver ≤ 5MB, o original é enviado; o servidor ainda normaliza (ST-8.1)
- **And** o preview da capa usa o resultado comprimido, não o base64 do original
- **Given** o documento em edição
- **When** imagens são inseridas, removidas ou coladas
- **Then** o contador mostra `X de 18`, contando `src` distintos já enviados em `/uploads/...`
- **And** imagens ainda em upload não entram na contagem até retornarem URL
- **And** a 19ª imagem é bloqueada com mensagem citando o limite, sem inserção parcial
- **And** com 0 a 4 imagens o formulário mostra o aviso suave "recomendado 5 ou mais", mas **permite publicar**
- **And** a capa é **obrigatória**: sem capa o formulário não permite publicar e o servidor recusa o projeto com `400`
- **And** a capa **não é contada** no `X de 18`; apenas os `<img>` de `conteudoHtml` entram no contador
- **And** testes cobrem 5MB/5,1MB, orientação, downscale 1600px, imagem menor sem upscale, contador 18/19, duplicata e aviso 0–4 sem bloqueio

### Story 7.6: Frontend — render do detalhe como HTML sanitizado

**ID:** `ST-7.6` · **Priority:** P0 · **Depends on:** ST-7.1, ST-7.8

As a visitante não autenticado,  
I want abrir o detalhe de um projeto e ver o conteúdo rico renderizado,  
So that eu leia a documentação completa com texto e imagens intercalados.

**Acceptance Criteria:**
- **Given** um projeto com `conteudoHtml`
- **When** abro `/ver-projeto/:id`
- **Then** o conteúdo é renderizado em um único container com `[innerHTML]`, sem parser de blocos
- **And** `BlocoConteudo`, `blocos()`, `itensLista()`, `urlImagemBloco()` e o renderizador de blocos do Editor.js foram removidos
- **And** títulos `h2`/`h3`/`h4`, parágrafos, listas, código, citação, links e imagens aparecem com a tipografia existente
- **And** links mantêm `rel="noopener noreferrer"` e imagens mantêm `loading="lazy"`
- **And** em desenvolvimento, `src="/uploads/..."` é reescrito para `environment.apiUrl` antes do bind; em produção o caminho relativo funciona via nginx
- **And** `bypassSecurityTrustHtml` não é usado em lugar nenhum (`rg` retorna zero); o sanitizador do Angular é a segunda linha de defesa
- **Given** um `conteudoHtml` com `<script>` ou `onerror`
- **When** a página renderiza
- **Then** nada executa e o texto inerte não quebra a tela
- **Given** `conteudoHtml` vazio, nulo ou malformado
- **When** a página renderiza
- **Then** aparece "Este projeto ainda não possui conteúdo detalhado." e nenhum erro não tratado é lançado
- **And** os testes de `ver-projeto` são reescritos para HTML (substituindo o caso de `conteudoEditorJs` inválido)

### Story 7.7: Frontend — autosave local em IndexedDB

**ID:** `ST-7.7` · **Priority:** P1 · **Depends on:** ST-7.3

As a aluno,  
I want continuar de onde parei se fechar ou recarregar o formulário,  
So that eu não perca o preenchimento das 4 seções.

**Acceptance Criteria:**
- **Given** que estou preenchendo o `/projeto-forms`
- **When** o editor ou qualquer campo das 4 seções muda
- **Then** um autosave com debounce de ~1s grava em IndexedDB o HTML do TipTap, os campos das seções e a `versao` do schema
- **And** a chave é `rascunho-projeto:<email>` do usuário autenticado
- **And** nenhuma chamada ao back-end é feita e nenhum projeto é criado nesse momento (NFR-5 permanece intacto)
- **Given** que existe um rascunho com `versao` conhecida
- **When** abro o formulário
- **Then** vejo a opção "Continuar rascunho" ou "Começar novo"
- **And** "Continuar" restaura as 4 seções e chama `editor.commands.setContent(html)`
- **And** "Começar novo" descarta o rascunho e abre o formulário limpo
- **Given** um rascunho com `versao` desconhecida
- **When** abro o formulário
- **Then** ele é descartado e nunca restaurado
- **And** outro usuário no mesmo computador (chave diferente) não vê o rascunho anterior
- **And** o rascunho é limpo ao publicar com sucesso, ao descartar explicitamente e no logout do usuário
- **Given** IndexedDB indisponível ou cota excedida
- **When** o autosave falha
- **Then** o formulário continua funcional, o erro é registrado no console e a publicação não é bloqueada
- **And** testes cobrem restauração, versão desconhecida, limpeza no publish/logout e isolamento por usuário

### Story 7.8: Seed em HTML e remoção do caminho legado de blocos

**ID:** `ST-7.8` · **Priority:** P0 · **Depends on:** ST-7.1

As a equipe de desenvolvimento,  
I want converter o seed para HTML e remover a leitura de blocos,  
So that o banco resetado já nasça no formato novo, sem caminho duplo.

**Acceptance Criteria:**
- **Given** `back-end/src/main/resources/seeds/projetos.json`
- **When** o seed carrega
- **Then** o único projeto (Golden Maker) usa a chave `conteudoHtml` e não possui `conteudoEditorJs`
- **And** os tipos presentes no seed foram convertidos: `header` → `h2`/`h3`, `paragraph` → `p`, `list` → `ul`/`ol`, `code` → `pre`/`code`, `quote` → `blockquote`, `image` → `figure`/`img`, `delimiter` → `hr`
- **And** o projeto semeado continua `APROVADO`, com capa, integrantes e instituição
- **And** o seed permanece idempotente (`upsert` não duplica; `replace` recarrega)
- **And** o carregamento em banco zerado não falha e o detalhe público renderiza o HTML
- **And** não existe caminho de leitura duplo (blocos + HTML): o parser/renderizador de blocos foi removido
- **And** `rg 'conteudoEditorJs'` no código de produção e testes retorna zero, exceto a anotação legada `@Column(name = "conteudo_editor_js")`
- **And** testes de seed e de render passam

### Story 7.9: Frontend — rota lazy e Editor.js fora do bundle inicial

**ID:** `ST-7.9` · **Priority:** P1 · **Depends on:** ST-7.3

As a visitante do catálogo público,  
I want que o editor não seja carregado ao abrir a vitrine,  
So that a página mais visitada continue leve, inclusive em rede de laboratório.

**Acceptance Criteria:**
- **Given** `app.routes.ts`
- **When** a aplicação compila
- **Then** `projeto-forms` é carregado via `loadComponent` (import dinâmico), e não no bundle inicial
- **And** a rota `/` (catálogo público), `/ver-projeto/:id` e `/perfil` não importam TipTap/ProseMirror
- **And** o build de produção coloca TipTap/ProseMirror apenas no chunk do formulário; não há chunk de `@editorjs`
- **And** a primeira navegação para `/projeto-forms` carrega o chunk sob demanda e o editor inicializa normalmente
- **And** não existe estratégia de preload que baixe o chunk do editor na rota pública
- **And** a verificação é automatizada (inspeção de chunks/source map ou bundle analyzer) e registrada no PR
- **And** `npm run build` passa

---

## Epic 8: Pipeline de mídia, limites e performance do catálogo

**Goal:** toda imagem tem pior caso conhecido (5MB de entrada, 1MB de saída, thumb de capa); a capa é obrigatória e não entra na contagem de 18; os limites de nginx, Spring, front e docker não divergem; o detalhe do catálogo abre em rede de laboratório; uploads abandonados não enchem o disco. Com o teto de 1MB, 500 projetos deixam de ocupar ~95GB e passam a ~3GB.

**FRs covered:** FR-8, FR-9, FR-12 (performance)  
**Architecture:** AD-ED-4, AD-ED-5, AD-ED-6, AD-ED-9, AD-ED-10; §1.1, §2, §6  
**Binds:** CAP-4, FR-8, FR-9, NFR-6  
**Non-goals:** conversão de formato além de WebP→JPEG, CDN externa, versionamento de imagem, redimensionamento de avatar (já resolvido).

### Story 8.1: Backend — normalização de imagem e thumbnail da capa

**ID:** `ST-8.1` · **Priority:** P0 · **Depends on:** —

As a back-end,  
I want decodificar, validar e re-encodar toda imagem recebida,  
So that o pior caso de armazenamento e de peso de página seja uma garantia, não um esforço do cliente.

**Decisão de implementação (aditiva):** `POST /uploads` aceita o campo opcional `finalidade` (`capa` | `inline`, default `inline`). O multipart continua com `file` e a resposta continua com `url` e `nomeArquivo`; a capa também gera `<uuid>-thumb.jpg`, derivável de `url` por convenção. Se o arquiteto preferir contrato estritamente inalterado, a alternativa é gerar thumb para todo upload; a escolha deve ser registrada antes de iniciar a story.

**Acceptance Criteria:**
- **Given** um `POST /uploads` com `file` válido e `finalidade=capa` ou `inline`
- **When** o upload termina
- **Then** a resposta mantém `url` e `nomeArquivo`; para `capa`, existe também `<uuid>-thumb.jpg` no diretório de uploads
- **And** o formulário usa a thumb no card do catálogo e a capa completa no detalhe; se a thumb não existir (ex.: seed antigo), o card cai na capa
- **And** a capa é **obrigatória**: o formulário sempre envia `finalidade=capa` e o servidor recusa `POST /projetos` sem `imagemCapaUrl` com `400`
- **Given** uma imagem de entrada
- **When** é validada
- **Then** tipos aceitos são PNG, JPEG/JPG e WebP, com magic-bytes (comportamento atual preservado)
- **And** entrada acima de 5MB é recusada com `413` e mensagem acionável citando 5MB
- **And** o decode é obrigatório; arquivo que não decodifica retorna `400`
- **And** dimensões absurdas (lado maior > 12.000px ou total > 40 megapixels) são recusadas com `400` antes de alocar o buffer completo (proteção contra decompression bomb)
- **And** WebP é decodificado via `com.twelvemonkeys.imageio:imageio-webp`
- **And** a saída é JPEG: capa em qualidade 0.82, inline em 0.80, lado maior ≤ 1600px, sem upscale
- **And** metadados EXIF são descartados; a orientação é aplicada no cliente (ST-7.5) e a imagem é re-encodada como decodificada
- **And** a thumb da capa tem lado maior ≤ 480px
- **And** se o arquivo resultante passar de 1MB, o upload é recusado com `413` e mensagem acionável
- **And** em qualquer rejeição (tipo, 5MB, decode, 1MB), nenhum arquivo parcial permanece no diretório de uploads
- **And** o nome final é UUID + `.jpg`; o nome original nunca é usado (anti path traversal)
- **And** uploads concorrentes não colidem (UUID) e arquivos existentes não são sobrescritos
- **And** testes cobrem PNG/JPEG/WebP, 5MB/5,1MB, arquivo corrompido, dimensões absurdas, 1600px, thumb 480px, EXIF descartado, saída > 1MB e nomes únicos

### Story 8.2: Infra + frontend — tetos de transporte coerentes e mensagem de 413

**ID:** `ST-8.2` · **Priority:** P0 · **Depends on:** ST-8.1 (limites de imagem)

As a equipe de operação,  
I want que nginx, Spring e frontend anunciem os mesmos limites,  
So that o aluno nunca receba uma mensagem contraditória ou um 413 confuso.

**Acceptance Criteria:**
- **Given** `front-end/nginx.conf`
- **When** a configuração é aplicada
- **Then** `client_max_body_size` é `15m` e o JSON do `413` não cita mais 60MB
- **Given** `back-end/src/main/resources/application.yml`
- **When** o back-end sobe
- **Then** `spring.servlet.multipart.max-file-size` e `max-request-size` são `12MB`
- **Given** o formulário
- **When** valida os arquivos e o payload
- **Then** a imagem é limitada a 5MB e o payload de `POST /projetos` a 2MB (medido em bytes UTF-8)
- **And** nenhum `data:` é aceito no payload; a mensagem orienta remover e reenviar por upload
- **Given** uma imagem de 5MB
- **When** sobe pelo nginx até o Spring
- **Then** é aceita (nginx 15m, Spring 12MB, aplicação 5MB)
- **Given** uma imagem de 5,1MB
- **When** sobe
- **Then** é recusada pela aplicação com mensagem de 5MB (não pelo nginx)
- **Given** um payload de projeto de 2,1MB
- **When** tento publicar
- **Then** o front bloqueia antes da rede com mensagem acionável; 1,9MB passa pela guarda
- **And** existe teste de paridade automatizado (script ou teste de build) que falha se `nginx.conf`, `application.yml` e as constantes do front divergirem da tabela AD-ED-5
- **And** comentários defasados (60MB, 10MB, `client_max_body_size 12m`) foram removidos
- **And** `docker compose config` e `nginx -t` passam

### Story 8.3: Infra — cache imutável para `/uploads/`

**ID:** `ST-8.3` · **Priority:** P1 · **Depends on:** ST-8.1

As a visitante do catálogo,  
I want que as imagens já baixadas não sejam baixadas de novo,  
So that a segunda visita ao detalhe não gaste 5–6MB de rede.

**Acceptance Criteria:**
- **Given** uma resposta de `/uploads/<uuid>.jpg` ou `<uuid>-thumb.jpg`
- **When** o navegador recebe
- **Then** o header inclui `Cache-Control: public, max-age=31536000, immutable`
- **And** o header é aplicado no `location ^~ /uploads/` do nginx
- **And** `index.html` continua com `no-store, no-cache, must-revalidate` e a API não recebe cache imutável
- **And** uma segunda requisição à mesma URL é servida do cache do navegador (sem revalidação)
- **And** uploads repetidos geram nomes UUID diferentes, comprovando que a URL nunca muda de conteúdo
- **And** `curl -I` e `nginx -t` verificam o comportamento

### Story 8.4: Backend — faxineiro de arquivos órfãos com carência de 7 dias

**ID:** `ST-8.4` · **Priority:** P2 · **Depends on:** ST-8.1, ST-8.3

As a equipe de operação,  
I want remover uploads que nenhum projeto referencia,  
So that o volume de imagens não cresça com arquivos abandonados.

**Acceptance Criteria:**
- **Given** o diretório de uploads e o banco
- **When** o job diário roda
- **Then** monta o conjunto de referências com todas as capas (`imagemCapaUrl`) e todos os `<img src>` de `conteudoHtml` de todos os projetos, em qualquer estado
- **And** também considera referenciada a thumb de uma capa referenciada
- **And** remove apenas arquivo **não referenciado** e com `mtime` superior a 7 dias (168h)
- **And** arquivos de 6 dias são mantidos; arquivos de 8 dias não referenciados são removidos
- **And** capa e imagens do seed são mantidas
- **And** se o banco falhar, o job não apaga nada (fail-closed) e registra o erro
- **And** o job é idempotente, loga contagem de verificados/removidos e lida com diretório ausente sem quebrar
- **And** é controlado por propriedade (`app.upload.cleanup.enabled`, default `false` até a validação em produção)
- **And** testes com diretório temporário e relógio fixo cobrem 6d/8d, referenciado, thumb, capa de seed, banco indisponível e execução repetida

Nota: rascunhos locais (ST-7.7) podem ter imagens já enviadas e ainda não publicadas; a carência de 7 dias é o que protege esse caso, conforme AD-ED-9.

### Story 8.5: Backend + frontend — `GET /uploads/policy` (opcional)

**ID:** `ST-8.5` · **Priority:** P2 · **Depends on:** ST-8.2

As a frontend,  
I want consultar os limites vigentes no servidor,  
So that não existam quatro cópias de constantes divergindo.

**Acceptance Criteria:**
- **Given** uma requisição pública a `GET /uploads/policy`
- **When** o back-end responde
- **Then** retorna JSON com `imagemEntradaMb: 5`, `imagemSaidaMb: 1`, `maxImagensInline: 18`, `thumbCapaPx: 480`, `imagemInlineMaxPx: 1600`, `payloadProjetoMb: 2` e os tipos aceitos
- **And** o formulário consome a policy na inicialização e usa fallback local se a chamada falhar
- **And** os valores retornados coincidem com a aplicação real; um teste falha se a policy divergir das constantes do servidor
- **And** o endpoint não exige autenticação
- **And** se a story não for implementada, o teste de paridade da ST-8.2 é obrigatório e a decisão fica registrada

### Story 8.6: Rollout e regressão da migração

**ID:** `ST-8.6` · **Priority:** P0 · **Depends on:** ST-7.1–7.9, ST-8.1–8.3

As a equipe de desenvolvimento,  
I want validar a migração de ponta a ponta antes de liberar,  
So that a troca do editor não regrida o catálogo público nem o fluxo de aprovação.

**Acceptance Criteria:**
- **Given** um banco zerado
- **When** `docker compose up --build` sobe a pilha
- **Then** o seed carrega 86 instituições e 1 projeto com `conteudoHtml`, sem erro
- **And** back-end e front-end compilam; as suítes de teste existentes e as novas passam
- **And** o roteiro manual cobre: colar `.docx` real com 5 imagens → publicar → aprovar → ver no catálogo público
- **And** cobre 18 imagens (aceito) e 19 (recusado); 5MB (aceito) e 6MB (recusado)
- **And** cobre publicação sem capa (bloqueada no cliente e recusada com `400` no servidor), confirmando que a capa é obrigatória e não entra na contagem de 18
- **And** cobre payload XSS (`<script>`, `onerror`) sem execução no detalhe
- **And** cobre restauração de rascunho após reload, limpeza no publish e isolamento por usuário no mesmo PC
- **And** no detalhe com 18 imagens, o HTML inicial é ≤ 2MB, as imagens são lazy e a segunda visita usa o cache de `/uploads/`
- **And** o pior caso do detalhe (capa 1MB + 18 imagens de 1MB) fica em ≤ 19MB, contra os ~190MB do cenário anterior de 10MB cru por imagem
- **And** o bundle inicial do catálogo não contém TipTap/ProseMirror nem `@editorjs`
- **And** o checklist do §7 da arquitetura está 100% marcado
- **And** a documentação foi sincronizada: PRD §2/§3/§4.5, `DESIGN.md`, `spec-exclusao-projetos/SPEC.md` e `SPEC.md` não falam mais em `conteudoEditorJs`/Editor.js como contrato vigente
- **And** `rg 'conteudoEditorJs|@editorjs'` no código de produção/testes retorna zero (exceto a coluna física legada)
- **And** a release note registra: reset de banco obrigatório, novos limites (5MB/1MB/18), remoção do Editor.js e rollback por restauração de backup (não há leitura dupla)

---

## Sequenciamento recomendado (Épicos 7–8)

O banco **pode e será resetado**; não há script de migração de dados nem caminho de leitura duplo (`ARCHITECTURE` §5). O rollout deve ser coordenado com essa janela.

| Onda | Stories | Por quê |
|---|---|---|
| 0 — Pipeline | ST-8.1, ST-8.2, ST-8.3 | Fecha o pior caso de bytes e destrava o upload usado pelo editor |
| 1 — Contrato | ST-7.1, ST-7.2 | Backend passa a aceitar/sanitizar/limitar `conteudoHtml` |
| 2 — Editor | ST-7.3, ST-7.5 | TipTap, compressão e contador; podem ser desenvolvidos contra o contrato da onda 1 |
| 3 — Integração | ST-7.4, ST-7.6, ST-7.8 | Paste de `.docx`, render do detalhe e seed HTML |
| 4 — Performance/estado | ST-7.9, ST-7.7 | Rota lazy e autosave local |
| 5 — Gate | ST-8.6 | Regressão, DoD e release note |
| 6 — Pós-migração | ST-8.4, ST-8.5 | Órfãos e policy; não bloqueiam a publicação |

**Paralelização segura:** ST-8.1/8.2/8.3 entre si; ST-7.1 e ST-7.2 em sequência; ST-7.6 e ST-7.8 podem começar com fixture HTML antes do seed final. ST-7.4 depende de ST-7.3 + ST-8.1; ST-7.5 depende de ST-7.3; ST-7.9 depende de ST-7.3.

---

## Matriz de rastreabilidade — Épicos 7–8

| Story | AD-ED / seção | FR/NFR | Evidência de aceite principal |
|---|---|---|---|
| ST-7.1 | AD-ED-2, §3.1–3.3 | FR-8, FR-9 | Teste de sanitização (script/onerror/javascript:) + E2E POST→GET |
| ST-7.2 | AD-ED-3, §3.1 | FR-8 | Teste 18/19, duplicata, `src` externo/`data:` |
| ST-7.3 | AD-ED-1, AD-ED-2, §4.2 | FR-8 | Teste do editor TipTap + `rg` sem `@editorjs` |
| ST-7.4 | AD-ED-1, AD-ED-4, §4.2 | FR-8 | Teste de paste com fixture + QA com `.docx` real |
| ST-7.5 | AD-ED-4, AD-ED-5 | FR-8 | Teste 5MB/5,1MB, orientação, contador 18/19 |
| ST-7.6 | AD-ED-2, §3.4 | FR-12 | Teste de render HTML + XSS inerte |
| ST-7.7 | AD-ED-8, §4.3 | NFR-5 | Teste de restore/limpeza/isolamento no IndexedDB |
| ST-7.8 | §5 | FR-8, FR-12 | Teste de seed + render do projeto semeado |
| ST-7.9 | AD-ED-7 | NFR-6 | Inspeção de chunks do build de produção |
| ST-8.1 | AD-ED-4 | FR-8, FR-9 | Testes de decode/re-encode/thumb/limite 1MB |
| ST-8.2 | AD-ED-5 | NFR-6 | Teste de paridade de limites + `docker compose config` |
| ST-8.3 | AD-ED-6 | FR-12 | `curl -I` com `Cache-Control` imutável |
| ST-8.4 | AD-ED-9 | Operação | Teste de 6d/8d, referenciado, thumb, fail-closed |
| ST-8.5 | AD-ED-10 | Operação | Teste de contrato da policy + fallback |
| ST-8.6 | §7 (DoD) | FR-8/9/12, NFR-5/6 | Suíte completa + roteiro manual + release note |

---

## Riscos específicos e mitigação

| Risco | Story que mitiga | Mitigação verificável |
|---|---|---|
| Paste de Word insere base64 e estoura o payload | ST-7.4, ST-7.2, ST-8.2 | `allowBase64:false`, conversão para Blob + upload, rejeição de `data:` no servidor, guarda de 2MB |
| HTML com `<script>`/`on*` vira XSS armazenado | ST-7.1, ST-7.6 | jsoup na escrita + sanitizador do Angular na leitura; teste com payload malicioso |
| `src` externo/tracking pixel | ST-7.2 | Regex restrita a `/uploads/<uuid>.<ext>`; 400 em URL externa |
| Imagem rotacionada por EXIF | ST-7.5, ST-8.1 | `imageOrientation:'from-image'` + re-encode sem EXIF |
| WebP não decodifica e vira 400 indevido | ST-8.1 | TwelveMonkeys `imageio-webp` + teste de WebP |
| Rascunho local vaza em PC compartilhado | ST-7.7 | Chave por e-mail + limpeza no logout + teste de isolamento |
| Faxineiro apaga imagem de rascunho | ST-8.4 | Carência de 7 dias + fail-closed |
| Limites divergentes voltam | ST-8.2, ST-8.5 | Teste de paridade obrigatório; policy opcional |
| Catálogo público regride em performance | ST-7.9, ST-8.3 | Rota lazy, thumb, `loading="lazy"`, cache imutável, orçamento no ST-8.6 |

---

## Decisões e questões abertas

1. **Contrato do upload de capa (default proposto):** campo opcional `finalidade=capa|inline` e thumb determinística `<uuid>-thumb.jpg`, sem novo campo no modelo. Validar com o arquiteto antes de ST-8.1; a alternativa é gerar thumb para todo upload.
2. **Capa e contagem de 18 (confirmado 2026-10-20):** a capa é **obrigatória** e **não conta** no limite de 18; o limite conta apenas `<img>` distintos de `conteudoHtml`.
3. **`alt` obrigatório (default proposto):** toda imagem inserida tem `alt` editável com fallback para o nome do arquivo; não bloqueia publicação.
4. **Policy de uploads:** `GET /uploads/policy` fica para depois da migração; até lá, o teste de paridade de ST-8.2 é obrigatório.
5. **Faxineiro de órfãos:** entra como P2, desabilitado por padrão até validação em produção.
6. **Sincronização documental:** PRD, DESIGN e `spec-exclusao-projetos/SPEC.md` ainda citam Editor.js/`conteudoEditorJs`; a atualização é entregável de ST-8.6, não deste artefato.
7. **Exclusão de projeto:** o spec de exclusão nulifica conteúdo e capa; após isso, o faxineiro só remove os arquivos depois da carência de 7 dias. Nenhuma mudança de contrato é necessária.

---

## Definition of Done — Épicos 7–8

- [ ] TipTap configurado com StarterKit + Image + Link + Placeholder; `allowBase64: false`.
- [ ] Paste/drop de imagem sobe para `/uploads` e insere URL; nenhum `data:` no documento.
- [ ] `@editorjs/*` removido de `package.json` e do código; `projeto-forms` em `loadComponent`.
- [ ] Backend sanitiza `conteudoHtml` com jsoup e aplica o Safelist de §3.2.
- [ ] Backend rejeita `> 18` imagens distintas e `src` fora de `/uploads/`.
- [ ] Capa **obrigatória**: 5MB de entrada, 1600px/q0.82 na saída, thumb 480px gerado; fora da contagem de 18.
- [ ] Imagens inline: 5MB de entrada, 1600px/q0.80, `loading="lazy"`.
- [ ] Spring `12MB` + nginx `15m` + JSON do 413 atualizado; guarda do front em 2MB.
- [ ] `Cache-Control: immutable` em `/uploads/`.
- [ ] Autosave em IndexedDB com versão, restauração e limpeza; nenhum `RASCUNHO` no servidor.
- [ ] Seed convertido para HTML (1 projeto); renderizador de blocos removido.
- [ ] Testes: sanitização (script/onerror), limite 18/19, 5MB/6MB, paste com imagem, restauração de rascunho, paridade de limites.
- [ ] Roteiro de regressão de ST-8.6 executado e release note publicada.
