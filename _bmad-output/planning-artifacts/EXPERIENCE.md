---
name: Fatec-Repositorios
status: final
sources:
  # `.memlog.md` é append-only e acumulado entre rodadas: as entradas 14+ são a rodada
  # de 2026-09-29 (o combobox de instituição). As anteriores são de 2026-09-23.
  - ./.memlog.md
  - ../specs/spec-fatec-repositorios/SPEC.md
  - ./prds/prd-Fatec-Repositorios-2026-09-16/prd.md
  - ./epics.md
  - ../specs/DESIGN-STORY-3.1-UPLOAD-PROJETO.md
updated: 2026-09-29
---

# Fatec-Repositorios — Experience Spine

> Esta revisão (2026-09-29) adiciona o `institution-autocomplete` compartilhado por três usos (campo na Seção 2 do formulário, aba "Instituições (0–4)" do Perfil, filtro da Home) e **retira a página de seleção de instituição do produto**. O alvo da rodada anterior (Home + hub de Perfil) permanece como está onde não aparece aqui. Tokens visuais são cruzados por nome a partir de `DESIGN.md` (ex.: `{colors.primary}`, `{components.institution-autocomplete}`); em conflito, os dois spines vencem.

## Foundation

Web responsiva (Angular 21 + Tailwind + Material Symbols; fonte de marca **Inter**), shell fixo: `app-header` sticky (4.5rem) + `app-footer` (© LuePad • Projetos FATEC).

Duas superfícies de entrada, com públicos distintos:

- **Home (`/`) — portal público.** Roda hoje na raiz (`app.routes.ts` redireciona `''` → `projeto-forms`; [assumption] a raiz passa a renderizar a Home). Vitrine pública de projetos `APROVADO` (CAP-7, FR-12): visitantes buscam, filtram e abrem detalhes sem login. Para quem está logado, ganha uma faixa discreta de atalho para o Perfil — mas continua sendo a voz pública da plataforma.
- **Perfil (`/perfil`) — hub do usuário autenticado.** Uma única rota, um único layout, com **atalhos e abas dirigidos por papel** — ALUNO e PROFESSOR compartilham a mesma estrutura e trocam apenas o conteúdo (padrão de referência do usuário: *página de canal do YouTube* — bloco de identidade, atalhos, e o conteúdo vinculado ao usuário). É privado: só o próprio usuário vê o próprio perfil. O rosto público de um trabalho é o próprio projeto na vitrine, não a página de quem o publicou.

Landing pós-login: **todo papel autenticado cai no Perfil** e o primeiro bloco/aba é escolhido pelo estado do papel (aluno → "Meus projetos"; professor com 0 vínculos → destaque/oferta de "Instituições"; professor com vínculos → "Pendentes"). Este delta sobre o SPEC/PRD (FR-14 destinava landings distintas por papel) foi **aprovado e consolidado** na correção de curso de 2026-09-23 — SPEC CAP-8, PRD §4.8/4.9/FR-13/FR-14, epics Epic 6 + story 6.1. **Sem delta em vigor.**

A escolha de instituição é **um componente só em três lugares**. `institution-autocomplete` atende a **Seção 2** do `/projeto-forms` (`mode=single` — 1 unidade, o aluno), a aba **"Instituições (0–4)"** do Perfil do professor (`mode=multi` — 0–4 vínculos, CAP-3/FR-6) e o filtro da Home (`mode=filter` — público, sem token, CAP-7). O que diverge entre os modos (contador, chips, Save, teto) mora no template de cada consumidor, nunca dentro do componente. **Não há rota de seleção de instituição**: os três usos são campo de formulário, aba do hub e filtro público.

O catálogo de instituições é uma **lista oficial e fechada de 86 unidades em operação**, mantida pelo admin (CAP-3, AD-5). Catálogo fechado é exatamente o motivo do **tipo-para-revelar**: nada renderiza antes da digitação e **texto livre nunca é aceito** — nem no formulário do aluno, nem na aba do Perfil, nem no filtro público. Não existe, em superfície nenhuma, cadastro de instituição em texto livre.

**Fora do escopo desta rodada:** a área `/admin` (CRUD do catálogo, FR-7, UJ-3) aparece na tabela de IA porque é uma superfície real, mas **não tem contrato de UX aqui** — nem KEY FLOW, nem anatomia de tabela, nem estado de erro de validação, nem 403. A Story 2.1 está `in-progress`; especificá-la é uma rodada própria, não um apêndice desta. Ver `Open Items`.

## Contract Delta (resolvido)

**Não há conflito de contrato em vigor.** Registrado aqui porque a rodada passou por ele e porque os artefatos de requisitos ainda precisam de emenda.

A rodada de 2026-09-29 propôs inicialmente **manter `/selecionar-instituicao` como rota do professor**, o que contradizia a CAP-8. O usuário reavaliou e **reverteu**: a página é removida por inteiro e o 0–4 vive só na aba "Instituições (0–4)" do Perfil. Os dois spines voltaram a concordar com a CAP-8 como aprovada em 2026-09-23 — **nada a emendar no SPEC quanto a este ponto**.

O que ainda está errado é o lado dos **requisitos**, e é `bmad-correct-course` pendente. Um dev que pegar qualquer destas stories hoje constrói contra um AC que esta rodada proíbe:

