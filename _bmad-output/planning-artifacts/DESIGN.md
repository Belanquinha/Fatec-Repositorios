---
name: Fatec-Repositorios
status: final
updated: 2026-09-29
description: Repositório acadêmico da Fatec/CPS — vitrine pública de projetos aprovados, postagem por alunos e aprovação por professores. Angular + Tailwind; este DESIGN.md codifica a identidade visual Flat Technical Style, a semântica da paleta institucional CPS e o combobox de catálogo fechado usado para escolher instituição.
colors:
  # Paleta institucional (ver paleta.css / tailwind.config.js). Significado semântico fixo.
  primary: '#DCA703'        # amarelo-500 — Ação · Conquista Acadêmica · destaque
  primary-hover: '#C89803'  # amarelo-600 — hover de ações primárias
  primary-soft: '#FCF6E6'   # amarelo-50 — fundo suave de seleção/destaque
  primary-foreground: '#202124'
  cps: '#B20000'            # vermelho-500 — Centro Paula Souza (institucional). NÃO usar em erro/alerta.
  accent-ink: '#8A6802'     # amarelo-800 — variante de TEXTO e de INDICADOR do amarelo de marca.
                           # 4.79:1 em primary-soft, 5.13:1 em surface. Onde um estado precisa
                           # ser lido (foco, aresta de opção ativa, marca de selecionado), é esta
                           # cor — primary (#DCA703, 2.18:1) serve de preenchimento, não de traço.
  corporate: '#303190'      # azul-700 — Empresas & Conexão Corporativa (links de repositório, badges corporativas)
  corporate-soft: '#ECECFA' # azul-50 — fundos informativos de media/links
  success: '#00B32A'        # verde-500 — Comunidade acadêmica · confirmação
  success-soft: '#E6F7EA'   # verde-50 — fundo suave de confirmação
  background: '#F4F6F9'     # branco-500 — superfície de página
  surface: '#FEFEFE'        # branco-50 — cards e painéis
  text: '#202124'           # cinza-500 — tipografia e ícones
  text-strong: '#0D0E0F'    # cinza-900 — títulos
  text-muted: '#4D4D50'     # cinza-400 — legenda / secundário
  border-soft: '#98999A'    # cinza-200 — bordas estruturais
  border: '#6A6A6C'         # cinza-300 — controles (inputs/selects)
  # Estados da máquina de estados do projeto (AGUARDANDO_APROVACAO → APROVADO | REJEITADO)
  status-pending: '#8A6802'  # amarelo-800 — texto de badge "Aguardando aprovação" e contador no teto.
                           # Era #9C7702 (amarelo-700): 3.85:1 em status-pending-bg, ABAIXO de 4.5:1.
                           # Escurecido um degrau na rampa CPS para passar sem sair da família.
  status-pending-bg: '#FCF6E6'
  status-approved: '#007F1E'  # verde-700 — texto de badge "Aprovado"
  status-approved-bg: '#E6F7EA'
  status-rejected: '#202124'  # cinza-500 — texto de badge "Rejeitado"
  status-rejected-bg: '#E9E9E9'
  # Erro de SISTEMA é neutro, nunca vermelho e nunca uma cor nova.
  # A identidade do erro é o peso da borda (2px) + a mensagem, não o matiz.
  error-ink: '#202124'        # texto do erro (15.96:1 em surface)
  error-border: '#202124'     # 2px no campo inválido — o identificador real (15.96:1)
  error-surface: '#F0F0F0'    # painel de erro (separação decorativa sobre surface)
contrast:
  # Auditoria medida. Referência de texto 4.5:1; referência de estado/indicador 3:1 (WCAG 1.4.11).
  # O contrato lived here, não em prosa: uma promessa de "todos ≥ 4.5:1" sem número é falsa por construção.
  text-on-surface: 'text #202124 = 15.96:1 · text-strong #0D0E0F = 19.16:1 · text-muted #4D4D50 = 7.38:1'
  text-on-primary: 'primary-foreground #202124 on primary #DCA703 = 9.14:1'
  status-pending: 'status-pending #8A6802 on status-pending-bg #FCF6E6 = 4.79:1 · on surface = 5.13:1'
  status-approved: 'status-approved #007F1E on status-approved-bg #E6F7EA = 4.65:1'
  status-rejected: 'status-rejected #202124 on status-rejected-bg #E9E9E9 = 13.34:1'
  indicator-accent: 'accent-ink #8A6802 on surface = 5.13:1 · on primary-soft = 4.79:1 (foco, aresta ativa, marca de selecionado)'
  ui-boundary: 'border #6A6A6C on surface = 5.35:1 (contorno de campo, chip e popup) · border-soft #98999A = 2.83:1 — apenas borda decorativa de card, nunca fronteira de controle'
  error: 'error-ink #202124 on surface = 15.96:1 · error-border #202124 = 15.96:1 · error-surface #F0F0F0 = decorativo'
  banned-for-contrast: 'primary #DCA703 (2.18:1 em surface) e primary-soft #FCF6E6 (1.05:1 vs surface) nunca carregam sozinhos um estado'
