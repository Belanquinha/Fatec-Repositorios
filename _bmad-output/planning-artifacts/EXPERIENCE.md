---
name: Fatec-Repositorios
status: final
sources:
  - ../specs/spec-fatec-repositorios/SPEC.md
  - ../planning-artifacts/prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - ../planning-artifacts/epics.md
  - ../specs/DESIGN-STORY-3.1-UPLOAD-PROJETO.md
updated: 2026-09-23
---

# Fatec-Repositorios — Experience Spine

> Alvo desta rodada: a **Tela Inicial (Home)** e o novo **hub de Perfil pós-login**. O shell (header/footer) e a página de postagem já existem. Tokens visuais são cruzados por nome a partir de `DESIGN.md` (ex.: `{colors.primary}`, `{components.profile-tab}`).

## Foundation

Web responsiva (Angular 21 + Tailwind + Material Symbols; fonte de marca **Inter**), shell fixo: `app-header` sticky (4.5rem) + `app-footer` (© LuePad • Projetos FATEC).

Duas superfícies top-level, com públicos distintos:

- **Home (`/`) — portal público.** Roda hoje na raiz (`app.routes.ts` redireciona `''` → `projeto-forms`; [assumption] a raiz passa a renderizar a Home). Vitrine pública de projetos `APROVADO` (CAP-7, FR-12): visitantes buscam, filtram e abrem detalhes sem login. Para quem está logado, ganha uma faixa discreta de atalho para o Perfil — mas continua sendo a voz pública da plataforma.
- **Perfil (`/perfil`) — hub do usuário autenticado.** Uma única rota, um único layout, com **atalhos e abas dirigidos por papel** — ALUNO e PROFESSOR compartilham a mesma estrutura e trocam apenas o conteúdo (padrão de referência do usuário: *página de canal do YouTube* — bloco de identidade, atalhos, e o conteúdo vinculado ao usuário). É privado: só o próprio usuário vê o próprio perfil. O rosto público de um trabalho é o próprio projeto na vitrine, não a página de quem o publicou.

Landing pós-login: **todo papel autenticado cai no Perfil** e o primeiro bloco/aba é escolhido pelo estado do papel (aluno → "Meus projetos"; professor com 0 vínculos → destaque/oferta de "Instituições"; professor com vínculos → "Pendentes"). ⚠️ Isto **delta** sobre o SPEC/PRD (FR-14 destina landings distintas por papel) — confirmado, vira proposta de correção de curso.

## Information Architecture

| Surface | Rota | Reached from | Purpose |
|---|---|---|---|
| Home (vitrine pública) | `/` | App open · logo · menu "Inicio" | Hero + busca/filtros + grade de projetos `APROVADO` + faixa para o Perfil quando logado |
| Detalhe do projeto (público) | `/projetos/:id` | Card da vitrine | Conteúdo completo do projeto `APROVADO`, sem login (CAP-7, Story 5.2) |
| Postagem de projeto | `/projeto-forms` | Menu "Upload" · atalho do Perfil (aluno) | Formulário em seções (existe hoje, Story 3.1) |
| **Perfil — hub (ALUNO)** | `/perfil` | Login · Home · avatar no header | Identidade + atalhos ("Novo Projeto", "Meus projetos") + listas por estado |
| **Perfil — hub (PROFESSOR)** | `/perfil` | Login · Home · avatar no header | Identidade + atalhos ("Pendentes", "Aprovados", "Instituições") + listas vinculadas |
| Detalhe de projeto (dono/professor) | `/projetos/:id` (autenticado) | Lista do Perfil | Mesmo detalhe, com badge de estado e (professor) ações de aprovação |
| Admin (catálogo) | `/admin` | Atalho do Perfil (papel ADMIN) | CRUD simples de instituições (FR-7) |

Dentro do `Perfil`, **abas em vez de rotas separadas** — "Meus Projetos", "Fila de Aprovação" e "Seleção de Instituições" deixam de ser superfícies avulsas e viram abas/atalhos do hub:

- **Aba ALUNO "Meus projetos"** — busca/lista dos próprios projetos agrupada por estado: `Aguardando aprovação`, `Publicados (APROVADO)`, `Rejeitados` (exibindo `motivoRejeicao`). CTA primário "Novo Projeto" sempre visível no perfil.
- **Aba PROFESSOR "Pendentes"** — a fila de aprovação das suas instituições (`AGUARDANDO_APROVACAO`); projetos onde o professor é o `emailProfessorResponsavel` ficam destacados (CAP-5/FR-10 — qualquer professor vinculado pode julgar).
- **Aba PROFESSOR "Aprovados"** — histórico do que já foi julgado `APROVADO` nas suas instituições.
- **Aba PROFESSOR "Instituições (0–4)"** — seleção do catálogo com o teto de 4 (CAP-3/AD-4) e onboarding quando zerada.

## Voice and Tone

Português do Brasil, registro institucional mas simples — uma universidade pública se apresentando, não uma SaaS em modo hype. Microcopy curto, verbo na frente.

| Do | Don't |
|---|---|
| "Publique seu trabalho e faça parte do acervo acadêmico da Fatec" | "🚀 Domine o futuro com sua inovação!" |
| "Projetos aprovados" | "Trabalhos validados com sucesso" |
| "Buscar por título, tag ou instituição" | "Pesquisar um grande número de trabalhos acadêmicos aprovados por docentes vinculados" |
| "Nenhum projeto encontrado. Tente outro termo." | "Vazio. Nenhum resultado." |
| "Seu projeto foi enviado para avaliação." | "Submissão realizada com êxito ✓" |
| Perfil: "Bem-vindo(a) de volta, {nome}" · papel em badge ("Aluno Fatec" / "Professor CPS") | Saudação com emojis; duplicar informações óbvias do header |
| Estado do projeto em uma palavra: "Aguardando aprovação" / "Aprovado" / "Rejeitado" | Jargão de API (`AGUARDANDO_APROVACAO`) no texto visível |

## Component Patterns

Comportamento. Visual em `DESIGN.md.Components`.

| Component | Use | Behavioral rules |
|---|---|---|
| Barra de busca | Home, topo da vitrine | Input com lupa; busca por título/descrição curta/palavras-chave; `debounce` ~250ms; `Enter` dispara imediato; `Esc` limpa e devolve foco. Resultados atualizam a grade em `aria-live` unobtrusivo. |
| Filtros | Home, sob a busca | Select de instituição (`GET /instituicoes` público) + chips de palavras-chave. Chips combináveis; chip ativo desselecionável; botão "Limpar" só aparece com filtro ativo. Filtro em visitante não exige token. |
| Project card | Home · Perfil | Área do card (capa acima) clicável → detalhe. Hover eleva (`{elevation.raised}`). Metadados: instituição, ano, até 3 tags. Na Home (visitante) todos são `APROVADO`; no Perfil, o card exibe badge de estado real do dono. |
| quick-action-card | Home logada · Perfil | 1 clique para a superfície/ação do papel. No Perfil, prioriza a ação de maior valor: aluno → "Novo Projeto"; professor sem vínculos → "Selecionar instituições"; professor com vínculos → "Pendentes". |
| profile-header | Perfil | Identidade do hub (padrão canal): avatar (`full`), nome `h2`, e-mail institucional e papel — dados MSAL **somente leitura** (FR-13). Com cartão de onboarding se apropriado (professor zerado). |
| profile-tab | Perfil | Abas dirigidas por papel, mesma geometria para ALUNO e PROFESSOR. Aba ativa sublinhada em `{colors.primary}`; inativa `{colors.text-muted}`. Navegável por teclado (setas) e ancorada por `role="tabpanel"`. |
| Lista por estado (status-group) | Perfil | Projetos do dono agrupados por estado com cabeçalho de contagem ("Aguardando aprovação · 2"). Rejeitado mostra `motivoRejeicao` inline como citação discreta. |
| Painel de aprovação | Perfil → dp projeto (professor) | Aprovar: `button-primary` confirma. Rejeitar: abre dialog com `textarea` obrigatório de motivo (`motivoRejeicao`) — sem redirecionar para outra tela; erro do servidor exibido no dialog, sem vermelho CPS. |
| Empty state | Home · Perfil | Ícone + uma frase de orientação + ação única ("Publicar meu primeiro projeto" / "Adicionar instituições" / "Limpar filtros"). |

## State Patterns