| Documento | O que ainda diz | Correção necessária |
|---|---|---|
| `epics.md` · **Story 2.2** | AC: *"**When** abro a seleção de instituições **Then** vejo a lista do catálogo administrado"* | **Invalidado.** Tipo-para-revelar: a lista de 86 unidades não existe em superfície nenhuma. Trocar por "digito e o catálogo filtra". |
| `epics.md` · **Story 2.3** | Superfície = "Seleção de Instituições" | Apontar para a **aba "Instituições (0–4)"** do Perfil, com contador e Save. |
| `epics.md` · **Story 3.1** | AC ancorado em passo prévio de instituição | Reancorar na **Seção 2** do formulário. |
| `epics.md` · **Story 5.1** | Filtro de instituição = `<select>` | Converter para o `institution-autocomplete` `mode=filter`; o bug de binding `inst.nome` × `instituicaoId` morre com ele. |
| `epics.md` · **Story 6.1** | AC: *"não existem como rotas paralelas (rotas: `/`, `/perfil`, `/projeto-forms`, `/projetos/:id`, `/admin`)"* | **Já correto** — confirma que a reversão é a decisão certa. Nenhuma ação. |

Fonte: `.memlog.md`, entradas "CONFLITO DE CONTRATO SURFACEADO" e "REVISAO DE DECISAO" (2026-09-29).

## Open Items

Gaps conhecidos que este spine **não** fecha, registrados para que ninguém os descubra tarde. Nenhum bloqueia a construção do combobox; o primeiro é o único que bloqueia trabalho em andamento.

| Gap | Superfície | Por que está aberto |
|---|---|---|
| **UJ-3 / área `/admin` sem contrato de UX** | `/admin` | A Story 2.1 está `in-progress` e a superfície não tem flow, tabela, estado de validação (`codigoUnidade` único + not-null) nem 403. Precisa de uma rodada própria. |
| **Diálogo de aprovação/rejeição sem comportamento de foco** | Perfil → detalhe | O visual existe agora (`{components.dialog}` em DESIGN.md), mas faltam a regra de contenção de foco, o `Esc` que descarta `motivoRejeicao` em rascunho, e o comportamento de erro do servidor no dialog. É a superfície de FR-11/AD-7 e o clímax do Flow 3. |
| **Dropdown de e-mail do professor não migrou para o padrão de referência** | Seção 2 do form | `{components.institution-autocomplete}` é o padrão; o typeahead de e-mail (`upload-projeto.html`, `upload-projeto.ts`) continua handmade. A Story 3.2 é o dono natural. |
| **`aria-live` duplicado na Home** | Home | Contagem do catálogo e contagem da grade ainda disputam a mesma região `aria-live`. Separar em duas regiões ao implementar o combobox na Home. |

## Information Architecture

→ **Referência de composição:** `mockups/institution-autocomplete.html` — o campo nos três modos e o quadro `<md` em que o popup não cobre o rodapé sticky. Os dois spines vencem em conflito; o mock é referência de composição, não uma terceira fonte.

| Surface | Rota | Reached from | Purpose |
|---|---|---|---|
| Home (vitrine pública) | `/` | App open · logo · menu "Inicio" | Hero + busca/filtros + grade de projetos `APROVADO` + faixa para o Perfil quando logado. O filtro de instituição é o `institution-autocomplete` compartilhado, `mode=filter` (mesmo campo dos outros dois usos, sem token) |
| Detalhe do projeto (público) | `/projetos/:id` | Card da vitrine | Conteúdo completo do projeto `APROVADO`, sem login (CAP-7, Story 5.2) |
| Postagem de projeto | `/projeto-forms` | Menu "Upload" · atalho do Perfil (aluno) · CTA "Publicar Projeto" do hero da Home | Formulário em seções (existe hoje, Story 3.1). A **Seção 2** carrega o `institution-autocomplete` `mode=single` inline; **não há passo prévio** de instituição — o aluno escolhe a unidade aqui dentro |
| **Perfil — hub (ALUNO)** | `/perfil` | Login · Home · avatar no header | Identidade + atalhos ("Novo Projeto", "Meus projetos") + listas por estado |
| **Perfil — hub (PROFESSOR)** | `/perfil` | Login · Home · avatar no header | Identidade + atalhos ("Pendentes", "Aprovados", "Instituições (0–4)") + listas vinculadas. A aba "Instituições (0–4)" é onde o professor vincula suas unidades — ver abaixo |
| Detalhe de projeto (dono/professor) | `/projetos/:id` (autenticado) | Lista do Perfil | Mesmo detalhe, com badge de estado e (professor) ações de aprovação |
| Admin (catálogo) | `/admin` | Atalho do Perfil (papel ADMIN) | CRUD simples de instituições (FR-7) |

Dentro do `Perfil`, **abas em vez de rotas separadas** — "Meus Projetos", "Fila de Aprovação" e "Seleção de Instituições" deixam de ser superfícies avulsas e viram abas/atalhos do hub. **A seleção de instituição não tem rota**: é uma aba, como as outras duas. As rotas autenticadas do produto são `/perfil`, `/projeto-forms`, `/projetos/:id` e `/admin`.