typography:
  # Fonte de marca: Inter (carregada via Google Fonts / @fontsource; fallback: -apple-system, 'Segoe UI', system-ui, sans-serif).
  # Rampa física herdada de tipografia.css (h1–h5, s1–s3, b1–b2).
  h1:
    fontFamily: 'Inter'
    fontSize: 3rem
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  h2:
    fontFamily: 'Inter'
    fontSize: 2.5rem
    fontWeight: '700'
    lineHeight: '1.15'
    letterSpacing: -0.01em
  h3:
    fontFamily: 'Inter'
    fontSize: 2rem
    fontWeight: '700'
    lineHeight: '1.2'
  h4:
    fontFamily: 'Inter'
    fontSize: 1.75rem
    fontWeight: '600'
    lineHeight: '1.25'
  h5:
    fontFamily: 'Inter'
    fontSize: 1.5rem
    fontWeight: '600'
    lineHeight: '1.3'
  subtitle:
    fontFamily: 'Inter'
    fontSize: 1.5rem
    fontWeight: '400'
    lineHeight: '1.4'
  subtitle-sm:
    fontFamily: 'Inter'
    fontSize: 1.25rem
    fontWeight: '400'
    lineHeight: '1.4'
  body:
    fontFamily: 'Inter'
    fontSize: 1rem
    fontWeight: '400'
    lineHeight: '1.6'
  body-sm:
    fontFamily: 'Inter'
    fontSize: 0.875rem
    fontWeight: '400'
    lineHeight: '1.5'
  label:
    fontFamily: 'Inter'
    fontSize: 0.75rem
    fontWeight: '700'
    letterSpacing: '0.08em'
    textTransform: 'uppercase'
rounded:
  # Filosofia: cantos 100% retos (Flat Technical / Swiss). radius 0 é o DEFAULT em toda superfície.
  DEFAULT: 0px
  sm: 0px
  md: 0px
  lg: 0px
  full: 9999px   # exceção única: fotos de perfil e ícones circulares avatares/status
spacing:
  # Escala do Tailwind herdada (base 4). Sem espaçamentos custom do produto.
  '1': 4px
  '2': 8px
  '3': 12px
  '4': 16px
  '5': 20px
  '6': 24px
  '8': 32px
  '10': 40px
  '12': 48px
  '16': 64px
  gutter: 24px
  section-gap: 64px
  content-max: 1152px   # max-w-6xl: largura máxima de conteúdo
  page-margin: 16px