| State | Surface | Treatment |
|---|---|---|
| Cold load (catálogo) | Home | Skeleton cards (4–6) na mesma grade esperada; sem "carregando…" texto. |
| Pós-login (role detected) | Perfil | Perfil carrega com header + abas do papel; a aba de landing é pré-selecionada pela regra de papel/estado (ver Foundation); seção "Novo Projeto" com foco para aluno. |
| Professor sem vínculos | Perfil | Aba "Instituições" em destaque + onboarding card "Sem instituições vinculadas ainda — escolha até 4 unidades." Não bloqueia as demais abas. |
| Aluno sem projetos | Perfil | Empty state: "Seu primeiro projeto ainda não foi publicado." CTA primário "Novo Projeto". |
| Sem resultados | Home | Empty state com "Limpar filtros" (filtros ativos) ou "Ver todos os projetos". |
| Erro da API | Home · Perfil | Painel de erro em bloco: "Não foi possível carregar. [Tentar novamente]" — retry reaproveita filtros/aba atuais. Sem vermelho CPS. |
| Visitante na vitrine | Home | Nenhuma barreira de login: buscar/filtrar/abrir detalhes funciona sem autenticação. Apenas o Perfil exige sessão (rota protegida). |
| Conta rejeitada pós-MSAL | Home | Fluxo MSAL redireciona para o erro do tenant; Home retorna ao estado de visitante sem mensagem fantasma. |
| Offline | Qualquer | Aviso único discreto no topo; conteúdo em cache permanece. |

## Interaction Primitives

- `Enter` na busca dispara; `Esc` limpa e move o foco para o input; `/` foca a busca (desktop).
- Perfil: abas por teclado — `Tab` entra, `←/→` troca de aba, `Home/End` vai à primeira/última (ARIA tabs pattern).
- `Tab` segue ordem de leitura: header → identidade → atalhos → abas → lista.
- Foco sempre visível: anel `{colors.primary}` (2px) em controles, abas e cards focados por teclado.
- Clique/hover: card de projeto eleva e sublinha o título; toque (mobile) sem depender de hover (área inteira clicável).
- Banned: infinite scroll (paginação ou "carregar mais" discreto), drag-to-reorder, modais em pilha, conteúdo com hover-only em telas pequenas.

## Accessibility Floor

Comportamental. Contraste visual vive em `DESIGN.md` (paleta verificada AA: amarelo `#DCA703` × texto `#202124`, verde `#00B32A` × `#004B12`, cinza sobre `branco-50` — todos ≥ 4.5:1 nos usos previstos).

- WCAG 2.2 AA na superfície web.
- Busca e filtros com `<label>` explícito; contagem de resultados anunciada via `aria-live="polite"` ("— projetos encontrados").
- Estado e papel nunca só por cor: badges têm texto; aba ativa tem texto + sublinhado; papel do usuário é texto ("Aluno Fatec" / "Professor CPS").
- Abas do Perfil implementadas como ABAs reais (`role="tablist"`/`tab`/`tabpanel`, `aria-selected`), não como conjunto de links fingindo abas.
- Animações ≤ 250ms; `prefers-reduced-motion` reduz a transições de opacidade; sem parallax.
- Área de toque mínima 40×40px em atalhos e chips de filtro (mobile).
- Cards clicáveis com link real (não `<div onclick>`), com `aria-label` quando a capa não tem texto alternativo útil.

## Responsive & Platform

| Breakpoint | Home | Perfil |
|---|---|---|
| `≥ lg` (1024px+) | Hero em linha; vitrine em 3 colunas; busca largura total | Header de perfil em linha (avatar + identidade + atalhos à direita); abas + 2 colunas de lista |
| `md` (768–1023px) | Hero empilhado; vitrine em 2 colunas | Header empilhado; atalhos viram card em fileira rolável |
| `< md` (sm) | Vitrine em 1 coluna; busca 100%; atalhos 2×2 | Identidade centrada; atalhos 2×2; abas com scroll horizontal; listas em 1 coluna |

Web responsiva, não app nativo. Postagem e aprovação mantêm o padrão desktop da plataforma; leitura/busca são confortáveis em celular.

## Inspiration & Anti-patterns