- **Aba ALUNO "Meus projetos"** — busca/lista dos próprios projetos agrupada por estado: `Aguardando aprovação`, `Publicados (APROVADO)`, `Rejeitados` (exibindo `motivoRejeicao`). CTA primário "Novo Projeto" sempre visível no perfil.
- **Aba PROFESSOR "Pendentes"** — a fila de aprovação das suas instituições (`AGUARDANDO_APROVACAO`); projetos onde o professor é o `emailProfessorResponsavel` ficam destacados (CAP-5/FR-10 — qualquer professor vinculado pode julgar).
- **Aba PROFESSOR "Aprovados"** — histórico do que já foi julgado `APROVADO` nas suas instituições.
- **Aba PROFESSOR "Instituições (0–4)"** — **a superfície completa**, sem redirecionar para lugar nenhum: o `institution-autocomplete` `mode=multi` (nada renderizado antes da digitação), a fileira de chips das unidades vinculadas, o contador "2 de 4 unidades", o teto de 4 com recusa inline, e o CTA primário "Salvar vínculos". Quando zerada, mostra o onboarding "Sem instituições vinculadas ainda — escolha até 4 unidades" **no lugar do campo**, sem bloquear as demais abas (CAP-3: mínimo 0, FR-6: onboarding não-bloqueante).

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
| "Buscamos no catálogo oficial do Centro Paula Souza (86 unidades em operação)." | "Escolha uma instituição" |
| "Sua unidade não está no catálogo oficial." | "Instituição inválida" — e jamais enquadramento de erro/alerta, muito menos vermelho |
| "Máximo de 4 instituições por professor" | "Limite excedido — erro" |
| "2 de 4 unidades" | "2/4" solto, sem rótulo |
| "Vincule as unidades onde você atua" | "Seleção de Instituições" (o título mecânico da página antiga) |

A mensagem de unidade fora do catálogo é **neutra e honesta**: diz que o catálogo é oficial, nomeia os campos buscados e oferece um canal de contato genérico. Ela **não** recria um papel *gestor/secretaria* no produto — o SPEC remove esse papel (Non-goals; CAP-3: catálogo é dado mantido por admin, o professor não cadastra instituição). A dica de contato é canal genérico de suporte, não um papel novo. Mesmo espírito do badge "Professor Convidado" no campo de e-mail do professor: o sistema diz a verdade sobre o que ele sabe.

## Component Patterns

Comportamento. Visual em `DESIGN.md.Components`.