components:
  button-primary:
    background: '{colors.primary}'
    foreground: '{colors.primary-foreground}'
    radius: '{rounded.DEFAULT}'
    fontWeight: '700'
    padding: '0.75rem 1.75rem'
  button-secondary:
    background: 'transparent'
    foreground: '{colors.text}'
    border: '1px solid {colors.border}'
    radius: '{rounded.DEFAULT}'
    fontWeight: '600'
  project-card:
    background: '{colors.surface}'
    border: '1px solid {colors.border-soft}'
    radius: '{rounded.DEFAULT}'
    shadow: '{elevation.card}'
  search-input:
    background: '{colors.surface}'
    border: '1px solid {colors.border}'
    radius: '{rounded.DEFAULT}'
    focus-border: '2px solid {colors.accent-ink}'   # anel de foco: accent-ink, não primary (2.18:1 falha 1.4.11)
  # Campo de instituição. É o MESMO campo nos três usos: mode=single (Seção 2
  # do /projeto-forms), mode=multi (aba "Instituições (0-4)" do Perfil do professor)
  # e mode=filter (filtro da Home, sem login). O visual é idêntico nos três modos;
  # o que diverge (contador, chips, save, limite) mora no template de cada consumidor.
  institution-autocomplete:
    background: '{colors.surface}'
    border: '1px solid {colors.border}'
    radius: '{rounded.DEFAULT}'
    focus-border: '2px solid {colors.accent-ink}'
    error-border: '2px solid {colors.error-border}'
    icon: 'caret-down (Material Symbols), right-aligned'
  # A camada flutuante do combobox. O max-height é o teto visual da lista; a rolagem é interna.
  # Contorno em `border` e não em `border-soft`: o popup é uma fronteira de controle (5.35:1),
  # não uma borda decorativa de card (2.83:1).
  combo-popup:
    background: '{colors.surface}'
    border: '1px solid {colors.border}'
    radius: '{rounded.DEFAULT}'
    shadow: '{elevation.raised}'
    max-height: '320px'
  # Linha da listbox. Radius 0 e anatomia de linha reta — opção não é card.
  combo-option:
    thumbnail: '24px square logo, radius 0'
    title: '{typography.body.fontFamily} / 1rem / 700'
    title-foreground: '{colors.text}'
    meta: '{typography.body-sm.fontFamily}, color {colors.text-muted}'
    active-background: '{colors.primary-soft}'
    active-inset: '2px solid {colors.accent-ink} (left edge)'
    selected-mark: 'Material Symbols check, {colors.accent-ink}'
  institution-chip:
    background: '{colors.surface}'
    border: '1px solid {colors.border}'   # fronteira de controle — ver combo-popup
    radius: '{rounded.DEFAULT}'
    remove-icon: 'Material Symbols close, {colors.text-muted}'
    label: '{typography.body-sm}'
  # Contador de vínculos do professor, na aba "Instituições (0-4)" do Perfil.
  # Rótulo por extenso — "2 de 4 unidades", nunca o glifo seco "2/4". No teto vira
  # accent-ink, nunca cps: um limite de vínculos não é erro — o vermelho CPS
  # é institucional e só institucional.
  selection-counter:
    foreground: '{colors.text-muted}'
    value-foreground: '{colors.text-strong}'
    fontFamily: '{typography.label.fontFamily}'
    fontSize: '{typography.label.fontSize}'
    fontWeight: '{typography.label.fontWeight}'
    at-cap-foreground: '{colors.accent-ink}'
  # Chips de palavras-chave da Home — distinta do institution-chip (contorno) porque é
  # preenchida quando ativa. Convive com o filtro de instituição, que é um institution-chip.
  filter-chip:
    background: '{colors.surface}'
    border: '1px solid {colors.border}'
    radius: '{rounded.DEFAULT}'
    active-background: '{colors.primary}'
    active-foreground: '{colors.primary-foreground}'
  # O filtro de instituição da Home é o mesmo institution-autocomplete em mode=filter
  # — sem <select> nativo visual. Substitui o <select> com 86 <option> do catálogo público.
  filter-select:
    reuse: '{components.institution-autocomplete}'
    selected: '{components.institution-chip}'
  # Diálogo de aprovação/rejeição. Única superfície com scrim no produto: a layer
  # elevada que mais tensiona o flat — daí o contorno duro e o scrim cinza, sem blur.
  dialog:
    background: '{colors.surface}'
    border: '1px solid {colors.border}'
    radius: '{rounded.DEFAULT}'
    shadow: '{elevation.raised}'
    scrim: 'rgba(32, 33, 36, 0.60)'
  sticky-footer:
    background: '{colors.surface}'
    border: '1px solid {colors.border-soft}'
    radius: '{rounded.DEFAULT}'
    shadow: '{elevation.sticky-footer}'
  # Badge informativo — "Professor Convidado" (CAP-4/FR-9). Sem estado, sem semântica
  # de erro: é a dica azul que diz o que o sistema sabe e o que ele não faz.
  badge-informative:
    foreground: '{colors.corporate}'
    background: '{colors.corporate-soft}'
    border: 'none'
  badge-pending:
    foreground: '{colors.status-pending}'
    background: '{colors.status-pending-bg}'
    border: 'none'
  badge-approved:
    foreground: '{colors.status-approved}'
    background: '{colors.status-approved-bg}'
    border: 'none'
  badge-rejected:
    foreground: '{colors.status-rejected}'
    background: '{colors.status-rejected-bg}'
    border: 'none'
  quick-action-card:
    background: '{colors.surface}'
    border: '1px solid {colors.border-soft}'
    radius: '{rounded.DEFAULT}'
    iconcolor: '{colors.primary}'
  profile-header:
    background: '{colors.surface}'
    border: '1px solid {colors.border-soft}'
    radius: '{rounded.DEFAULT}'
  profile-name:
    fontFamily: '{typography.h2.fontFamily}'
    fontSize: '{typography.h2.fontSize}'
    fontWeight: '{typography.h2.fontWeight}'
  profile-tab:
    foreground: '{colors.text-muted}'
    active-foreground: '{colors.accent-ink}'
    active-underline: '2px solid {colors.accent-ink}'