- **Lifted from** o próprio shell existente (header sticky + menu lateral, footer) e a linguagem do formulário de postagem (seções, cantos retos, badge "Convidado"). Home, Perfil e formulário falam a mesma língua.
- **Lifted from (decisão do usuário)** a **página de canal do YouTube** para o Perfil: bloco de identidade no topo, abas de conteúdo, e a sensação de "este é o meu espaço na plataforma". Usamos o *padrão estrutural*, não a estética editorial do YouTube.
- **Lifted from** portais acadêmicos institucionais para a Home: vitrine em grade com capa + descrição curta + instituição, busca e filtro explícitos.
- **Rejected — Hero "startup"**: gradiente, foto gigante, claims vazios. O hero da Home é tipográfico e institucional.
- **Rejected — Login antes de ver qualquer coisa**: a vitrine é pública (CAP-7); nenhum wall de login na Home.
- **Rejected — Perfil público / redes sociais**: perfil é privado e instrumental; sem seguir, curtir, comentar ou bio editável no MVP.
- **Rejected — Cards com ações ocultas no hover (só ícone)**: mobile quebraria; ações do card são só navegação.
- **Rejected — Vermelho em "rejeitado"**: vermelho pertence à marca CPS; rejeitado é cinza, neutro e honesto, com o motivo sempre à vista.

## Key Flows

### Flow 1 — Visitação pública (Beatriz, comunidade acadêmica, noite de sexta)

1. Beatriz abre o portal pelo navegador; a Home carrega com skeleton e resolve para a vitrine.
2. Ela lê o hero ("Trabalhos que fazemos em sala, abertos a todos"), rola para a grade.
3. Digita "inteligência artificial" na busca; a grade reduz com contagem anunciada.
4. Abre o filtro de instituição e escolhe "Fatec Ipiranga"; refina com um chip de tag.
5. **Climax:** um card chama atenção — capa, título, descrição curta e o selo "Aprovado". Ela clica e vê o projeto completo (conteúdo, integrantes, repositório) **sem login — e sem nenhum botão de cadastro gritando**.
6. Failure: a API falha ao carregar → painel "Não foi possível carregar os projetos. [Tentar novamente]"; ela retenta e os filtros permanecem.

### Flow 2 — De visitante a publicadora (Mariana, aluna da Fatec)

1. Mariana acessa a Home deslogada e topa com o CTA do header "Login institucional".
2. Autentica via MSAL (tenant CPS); o retorno a leva ao **Perfil** — cabeçalho com avatar, nome, "Aluna Fatec" e a aba **"Meus projetos"** pré-selecionada.
3. O perfil dela está vazio. O empty state a convida: "Seu primeiro projeto ainda não foi publicado." e o atalho **"Novo Projeto"** fica em destaque amarelo.
4. Ela clica e cai no formulário em seções (`/projeto-forms`), que já conhece.
5. Depois de publicar, volta ao Perfil: a aba "Meus projetos" agora mostra "Aguardando aprovação · 1" no grupo amarelo, e o atalho de contagem fecha o loop.
6. **Climax:** quando o professor aprova, o projeto migra para o grupo verde "Publicados" — e ela o encontra na vitrine pública da Home, que é a mesma plataforma de onde ela começou, agora com o trabalho dela somando ao acervo.
7. Failure: o projeto é rejeitado → ele cai no grupo cinza "Rejeitados" com o motivo inline; Mariana corrige e envia de novo pelo botão do grupo. Nenhum passo a força a decorar onde fica "o status".

### Flow 3 — Professor entre fila e histórico (Roberto, professor adjunto)

1. Roberto loga pelo CTA da Home com e-mail `@cps.sp.gov.br`; o back-end classifica como professor.
2. **Primeiro acesso (0 vínculos):** o Perfil abre com onboarding destacado — aba/cartão **"Instituições"** ("Sem instituições vinculadas ainda — escolha até 4 unidades.").
3. Ele vincula 2 unidades do catálogo. A aba **"Pendentes"** surge como primária nos próximos acessos, e a Home (faixa logada) oferece o atalho direto.
4. A fila de Pendentes lista os `AGUARDANDO_APROVACAO` das suas unidades; projetos em que ele é o `emailProfessorResponsavel` aparecem destacados.
5. **Climax:** no detalhe de um projeto, ele clica **Rejeitar** e o dialog exige o motivo — ele escreve a justificativa, confirma, e o projeto sai de Pendentes. Volta ao Perfil e o grupo "Aprovados"/histórico reflete o novo estado; o aluno (Mariana) vê o mesmo estado no lado dela.
6. **Clímax do teto:** ele tenta vincular a 5ª unidade — o sistema recusa com "Máximo de 4 instituições por professor" (AD-4). O perfil mantém o contador correto e o onboarding some; nada quebra.
7. Failure: salva 0 vínculos no 1º acesso → sem bloqueio (FR-6); o Perfil apenas re-apresenta o convite de "Instituições" até ele vincular. A vitrine pública continua acessível para ele entender o alcance do que aprova.