| Component | Use | Behavioral rules |
|---|---|---|
| institution-autocomplete | Seção 2 do `/projeto-forms` (`single`, CAP-4) · aba "Instituições (0–4)" do Perfil do professor (`multi`, CAP-3) · filtro da Home (`filter`, CAP-7) | **Um componente, três modos** — o mesmo campo, o mesmo `{elevation.raised}` de popup (`{components.combo-popup}`) e as mesmas linhas de opção (`{components.combo-option}`) nos três lugares; só o template do consumidor muda. `single` = 1 unidade, grava `instituicaoId`; `multi` = 0–4 vínculos, chips + contador + Save; `filter` = 1 filtro, **sem token e sem login** (CAP-7). O popup só abre com **consulta não vazia** — nada renderiza antes de digitar. O casamento é sobre `nome`, `cidade`, `codigoUnidade` e `regiaoAdministrativa`, com normalização accent-insensitive (o `normalizarTexto` de `selecionar-instituicao.ts` é preservado); região deixa de ser chip e vira critério de consulta. `Enter`/clique confirmam; confirmar fixa o valor e fecha o popup. Em `single`, o estado confirmado mostra uma linha de metadado ("Cód. 291 · São Paulo · Região Litoral") e a ação **"Alterar"**, que reabre a consulta. Em `multi`, cada confirmação **acrescenta um chip e reabre a consulta** para a próxima. |
| institution-chip | Aba "Instituições (0–4)" do Perfil (`multi`) · filtro da Home (`filter`) · valor confirmado do aluno | Unidade vinculada/selecionada. A remoção é estado local imediato — **não é chamada ao servidor** — até o "Salvar vínculos". Em `filter`, o chip é o filtro ativo, removível. Foco individual e rótulo no botão de remoção. |
| selection-counter | Aba "Instituições (0–4)" do Perfil (`multi`) | "2 de 4 unidades". Atualiza a cada adição/remoção e fica visível ao lado da fileira de chips. No teto vira `{colors.accent-ink}`; a 5ª escolha é **recusada inline** com "Máximo de 4 instituições por professor" (AD-4), sem modal de erro. Para trocar uma unidade, o professor remove e adiciona. |
| Barra de busca | Home, topo da vitrine | Input com lupa; busca por título/descrição curta/palavras-chave; `debounce` ~250ms; `Enter` dispara imediato; `Esc` limpa e devolve foco. Resultados atualizam a grade em `aria-live` unobtrusivo. |
| Filtros | Home, sob a busca | Filtro de instituição = o mesmo `institution-autocomplete` em `mode=filter` (`{components.filter-select}`) sobre `GET /instituicoes` (público) — sem `<select>` nativo de 86 `<option>`, sem chips de região; a escolha vira um `institution-chip` removível. Mais os chips de palavras-chave, combináveis. Botão "Limpar" só aparece com filtro ativo. **Visitante não exige token** e o filtro agora casa de fato por `instituicaoId` (o `<select>` antigo ligava o value da option a `inst.nome` — ver Story 5.1). |
| Não encontrado no catálogo | Formulário do aluno (`single`) · aba "Instituições (0–4)" do Perfil | Beco sem saída **honesto**: sem texto livre, sem auto-cadastro, sem vermelho — a mensagem vive em `{colors.text-muted}`. Diz "Sua unidade não está no catálogo oficial", nomeia os campos buscados e dá a dica de contato genérica. Focável pelo teclado, ligado ao campo por `aria-describedby`. |
| Salvar vínculos (professor) | Aba "Instituições (0–4)" do Perfil | CTA primária explícita "Salvar vínculos" — hoje nada persiste. Sair do Perfil com alterações não salvas pede confirmação. Remover o último chip é permitido (o mínimo de CAP-3 é 0) e o contador cai para "0 de 4". ⚠️ **O endpoint de persistência professor↔instituição não existe** — não há controller ProfessorInstituicao no back-end: comportamento especificado, não implementado (pendente Story 2.3). |
| Project card | Home · Perfil | Área do card (capa acima) clicável → detalhe. Hover eleva (`{elevation.raised}`). Metadados: instituição, ano, até 3 tags. Na Home (visitante) todos são `APROVADO`; no Perfil, o card exibe badge de estado real do dono. |
| quick-action-card | Home logada · Perfil | 1 clique para a superfície/ação do papel. No Perfil, prioriza a ação de maior valor: aluno → "Novo Projeto"; professor sem vínculos → "Instituições (0–4)" (a própria aba, recarregando o hub); professor com vínculos → "Pendentes". |
| profile-header | Perfil | Identidade do hub (padrão canal): avatar (`full`), nome `h2`, e-mail institucional e papel — dados MSAL **somente leitura** (FR-13). Com cartão de onboarding se apropriado (professor zerado). |
| profile-tab | Perfil | Abas dirigidas por papel, mesma geometria para ALUNO e PROFESSOR. Aba ativa sublinhada em `{colors.accent-ink}` (`{components.profile-tab.active-underline}`); inativa `{colors.text-muted}`. Navegável por teclado (setas) e ancorada por `role="tabpanel"`. |
| Lista por estado (status-group) | Perfil | Projetos do dono agrupados por estado com cabeçalho de contagem ("Aguardando aprovação · 2"). Rejeitado mostra `motivoRejeicao` inline como citação discreta. |
| Painel de aprovação | Perfil → dp projeto (professor) | Aprovar: `button-primary` confirma. Rejeitar: abre dialog com `textarea` obrigatório de motivo (`motivoRejeicao`) — sem redirecionar para outra tela; erro do servidor exibido no dialog, sem vermelho CPS. |
| Empty state | Home · Perfil | Ícone + uma frase de orientação + ação única ("Publicar meu primeiro projeto" / "Adicionar instituições" / "Limpar filtros"). |

## State Patterns