elevation:
  # Extensão de projeto (fora do subconjunto do spec): a spec carrega cores, tipografia,
  # rounded, spacing e components. O flat Swiss precisa nomear as três camadas de elevação do
  # produto, e um consumidor não deve inventá-las. `raised` serve popup e diálogo.
  card: '0 1px 2px rgba(13, 14, 15, 0.06), 0 2px 6px rgba(13, 14, 15, 0.04)'
  raised: '0 4px 12px rgba(13, 14, 15, 0.10)'
  sticky-footer: '0 -4px 12px rgba(13, 14, 15, 0.08)'
---

## Brand & Style

Fatec-Repositorios é um repositório acadêmico com cara institucional e intenção pública: uma vitrine de trabalhos feitos em sala. A expressão visual segue o **Design Suíço / Flat Technical Style** — superfícies limpas, hierarquia tipográfica forte, sem enfeite decorativo e com **cantos 100% retos** (`border-radius: 0`). A marca gráfica é "LuePad" (logo + wordmark do header). A paleta vem do Centro Paula Souza, e cada cor tem um **significado institucional fixo** — cor não é decoração, é semântica.

A Home é a porta de entrada pública: herói tipográfico direto, busca e filtros técnicos, e a grade de projetos aprovados como o corpo da página. Nada nela deve parecer "app de feira de startups": é um instrumento de curadoria do trabalho acadêmico. Para quem está logado, somam-se atalhos de ação por papel — mas a vitrine continua sendo a voz principal da tela.

O catálogo de instituições é uma **lista oficial e fechada** — 86 unidades em operação, mantidas pelo admin — e por isso todo campo de instituição é um controle **tipo-para-revelar**: nada é renderizado antes de o usuário digitar, e texto livre nunca é aceito. Despejar 86 linhas antecipadas é uma ferramenta de navegação, não uma escolha, e quebra a disciplina de uma superfície, um acento, fluxo mínimo. O aluno encontra esse campo **dentro do formulário**, na Seção 2 de `/projeto-forms`; o professor, dentro da aba "Instituições (0–4)" do Perfil. **A página dedicada de seleção saiu do produto** — não sobrou rota, e é essa remoção que faz a superfície do professor obedecer à mesma disciplina de fluxo mínimo do aluno.

## Colors

A paleta é a institucional CPS, com **papéis semânticos imutáveis** — a mesma regra que vale no formulário de postagem (`DESIGN-STORY-3.1`) vale na Home:

- **Amarelo Conquista (`primary`)** é a cor de ação e de destaque: CTA primário ("Novo Projeto", "Entrar para publicar"), item de menu ativo, foco de campo, ícones de atalho. Nunca é usado como cor de fundo de página.
- **Vermelho CPS (`cps`)** é exclusivamente institucional (marca/unidades CPS). **Nunca** em mensagens de erro, alerta ou rejeição — isso piratar um significado da identidade.
- **Azul Mercado (`corporate`)** representa conexão corporativa: links de repositório (GitHub/GitLab), badges de "link externo", dicas informativas de mídia/ferramentas.
- **Verde Estudante (`success`)** representa a comunidade acadêmica e a confirmação: estados "Aprovado", avatares de integrantes, ícones de sucesso.
- **Branco Neutro (`background` / `surface`)** é a superfície da aplicação: página em `background`, cards e painéis em `surface`.
- **Cinza (`text`, `text-muted`, `border`)** carrega tipografia, ícones e bordas estruturais — o cinza é o "passivo" que mantém o flat técnico calmo.