| State | Surface | Treatment |
|---|---|---|
| Catálogo carregando | Combobox (qualquer modo) | Skeleton rows **dentro** do popup (`{elevation.raised}`), nunca spinner bloqueando o formulário; o campo continua digitável. |
| Catálogo indisponível | Combobox (qualquer modo) | A consulta não pode casar contra nada. Mensagem neutra no popup ("Catálogo indisponível no momento. [Tentar novamente]") com `{components.button-secondary}`; **nunca** lista vazia sem explicação e nunca erro em vermelho. O campo mantém a consulta digitada. |
| Consulta vazia | Combobox | **Nenhum popup.** O campo é um input vazio com texto de ajuda ("Buscamos no catálogo oficial do Centro Paula Souza (86 unidades em operação)"). Foco, clique e `Alt+Down` também **não** abrem: sem consulta, não há o que mostrar. |
| Sem correspondência | Combobox | Empty state **dentro** do popup, nomeando os campos buscados: "Nada encontrado por nome, cidade, código ou região." Cinza, com "Limpar busca". |
| Conjunto de resultados | Combobox (86 linhas) | Ordenado por `nome`, depois por `cidade`. Exibe no máximo **20** linhas e, havendo mais, acrescenta a linha "… e mais N unidades. Refine a busca." A opção ativa por `aria-activedescendant` entra em rolagem no viewport a cada `↑`/`↓` — sem isso a navegação por teclado corre para fora da tela. |
| Unidade fora do catálogo | Formulário do aluno (`single`) · aba "Instituições (0–4)" do Perfil | Beco sem saída honesto: mensagem cinza + campos buscados + contato. Sem texto livre, sem auto-cadastro. **Acontece só depois que o usuário escolheu um valor e tentou enviá-lo** — nunca no meio da digitação. |
| Valor confirmado | Seção 2 (`single`) | Metadado da unidade + "Alterar"; clicar reabre a consulta. |
| Pré-preenchido por deep-link | Seção 2 (`single`) | `?instituicaoId=` válido preenche o campo **já confirmado** — mostra a linha de metadado e "Alterar", sem exigir nova confirmação, e satisfaz `instituicaoId` no envio. Id inválido ou inativo: campo volta a vazio com o texto de ajuda, sem erro. |
| "Alterar" cancelado | Seção 2 (`single`) | O valor confirmado anterior **sobrevive**. A consulta digitada durante a edição é descartada ao fechar sem `Enter`/`Tab`; a unidade confirmada continua no formulário. Cancelar edição nunca deixa o campo vazio. |
| `Enter` sem consulta | Seção 2 (`single`) | Com o popup fechado e o campo vazio, `Enter` **não** submete: aciona a validação do formulário, que bloqueia e devolve o foco ao campo obrigatório. Sem isso o aluno tomaria "Enter" por submissão e receberia um erro que não pediu. |
| Teto atingido | Aba "Instituições (0–4)" do Perfil | Contador em `accent-ink` no "4 de 4 unidades" + 5ª escolha recusada inline com "Máximo de 4 instituições por professor". Sem modal, sem vermelho, popup continua aberto. |
| Alterações não salvas | Aba "Instituições (0–4)" do Perfil | "Salvar vínculos" habilitado + indicador de não salvo; sair do Perfil ou trocar de aba pede confirmação. |
| Erro ao salvar vínculos | Aba "Instituições (0–4)" do Perfil | Falha do `Save`: mensagem neutra (`{colors.error-ink}`) em `{colors.error-surface}` **acima** do rodapé do conteúdo da aba, **mantendo os chips e a contagem** e reabilitando o botão. Perder a seleção do professor seria pior que falhar o save; nada some sozinho. |
| Publicação bloqueada (campo obrigatório) | `/projeto-forms` | Instituição ou e-mail de professor em branco no envio: nada é submetido, o foco vai ao primeiro campo inválido, ele recebe `2px {components.institution-autocomplete.error-border}` e a mensagem ligada por `aria-describedby`. Sem vermelho, sem alert, sem `alert()`. Esta é a trava de UJ-1. |
| Erro ao publicar | `/projeto-forms` | Falha do `POST /projetos`: o formulário **permanece inteiro** — nada limpa, nada reseta, o aluno não perde capa nem conteúdo do Editor.js. Painel de erro neutro no topo, com "Tentar novamente". O sucesso emite o atalho de retorno ao Perfil. |
| Cold load (catálogo) | Home | Skeleton cards (4–6) na mesma grade esperada; sem "carregando…" texto. |
| Pós-login (role detected) | Perfil | Perfil carrega com header + abas do papel; a aba de landing é pré-selecionada pela regra de papel/estado (ver Foundation); seção "Novo Projeto" com foco para aluno. |
| Professor sem vínculos | Perfil | Aba "Instituições (0–4)" em destaque; **dentro da aba**, o onboarding "Sem instituições vinculadas ainda — escolha até 4 unidades" ocupa o lugar do campo, com contador em "0 de 4". Não bloqueia as demais abas (FR-6). |
| Aluno sem projetos | Perfil | Empty state: "Seu primeiro projeto ainda não foi publicado." CTA primário "Novo Projeto". |
| Sem resultados | Home | Empty state com "Limpar filtros" (filtros ativos) ou "Ver todos os projetos". |
| Erro da API | Home · Perfil | Painel de erro em bloco: "Não foi possível carregar. [Tentar novamente]" — retry reaproveita filtros/aba atuais. Sem vermelho CPS. (A postagem tem linhas próprias acima, porque a perda de dados em jogo é diferente.) |
| Visitante na vitrine | Home | Nenhuma barreira de login: buscar/filtrar/abrir detalhes funciona sem autenticação. Apenas o Perfil exige sessão (rota protegida). |
| Conta rejeitada pós-MSAL | Home | Fluxo MSAL redireciona para o erro do tenant; Home retorna ao estado de visitante sem mensagem fantasma. |
| Offline | Qualquer | Aviso único discreto no topo; conteúdo em cache permanece. |

`?instituicaoId=` na URL **pre-preenche** o combobox em `mode=single` (deep-link retrocompatível com links antigos; leitura preservada em `upload-projeto.ts`) e não é mais **produzido** por nenhuma UI — ele só existia porque a página removida navegava para o formulário com `queryParams`.

## Interaction Primitives

- `Enter` na busca dispara; `Esc` limpa e move o foco para o input; `/` foca a busca (desktop).
- Perfil: abas por teclado — `Tab` entra, `←/→` troca de aba, `Home/End` vai à primeira/última (ARIA tabs pattern).
- `Tab` segue ordem de leitura: header → identidade → atalhos → abas → lista.
- Foco sempre visível: anel `{colors.accent-ink}` (2px) em controles, abas e cards focados por teclado.
- Clique/hover: card de projeto eleva e sublinha o título; toque (mobile) sem depender de hover (área inteira clicável).
- Banned: infinite scroll (paginação ou "carregar mais" discreto), drag-to-reorder, modais em pilha, conteúdo com hover-only em telas pequenas.

**Combobox — mapa de teclado completo (ARIA 1.2).**

| Tecla | Comportamento |
|---|---|
| Foco / clique no campo | Abre **apenas se houver consulta**; com o campo vazio nada abre |
| `Down` / `Up` | Abre (se fechado) e move a opção ativa; `Up` no topo fecha |
| `Home` / `End` | Primeira / última opção |
| Caractere imprimível | Edita a consulta e re-filtra |
| `Enter` | Confirma a opção ativa e fecha o popup |
| `Esc` | Fecha o popup **mantendo** a consulta; um segundo `Esc` limpa a consulta |
| `Tab` | Confirma e **sai** — nunca descarta silenciosamente uma opção realçada |
| `Alt+Down` | Abre sem alterar a consulta |