Rule of thumb: **uma cor de destaque por superfície.** Se algo já está amarelo, o resto da tela respira em branco/cinza.

`{colors.primary-soft}` (amarelo-50) é o fundo de confirmação de seleção — a linha de opção ativa dentro do combobox e o realce de uma escolha já feita — nunca uma superfície inteira. Os estados de **unidade não encontrada no catálogo** e de **teto de vínculos atingido** são resolvidos em cinza (`{colors.text-muted}`) mais **`{colors.accent-ink}`**, nunca em vermelho CPS: recusa de catálogo não é erro do sistema, e o vermelho CPS continua sendo marca, nada mais.

**Erro é neutro, e isso é uma regra, não uma falta de opção.** O `cps` é vermelho institucional e está proibido em erro; a paleta, portanto, **não tem** — e não deve ter — uma cor de erro própria. Um erro de sistema se identifica pelo **peso**: `{colors.error-border}` a 2px no campo, `{colors.error-surface}` como painel, e a mensagem em `{colors.error-ink}`. Nunca por matiz. É a mesma honestidade do badge "Rejeitado" em cinza: recusa e falha são coisas que o sistema diz, não coisas que ele alarma.

**A auditoria de contraste está no frontmatter, em `contrast:`** — pares, números medidos e o piso de cada um (4.5:1 para texto, 3:1 para estado/indicador). A consequence que mais importa: **`{colors.primary}` (`#DCA703`) é preenchimento, nunca traço.** A 2.18:1 sobre `surface` ela não pode carregar sozinha um estado — foco, aresta de opção ativa, marca de selecionado e sublinhado de aba usam `{colors.accent-ink}` (`#8A6802`, 5.13:1), que é o mesmo amarelo um degrau mais escuro na rampa CPS. Pelo mesmo motivo, `border-soft` (2.83:1) é **só** borda decorativa de card; campo, chip e popup usam `border` (5.35:1).

## Typography

Rampa física de `tipografia.css`: display em `h1` (3rem) apenas no hero da Home; `h2`–`h5` para seções e títulos de card; `subtitle`/`subtitle-sm` para legendas e descrição curta; `body` e `body-sm` para texto corrido e metadados; `label` (maiúsculas, tracking `0.08em`) para eyebrows como "Projetos aprovados", "Busca por" e cabeçalhos de seção.

- Títulos em `text-strong` (`cinza-900`); corpo em `text` (`cinza-500`); legenda em `text-muted` (`cinza-400`).
- A fonte de marca é **Inter**, carregada globalmente (Google Fonts ou `@fontsource`) com fallback para a sans-serif do sistema. A identidade vem da **escala + peso + Inter**, não de variações de fonte; Inter é a única família da aplicação.
- Sem serifa complexa, sem itálico decorativo, sem "display" em texto de corpo. O hero da Home é o único momento de tipo grande; no perfil, o nome do usuário usa `h2`.

## Layout & Spacing

Escala Tailwind (base 4) herdada em todo o front. Padrões da Home:

- Largura máxima de conteúdo: `content-max` (1152px, `max-w-6xl`), centralizada; margem lateral mínima de `page-margin`.
- Grade da vitrine: **1 coluna em `<sm`; 2 em `md`; 3 em `lg+`** (`grid-cols-1 md:grid-cols-2 lg:grid-cols-3`), com gabarito `gap-6`.
- Seções verticais separadas por `section-gap`; dentro de um card, respiro por `6` (24px).
- Hero: bloco de texto esquerdo-alinhado + tomada de ação; abaixo, barra de busca em largura total e filtros em linha.
- Header sticky (4.5rem) e footer como existem hoje (`app-header` / `app-footer`) emolduram a página; a Home não reinventa o shell.

## Elevation & Depth

Elevação discreta, só para organização de camadas — nunca para hierarquia emocional:

- Cards de projeto: `elevation.card` à laje (1px + sombra sutil), intensificando para `elevation.raised` no hover.
- Rodapé de ação fixo (fila/atalhos se houver) usa `elevation.sticky-footer`.
- Dropdowns e sugestões — incluindo o popup do `institution-autocomplete`: `elevation.raised`.
- Nada de gradientes, nada de sombras coloridas, nada de blur de fundo.

## Shapes