O foco **nunca sai do input**: a opção ativa é rastreada por `aria-activedescendant`, não por foco real. O fechamento por clique fora **não** usa corrida de `setTimeout` — listener no documento com verificação de contenção, resolvido no mesmo ciclo do clique.

Este é o **padrão de referência do produto** para campos de catálogo. O dropdown de e-mail do professor (`upload-projeto.html`, `upload-projeto.ts`) é uma **deficiência conhecida, não um precedente**: sem teclado, sem `aria-expanded`/`aria-activedescendant`, confirma em `mousedown` e fecha por `setTimeout(200ms)` no blur. Ele deve ser refatorado para este padrão — não copiado.

Dialog: `Tab` circula **contido** entre os controles, `Esc` fecha descartando `motivoRejeicao` em rascunho (confirmando se houver texto) e o foco volta ao botão de origem. Um modal em pilha continua banido.

## Accessibility Floor

Comportamental. **A auditoria de contraste vive em `DESIGN.md` → `contrast:`** — pares, números medidos e o piso de cada um. Referência de texto 4.5:1; referência de estado/indicador 3:1 (WCAG 1.4.11). Os dois casos que importam aqui, ambos medidos: `accent-ink #8A6802` = 5.13:1 em `surface` e 4.79:1 em `primary-soft` (foco, aresta ativa, marca de selecionado, aba ativa, contador no teto); `status-pending #8A6802` sobre `status-pending-bg` = 4.79:1 (badge "Aguardando aprovação" e "4 de 4 unidades"). `primary #DCA703` a 2.18:1 é preenchimento e **nunca** carrega um estado sozinho.

- WCAG 2.2 AA na superfície web.
- Combobox ARIA 1.2 completo: `role="combobox"` + `aria-expanded` + `aria-controls` + `aria-autocomplete="list"` no `<input>`; `role="listbox"` no popup; `role="option"` + `aria-selected` em cada linha; `aria-activedescendant` no input apontando para o id da opção ativa; `id` do `<label>` amarrado ao campo; `aria-describedby` apontando **tanto** para o texto de ajuda **quanto** para a mensagem de dead end/erro; `aria-required` no campo `single`.
- A contagem de resultados do catálogo é anunciada em `aria-live="polite"` ("12 unidades para 'ipir'") — o popup é a informação, o foco não se move. Quando o teto trunca a lista em 20, a anúncio é o número **real** de correspondências, não o número de linhas renderizadas.
- Chips são focáveis individualmente, com `aria-label` no botão de remoção ("Remover Fatec Ipiranga"). Nunca só cor, nunca só ícone.
- A recusa do teto é **anunciada** (texto + `aria-live`), nunca só cor, nunca vermelho. O contador é texto ("2 de 4 unidades"), nunca um glifo seco.
- O popup **nunca** é hover-only: abre por clique ou teclado em qualquer viewport — e apenas quando há consulta, nunca ao focar o campo vazio.
- Busca e filtros da vitrine com `<label>` explícito; contagem de resultados da **grade** anunciada via `aria-live="polite"` ("— projetos encontrados"). [open] A região `aria-live` da grade e a do catálogo ainda colidem: separar as duas ao implementar.
- Estado e papel nunca só por cor: badges têm texto; aba ativa tem texto + sublinhado; papel do usuário é texto ("Aluno Fatec" / "Professor CPS").
- Abas do Perfil implementadas como ABAs reais (`role="tablist"`/`tab`/`tabpanel`, `aria-selected`), não como conjunto de links fingindo abas.
- Animações ≤ 250ms; `prefers-reduced-motion` reduz a transições de opacidade; sem parallax.
- Área de toque mínima 40×40px em atalhos, chips de filtro e chips de vínculo (mobile).
- Cards clicáveis com link real (não `<div onclick>`), com `aria-label` quando a capa não tem texto alternativo útil.
- Foco visível: anel de **2px `{colors.accent-ink}`** — `{components.institution-autocomplete.focus-border}`, idem `{components.search-input.focus-border}` e `{components.profile-tab.active-underline}` — medido em 5.13:1 sobre `{colors.surface}` e 4.79:1 sobre a opção ativa em `{colors.primary-soft}`. O foco nunca é sinalizado só pela borda de 1px do campo.
- O dialog de aprovação mantém o foco **contido** enquanto aberto, devolve o foco ao botão que o abriu, e `Esc` fecha **descartando** o `motivoRejeicao` em rascunho — com confirmação se o campo já tiver texto.

## Responsive & Platform

| Breakpoint | Home | Perfil | Seleção de instituição (`single` na Seção 2 do form · `multi` na aba "Instituições (0–4)") |
|---|---|---|---|
| `≥ lg` (1024px+) | Hero em linha; vitrine em 3 colunas; busca largura total | Header de perfil em linha (avatar + identidade + atalhos à direita); abas + 2 colunas de lista | Popup ancorado ao campo, largura do campo, teto de 320px com rolagem interna (`{components.combo-popup}`); chips em linha única; contador à direita da fileira |
| `md` (768–1023px) | Hero empilhado; vitrine em 2 colunas | Header empilhado; atalhos viram card em fileira rolável | Popup ainda na largura do campo; chips quebram em duas linhas; contador permanece visível ao lado da fileira |
| `< md` (sm) | Vitrine em 1 coluna; busca 100%; atalhos 2×2 | Identidade centrada; atalhos 2×2; abas com scroll horizontal; listas em 1 coluna | Popup **100% da largura do campo**, sempre ancorado — nunca um popover com overflow horizontal; teto de 320px com rolagem interna; chips quebram livremente; contador visível ao lado da fileira; o rodapé sticky de submit **nunca** fica coberto pelo popup (`z-index` acima de `{elevation.sticky-footer}`) |

Web responsiva, não app nativo. Postagem, seleção de instituição e aprovação mantêm o padrão desktop da plataforma; leitura/busca são confortáveis em celular.

## Inspiration & Anti-patterns

- **Lifted from** o próprio shell existente (header sticky + menu lateral, footer) e a linguagem do formulário de postagem (seções, cantos retos, badge "Convidado"). Home, Perfil e formulário falam a mesma língua.
- **Lifted from o ARIA APG 1.2 (combobox)**: teclado, `aria-activedescendant` e fechamento por clique fora vêm da referência de acessibilidade. O visual não vem de biblioteca — a guarda é o campo reto/swiss já existente: não há `@angular/material` nem `@angular/cdk` neste repositório, e o default arredondado de Material seria rejeitado por `{rounded.DEFAULT}`.
- **Lifted from (decisão do usuário)** a **página de canal do YouTube** para o Perfil: bloco de identidade no topo, abas de conteúdo, e a sensação de "este é o meu espaço na plataforma". Usamos o *padrão estrutural*, não a estética editorial do YouTube.
- **Lifted from** portais acadêmicos institucionais para a Home: vitrine em grade com capa + descrição curta + instituição, busca e filtro explícitos.
- **Rejected — Hero "startup"**: gradiente, foto gigante, claims vazios. O hero da Home é tipográfico e institucional.
- **Rejected — Login antes de ver qualquer coisa**: a vitrine é pública (CAP-7); nenhum wall de login na Home.
- **Rejected — Perfil público / redes sociais**: perfil é privado e instrumental; sem seguir, curtir, comentar ou bio editável no MVP.
- **Rejected — Cards com ações ocultas no hover (só ícone)**: mobile quebraria; ações do card são só navegação.
- **Rejected — Vermelho em "rejeitado"**: vermelho pertence à marca CPS; rejeitado é cinza, neutro e honesto, com o motivo sempre à vista.
- **Rejected — página/tabela de 86 unidades, para qualquer papel**: é ferramenta de navegação, não escolha. Para o aluno ela saiu do fluxo; para o professor virou aba.
- **Rejected — recriar a seleção de instituição como rota paralela ao Perfil**: a rodada chegou a propor isso e reverteu. A seleção é uma aba, como "Meus Projetos" e "Fila"; uma rota duplicada seria duas superfícies para o mesmo trabalho, e é o que a CAP-8 proíbe.
- **Rejected — renderizar o catálogo completo antes da digitação**: o popup só existe depois que existe consulta.
- **Rejected — texto livre para catálogo fechado**: quebraria CAP-3/AD-5 e devolveria a manutenção do catálogo para o usuário.
- **Rejected — teto de 4 implícito, sem contador**: o limite precisa estar visível antes de ser atingido.
- **Rejected — vermelho em "não encontrado" ou no teto**: recusa de catálogo e limite de vínculo não são erro; vermelho CPS é marca.

## Key Flows

### Flow 1 — Visitação pública (Beatriz, comunidade acadêmica, noite de sexta) — UJ-4

1. Beatriz abre o portal pelo navegador; a Home carrega com skeleton e resolve para a vitrine.
2. Ela lê o hero ("Trabalhos que fazemos em sala, abertos a todos"), rola para a grade.
3. Digita "inteligência artificial" na busca; a grade reduz com contagem anunciada.
4. No filtro de instituição ela digita "ipir", confirma "Fatec Ipiranga" e refina com um chip de tag.
5. **Climax:** um card chama atenção — capa, título, descrição curta e o selo "Aprovado". Ela clica e vê o projeto completo (conteúdo, integrantes, repositório) **sem login — e sem nenhum botão de cadastro gritando**.
6. Failure: a API falha ao carregar → painel "Não foi possível carregar os projetos. [Tentar novamente]"; ela retenta e os filtros permanecem.

### Flow 2 — De visitante a publicadora (Mariana, aluna da Fatec) — UJ-1