Cantos retos em **tudo**: `rounded` global = `0`. Isso é a assinatura do Flat Technical e já está imposto no formulário de postagem (`border-radius: 0 !important` em `upload-projeto.css`). A exceção única `full` (9999px/círculo) fica restrita a **fotos de perfil** e **ícones-avatar circulares** (como o botão de perfil do header) — e nada mais. Não há "pill" em badges de estado: no estilo Swiss, badge de estado é um quadrado com borda solta, código e tom semântico.

As miniaturas de logo de 24px dentro do combobox são **quadradas** (`radius 0`), coerentes com a língua plana; a exceção `full` continua reservada às fotos de perfil e aos avatares circulares.

## Components

Componentes visuais canônicos do produto (especificação de comportamento em `EXPERIENCE.md`):

- **button-primary** — `{colors.primary}` com texto `{colors.primary-foreground}`, radius 0, `font-bold`. Ações: "Novo Projeto", "Entrar para publicar"/"Publicar meu trabalho". Hover: `{colors.primary-hover}`.
- **button-secondary** — contorno `{colors.border}`, texto `{colors.text}`, radius 0. Ações de apoio ("Ver instituições", "Minha fila", cancel).
- **search-input** — campo de busca da vitrine: `{colors.surface}`, borda `{colors.border}`, focus com anel `2px solid {colors.accent-ink}`. Ícone de lupa à esquerda (Material Symbols).
- **filter-select / filter-chip** — o filtro de instituição da Home é o **mesmo `institution-autocomplete` em `mode=filter`**: catálogo oficial, sem texto livre (CAP-3/AD-5), operável por visitante **sem token e sem login** (CAP-7). A escolha aparece como `institution-chip`, e o que era um `<select>` nativo de 86 `<option>` deixa de existir como visual próprio. `filter-chip` é o chip de **palavras-chave** — distinto do `institution-chip` por ser preenchido quando ativo (`{colors.primary}` / `{colors.primary-foreground}`) em vez de contornado.
- **institution-autocomplete** — o campo de instituição, compartilhado por três usos: Seção 2 do formulário do aluno (`mode=single`), aba "Instituições (0–4)" do Perfil do professor (`mode=multi`) e filtro da Home (`mode=filter`). É **o mesmo campo com o mesmo visual nos três modos** — `{colors.surface}`, borda `1px {colors.border}`, radius 0, focus com anel `2px solid {colors.accent-ink}`, `caret-down` (Material Symbols) à direita, inválido com `2px {colors.error-border}`. O modo altera o comportamento (valor único, chips com save, ou filtro), nunca a aparência.
- **combo-popup** — a listbox suspensa: `{colors.surface}`, borda `1px {colors.border}`, radius 0, `elevation.raised`, teto de `320px` com rolagem interna. É a **única camada flutuante** de uma página flat, junto do diálogo; nada mais na tela recebe sombra dessa ordem.
- **combo-option** — linha de opção com radius 0 e anatomia de linha reta: miniatura de logo 24px quadrada, título `1rem/700` em `{colors.text}` e metadado `body-sm` em `{colors.text-muted}`. A opção ativa recebe `{colors.primary-soft}` mais uma aresta esquerda `2px` de `{colors.accent-ink}` — marcação suíça por borda, nunca um realce arredondado, e nunca em `primary`, que não chega a 3:1. A já selecionada leva `check` (Material Symbols) em `{colors.accent-ink}` **e** `aria-selected`: a marca é redundante de propósito.
- **institution-chip** — a unidade já escolhida ou vinculada: `{colors.surface}`, borda `1px {colors.border}` (fronteira de controle, não decorativa), radius 0, rótulo em `{typography.body-sm}` e remoção por `close` (Material Symbols) em `{colors.text-muted}`. A fileira de chips é a representação visual da seleção — a linha de vínculos do professor e o filtro confirmado dentro da Home.
- **selection-counter** — o contador de vínculos do professor: valor em `{colors.text-strong}`, rótulo em `{typography.label}`, entorno em `{colors.text-muted}`, escrito por extenso ("2 de 4 unidades") e nunca como glifo seco. No teto o "4 de 4 unidades" passa a `{colors.accent-ink}` e a quinta escolha é recusada inline — **nunca vermelho CPS**, porque um limite de vínculos não é erro.
- **dialog** — o diálogo de aprovação/rejeição: `{colors.surface}`, borda `1px {colors.border}`, radius 0, `elevation.raised`, sobre scrim `rgba(32,33,36,0.60)`. É a única superfície com scrim no produto e, por isso, a que mais tensiona o flat: a resposta é contorno duro e cinza, sem blur nem sombra colorida.
- **sticky-footer** — o rodapé de ação do formulário: `{colors.surface}`, borda superior `1px {colors.border-soft}`, `elevation.sticky-footer`. Vive abaixo do popup do combobox em z, nunca coberto por ele.
- **badge-informative** — o badge "Professor Convidado" (CAP-4/FR-9): `{colors.corporate}` sobre `{colors.corporate-soft}`, quadrado, sem estado. É a dica informativa que diz o que o sistema sabe e o que ele **não** faz.
- **project-card** — `{colors.surface}` + borda `{colors.border-soft}`, radius 0. Anatomia: capa (16:9), título `h4`, descrição curta `body-sm`, linha de metadados (instituição, ano, tags), badge de estado quando o papel do usuário permite ver.
- **badge-pending / badge-approved / badge-rejected** — estados da máquina de estados (`AGUARDANDO_APROVACAO` amarelo, `APROVADO` verde, `REJEITADO` cinza). Quadrados, sem pill, sem vermelho CPS.
- **quick-action-card** — atalho por papel no hub do perfil (e, opcionalmente, na Home logada): card raso com ícone `{colors.primary}`, rótulo e microcopy; em fileira horizontal (`gap-4`).
- **profile-header** — cabeçalho do `Perfil` (padrão "canal" do YouTube): avatar, nome (`h2`), papel, e-mail institucional somente leitura; `{colors.surface}` sobre `{colors.background}`, borda `{colors.border-soft}`, radius 0.
- **profile-tab** — navegação por abas do perfil, dirigida por papel. Aba inativa: `{colors.text-muted}`; ativa: `{colors.accent-ink}` com sublinhado `2px` (`active-underline`). Mesma estrutura de abas para ALUNO e PROFESSOR, dados diferentes.
- **empty-state** — ícone Material Symbol + `h3` + `body-sm` + uma ação única (`button-primary` ou `button-secondary`).