1. Mariana acessa a Home deslogada e topa com o CTA do header "Login institucional".
2. Autentica via MSAL (tenant CPS); o retorno a leva ao **Perfil** — cabeçalho com avatar, nome, "Aluna Fatec" e a aba **"Meus projetos"** pré-selecionada.
3. O perfil dela está vazio. O empty state a convida: "Seu primeiro projeto ainda não foi publicado." e o atalho **"Novo Projeto"** fica em destaque amarelo.
4. Ela clica e cai direto no formulário em seções (`/projeto-forms`) — **sem passo prévio de instituição**. Na **Seção 2**, o campo "Instituição" é o autocomplete: digita "ipir", o popup devolve "Fatec Ipiranga", ela confirma com `Enter` e o campo vira "Cód. 291 · São Paulo · Região Litoral · Alterar".
5. Ela envia e volta ao Perfil: a aba "Meus projetos" agora mostra "Aguardando aprovação · 1" no grupo amarelo, e o atalho de contagem fecha o loop.
6. **Climax:** quando o professor aprova, o projeto migra para o grupo verde "Publicados" — e ela o encontra na vitrine pública da Home, que é a mesma plataforma de onde ela começou, agora com o trabalho dela somando ao acervo.
7. Failure: o projeto é rejeitado → ele cai no grupo cinza "Rejeitados" com o motivo inline; Mariana corrige e envia de novo pelo botão do grupo. Nenhum passo a força a decorar onde fica "o status".
8. Failure: a unidade dela não está no catálogo → beco sem saída honesto na Seção 2: "Sua unidade não está no catálogo oficial", quais campos foram buscados e uma dica de contato. Sem texto livre, sem auto-cadastro, sem vermelho — ela avisa o suporte e volta ao formulário.
9. Failure: ela manda publicar sem confirmar a instituição → **nada é submetido**; o foco volta ao campo, que ganha a borda de erro neutra e a mensagem. É a trava de UJ-1: o formulário exige a escolha, mas nunca a cobra com Vermelho nem com um `alert()`.
10. Failure: o `POST` falha → o formulário **continua inteiro** — capa, Seções 1–4 e o conteúdo do Editor.js permanecem, e ela tenta de novo. Perder um TCC inteiro por um erro de rede seria o pior resultado possível nesta tela.

### Flow 3 — Professor entre vínculo, fila e histórico (Roberto, professor adjunto) — UJ-2

1. Roberto loga pelo CTA da Home com e-mail `@cps.sp.gov.br`; o back-end classifica como professor.
2. **Primeiro acesso (0 vínculos):** o Perfil abre com a aba **"Instituições (0–4)"** já destacada, e o onboarding ocupa o lugar do campo: "Sem instituições vinculadas ainda — escolha até 4 unidades." O contador marca "0 de 4". Nada bloqueia — as demais abas seguem acessíveis (FR-6).
3. Ele digita "ipe" direto na aba. Nada renderiza antes de digitar; o popup devolve a unidade, ele confirma com `Enter`, um chip entra na fileira e o contador marca "1 de 4 unidades" — a consulta reabre para a próxima. **Ele não sai do Perfil em momento algum:** nenhuma página, nenhum segundo passo.
4. Ele conclui com **"Salvar vínculos"**. A aba **"Pendentes"** passa a ser a pré-selecionada nos próximos acessos, e a Home (faixa logada) oferece o atalho direto.
5. A fila de Pendentes lista os `AGUARDANDO_APROVACAO` das suas unidades; projetos em que ele é o `emailProfessorResponsavel` aparecem destacados.
6. **Climax:** no detalhe de um projeto, ele clica **Rejeitar** e o dialog exige o motivo — ele escreve a justificativa, confirma, e o projeto sai de Pendentes. Volta ao Perfil e o grupo "Aprovados"/histórico reflete o novo estado; o aluno (Mariana) vê o mesmo estado no lado dela.
7. **Clímax do teto:** ele tenta vincular a 5ª unidade. O contador já está em `accent-ink` no "4 de 4 unidades" e a quinta escolha é **recusada inline** com "Máximo de 4 instituições por professor" (AD-4) — cinza e amarelo-escuro, sem modal de erro, e o popup continua aberto. Para trocar, ele remove um chip e adiciona outro; nada quebra. ⚠️ Esse vínculo ainda não persiste: o endpoint professor↔instituição não existe (Story 2.3).
8. Failure: salva 0 vínculos no 1º acesso → sem bloqueio (FR-6); o Perfil apenas re-apresenta o onboarding de "Instituições (0–4)" até ele vincular. A vitrine pública continua acessível para ele entender o alcance do que aprova.

### Flow 4 — Filtro público, sem login (Beatriz de novo, terça de manhã) — UJ-4

1. Beatriz volta ao portal pelo link do e-mail; a Home resolve como visitante, sem sessão.
2. Ela quer um recorte: digita "energia" na busca e, no filtro de instituição, digita "são paulo". O popup casa por nome, cidade, código e região — tudo normalizado, acento ou não.
3. Ela confirma "Fatec São Paulo"; o filtro vira um chip removível e a grade filtra **por `instituicaoId`** — desta vez de verdade. Nenhum token, nenhum login, nenhum convite para entrar no meio do caminho (CAP-7).
4. Ela refina com um chip de tag e depois **remove o chip da instituição**; a grade volta ao recorte anterior. Nada some sozinho.
5. **Climax:** ela digita "mauá" no mesmo campo e o catálogo devolve a unidade de cidade. O que era um `<select>` de 86 linhas vira uma escolha de dois toques — e ela nunca precisou ver o catálogo inteiro para escolher.
6. Failure: ela digita "campinas" e nada casa → empty state **dentro** do popup, nomeando os campos buscados ("nome, cidade, código ou região"), em cinza. O filtro continua sendo o que é: recorte de catálogo público.