## Do's and Don'ts

| Do | Don't |
|---|---|
| Usar cantos retos (radius 0) em todos os elementos | Usar `rounded-lg`/`rounded-xl` soltos — o estilo é flat |
| `primary` (amarelo) só para ação/destaque — uma vez por superfície | Pintar fundos de página ou áreas inteiras de amarelo |
| Reservar o vermelho CPS para marca/institucional | Vermelho em erro, alerta ou badge de rejeição |
| Estado APROVADO em verde, AGUARDANDO em amarelo, REJEITADO em cinza | Vermelho como "estado negativo" |
| Cinza para tipografia/bordas; branco para superfícies | Mais de uma cor de destaque por tela |
| Hero tipográfico em `h1` + uma ação primária | Hero com imagem pesada, gradiente ou carrossel |
| Cards com capa, título, descrição curta e metadados claros | Cards "fofos" com muito sombreado, cantos arredondados ou emojis |
| Campos de catálogo fechado por tipo-para-revelar — nada renderizado antes da digitação | Despejar as 86 unidades em tabela/lista antes de o usuário digitar |
| Combobox ARIA 1.2 completo: teclado, `aria-activedescendant`, fecha por clique fora | Dropdown que só abre no clique e não navega por teclado |
| Teto de 4 explícito: contador "2 de 4 unidades" e recusa em amarelo | Limite de instituições implícito ou ausente na UI |
| Uma superfície por job: instituição do aluno é campo no formulário, a do professor é aba no Perfil | Recriar a página de seleção como rota paralela ao Perfil |
| Unidade fora do catálogo → mensagem cinza honesta + contato | Vermelho CPS como "não encontrado" |
| Erro de sistema identificado por **borda 2px + mensagem**, em cinza | Inventar uma cor de erro, ou cromar o campo/vale de vermelho |
| `accent-ink` (`#8A6802`) para foco, aresta ativa, marca de selecionado, aba ativa | `primary` (`#DCA703`) como traço de estado — 2.18:1 não identifica nada |
| `border` (5.35:1) em campo, chip e popup | `border-soft` (2.83:1) como fronteira de controle |