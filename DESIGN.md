# Especificação Geral de UX/UI — Fatec-Repositorios

Documentação central do Design System, linguagem geométrica, padrões transversais, componentes comuns e **todas as telas e fluxos** da plataforma **Fatec-Repositorios**. Este documento é a **fonte única de design** (artefato `bmad-ux` do BMAD Method); especificações de tela individuais tornam-se capítulos aqui em vez de arquivos separados. A antiga `_bmad-output/specs/DESIGN-STORY-3.1-UPLOAD-PROJETO.md` foi absorvida no capítulo 4.3.2.

> **Decisões de produto incorporadas neste documento:**
> - **Professor responsável**: autocomplete dos professores do tenant CPS **+ liberação de e-mail livre** (badge "Professor Convidado"). **Sem envio de notificações no MVP** (registro informativo).
> - **Integrante**: **sem `papelNoProjeto` no MVP** (apenas Nome Completo + LinkedIn).
> - **Arquivos de imagem**: enviados ao **back-end** via `multipart` e servidos por ele (`imagemCapaUrl` interna, ex.: `/api/uploads/...`).
> - **`imagensExtras` removida**: imagens complementares do projeto são **blocos nativos do Editor.js** (dados vivem no `conteudoEditorJs`).
> - **Admin**: acesso via **MSAL** (e-mail do dono semeado no banco), sem senha de formulário.
> - **Erros/avisos**: cor alvo laranja `#D97706`. O **vermelho CPS é proibido** em mensagens de erro ou alerta.
> - **Landing pós-login (elicitação 2026-09-22)**: professor sem instituições vinculadas cai **direto na Seleção de Instituições** (1º login = onboarding); professor com vínculos vai para a **Fila de Aprovação**; **aluno** vai para **Meus Projetos**; **admin** vai para `admin-main`.
> - **Perfil (elicitação 2026-09-22)**: todo usuário autenticado (aluno e professor) possui **tela de Perfil** com dados do MSAL + contexto do papel; professor **vê e edita suas instituições (0 a 4)** também pelo perfil.
> - **Quem aprova (elicitação 2026-09-22)**: **qualquer professor vinculado à instituição do projeto** pode aprovar/rejeitar; o professor com `emailProfessorResponsavel` fica apenas **destacado** na fila.

---

## 1. Design System & Identidade Visual

### 1.1 Diretriz Geométrica: Cantos Retos Técnico-Editoriais (`border-radius: 0`)

A plataforma **Fatec-Repositorios** adota a linguagem visual baseada no **Design Suíço (Swiss Style / Flat Technical Design)** com cantos 100% retos (`border-radius: 0`).

#### Raciocínio & Benefícios de UX:
- **Identidade Única**: diferencia a plataforma dos portais genéricos do mercado (bordas arredondadas padrão), conferindo assinatura visual técnica e marcante.
- **Precisão Acadêmica**: transmite clareza, rigor arquitetural e alinhamento tipográfico limpo — valores de uma faculdade de tecnologia.
- **Sensação de Leveza**: elimina curvaturas desnecessárias e sombras acumuladas, criando uma grade visual leve e fácil de escanear.

```css
/* Diretriz Global de Geometria em paleta.css / reset.css */
* {
  border-radius: 0 !important;
}
```

### 1.2 Filosofia e Paleta Semântica de Cores (`paleta.css`)

Cada cor possui significado semântico no ecossistema FATEC:

| Cor | Variável CSS | Hexadecimal | Significado Semântico | Uso na Interface |
| :--- | :--- | :--- | :--- | :--- |
| 🟡 **Amarelo Conquista** | `--amarelo-500` | `#DCA703` | Conquista / Excelência Acadêmica | Cor primária. Botão principal ("Enviar para Avaliação Acadêmica"), badges de destaque, troféus. |
| 🔴 **Vermelho CPS** | `--vermelho-500` | `#B20000` | Centro Paula Souza (Institucional) | Logos e marcas institucionais FATEC/CPS. **⚠️ Regra de Ouro: NUNCA usar em mensagens de erro/alerta!** |
| 🔵 **Azul Mercado** | `--azul-700` | `#303190` | Empresas & Conexão Corporativa | Badges corporativas, links de repositório (GitHub/GitLab/Bitbucket), conexões externas. |
| 🟢 **Verde Estudante** | `--verde-500` | `#00B32A` | Comunidade Acadêmica / Estudantes | Avatares de alunos, confirmações de envio para o professor, estado `APROVADO`. |
| ⚪ **Branco Neutro** | `--branco-500` | `#F4F6F9` | Superfície & Fundo da Aplicação | Fundo de página e áreas limpas de conteúdo. |
| 🔘 **Cinza Suave** | `--cinza-500` | `#202124` | Tipografia & Bordas Estruturais | Textos principais, rótulos, bordas. |
| 🟠 **Alerta Neutro** | — | `#D97706` | Validação de formulários e avisos | Erros, alertas e orientações — sem conflitar com a marca do CPS. |

### 1.3 Tipografia & Ícones

- **Família**: fontes do sistema/`sans-serif` (sem fonte custom exótica), mantendo neutralidade técnica.
- **Escala hierárquica:**
  - Título de tela/seção: `text-xl font-bold`
  - Subtítulos e nomes: `text-base font-semibold`
  - Corpo de texto: `text-base`
  - Rótulos, captions, metadados: `text-xs`/`text-sm`
- **Ícones**: `material-symbols-outlined` (já adotado no rodapé da Story 3.1). Ícone `info` para avisos; `send` para envio; lixeira para remoção.

---

## 2. Padrões Transversais (aplicam-se a TODAS as telas)

### 2.1 Estados de interface
Toda tela deve prever 4 estados quando consumir API:

| Estado | Regra |
| :--- | :--- |
| **Loading** | Skeleton/spinner neutro em cinza; nunca bloquear a página sem feedback. |
| **Vazio** | Copy orientadora + ação principal (ex.: professor sem projetos na fila → "Selecione instituições" ; catálogo sem resultados → "Ajuste sua busca"). |
| **Erro** | Toast alvo laranja `#D97706` com a mensagem do envelope `{"codigo", "mensagem"}`. **Vermelho CPS proibido.** |
| **Sucesso** | Feedback verde `--verde-500` (confirmação de envio, estado `APROVADO`). |

### 2.2 Validação de formulário
- Validação inline **no blur/change**: campo com erro recebe borda laranja `#D97706` + mensagem `text-xs` logo abaixo.
- Validação de cardinalidade (máx 4 instituições, mín 1 integrante) sempre reforçada no servidor com resposta clara (`400/409` + mensagem).
- Contadores visíveis onde houver limite (ex.: descrição curta `0 / 144`).

### 2.3 Responsividade
- **Desktop**: grid multi-coluna; rodapé de ação fixo (`sticky bottom-0`).
- **Mobile (< `sm`)**: empilhamento vertical; botões `w-full`; rodapé de ação empilhado (`flex-col`).
- Formulários longos organizados em **painéis por seções ativas**.

---

## 3. Componentes Comuns

| Componente | Especificação |
| :--- | :--- |
| **Header** | Logo/identidade FATEC-CPS (vermelho institucional no logo); nav conforme papel (visitante: catálogo; aluno: + Meus Projetos/Novo Projeto; professor: + Área do Professor; admin: admin). À direita: botão "Entrar" (visitante) ou menu do usuário com nome e "Sair". |
| **Footer** | Rodapé institucional simples (marca CPS), sem links operacionais. |
| **CardProjeto** | Usado no catálogo público e na fila do professor. Contém capa, título, descrição curta, instituição, estado (badge de cor: `AGUARDANDO_APROVACAO` neutro, `APROVADO` verde, `REJEITADO` laranja), clique abre detalhe. |
| **Botões** | Primário: fundo `--amarelo-500`, texto cinza-900, sem borda arredondada; hover `--amarelo-600`. Secundário: contorno cinza. Desabilitado: opacidade reduzida. |
| **Dropzone de Capa** | Área de arrastar/clicar com preview em tempo real e opção de substituir. **Envio `multipart` ao back-end**; valida tipo (imagem) e tamanho máximo definido no back-end; resposta define `imagemCapaUrl`. |
| **Autocomplete Professor** | Input com sugestões dos professors do tenant. Se o e-mail digitado não estiver cadastrado, aceitar mesmo assim exibindo badge informativo: `[ ✉️ Professor Convidado — registro informativo, sem envio de notificações ]`. |
| **Select Instituição** | Dropdown populado via API (`GET /instituicoes`); sem texto livre. |
| **Toast/Snackbar** | Mensagens globais de erro (laranja), aviso (laranja) e sucesso (verde); auto-dismiss de ~4s, exceto erros que permanecem. |

---

## 4. Fluxos por Papel & Especificação de Telas

### 4.1 Mapa de Fluxos

| Papel | Fluxo | Telas |
| :--- | :--- | :--- |
| **Visitante** | Buscar → visualizar | Catálogo Público → Ver Projeto |
| **Aluno** | Login → publicar → acompanhar | Login → Upload de Projeto → Meus Projetos → Perfil |
| **Professor** | Login → onboarding/associar instituições → aprovar/rejeitar | Login → Seleção de Instituições → Fila de Aprovação → Perfil |
| **Admin** | Manter catálogo | Admin Main → Cadastro de Instituição |

#### 4.1.1 Landing pós-login (MSAL)

Ação executada pelo sistema imediatamente após o retorno bem-sucedido do fluxo MSAL, antes de renderizar qualquer tela:

| Papel | 1º login (sem contexto) | Retorno (com contexto) |
| :--- | :--- | :--- |
| **Aluno** | → `meus-projetos` (estado vazio com CTA "Novo Projeto") | → `meus-projetos` |
| **Professor** | sem instituições vinculadas → `area-professor` (onboarding, seleção de instituições) | com vínculos → `area-professor/fila` (Fila de Aprovação) |
| **Admin** | → `admin-main` | → `admin-main` |

- A diferenciação **1º login vs retorno** é definida pela regra de negócio: professor com **zero instituições vinculadas** é tratado como "primeiro acesso" **sempre que** estiver zerado, não apenas na primeira autenticação (comportamento idempotente se ele remover todos os vínculos depois).
- O onboarding não é obrigatório nem bloqueante (D-3): o professor pode sair da seleção a qualquer momento; a Fila de Aprovação em estado vazio reitera o caminho de volta (`4.4.2`).

### 4.2 Visitante (não autenticado)

#### 4.2.1 Catálogo Público (`catalogo-publico`)
- **Propósito**: listar projetos `APROVADO` com busca e filtro (FR-12).
- **Elementos**: barra de busca por palavra-chave; filtro por instituição (select do catálogo); grid de `CardProjeto`.
- **Estados**: vazio → "Ajuste sua busca"; loading → skeletons.
- **Regra**: endpoints `permitAll()`; nunca exige token.

#### 4.2.2 Ver Projeto (`ver-projeto`)
- **Propósito**: detalhes completos de projeto `APROVADO` (Story 5.2).
- **Elementos**: capa, título, descrição curta, **conteúdo rico renderizado do Editor.js** (cabeçalhos, parágrafos, listas, código, citações e imagens), ano, palavras-chave, integrantes (nome, LinkedIn, papel), link do repositório em azul mercado. Sem ações de edição.
- **Nota**: sanitização/render segura do JSON Editor.js no cliente.

### 4.3 Aluno

#### 4.3.1 Meus Projetos (`meus-projetos`)
- **Propósito**: o aluno acompanha o status dos próprios projetos (fecha o ciclo da UJ-1 e da Story 4.2).
- **Elementos**: lista dos projetos do aluno com badge de estado (`AGUARDANDO_APROVACAO` | `APROVADO` | `REJEITADO`); no `REJEITADO`, exibir o `motivoRejeicao` em destaque laranja; CTA "Novo Projeto".
- **Estados**: vazio → "Você ainda não publicou projetos" + botão "Novo Projeto".

#### 4.3.2 Upload de Projeto (`projeto-forms`) — Story 3.1 (absorbed)
- **Propósito**: formulário de submissão em **Painel por Seções Ativas**, geometria reta.

**Seção 1 — Dados Gerais & Imagem de Capa**
- Título: input `text-xl font-bold`.
- Descrição curta: textarea com limite de **144 caracteres** e contador dinâmico (`0 / 144`).
- Capa: **Dropzone** com preview, substituição e envio `multipart` ao back-end.

**Seção 2 — Instituição & Validação Acadêmica**
- Instituição: select da API.
- E-mail do professor responsável: **Autocomplete** + e-mail livre com badge "Professor Convidado".
- Link do repositório: input de URL (GitHub/GitLab/Bitbucket) com ícone azul mercado.
- Palavras-chave: tags editáveis.

**Seção 3 — Conteúdo & Mídias (Editor.js)**
- Blocos suportados: cabeçalhos, parágrafos, listas, código e citações.
- **Imagens complementares = blocos de imagem nativos do Editor.js** (as imagens vivem no `conteudoEditorJs`; não há `imagensExtras`).

**Seção 4 — Integrantes do Projeto**
- Lista dinâmica com botão `+ Adicionar Integrante` (mínimo 1).
- Cada card: **Nome Completo**, Link do LinkedIn e botão Remover (lixeira). MVP sem campo "Papel no Projeto".

**Rodapé de Ação Final** (sticky bottom)
- Aviso: "O projeto será submetido à revisão do Professor Responsável antes da publicação."
- Botão primário amarelo: **"Enviar para Avaliação Acadêmica"** (ícone send).
- Sucesso: tela/estado verde de confirmação (Verde Estudante).

### 4.4 Professor

#### 4.4.1 Seleção de Instituições / Onboarding (`area-professor`)
- **Propósito**: associar de 0 a 4 instituições do catálogo oficial (FR-5/FR-6). É a **landing do 1º acesso** do professor sem vínculos (4.1.1) e a tela de edição acessível a qualquer momento pelo Perfil.
- **Elementos**: grid de seleção do catálogo (cards/toggles, busca por nome); contador "X de 4"; tentativa de 5ª selecionada é recusada com toast laranja + estado não-selecionado + **reforço no servidor** (400/409); botões "Salvar vínculos" (primário amarelo) e "Agora não" (secundário, visível **apenas em modo onboarding**).
- **Copy de onboarding** (1º login sem instituições): "Vincule as instituições onde você atua — pode ser feito depois." + nota "Você pode selecionar entre 0 e 4 instituições."
- **Estados**:
  - *Loading* → skeletons do grid.
  - *Vazio* (catálogo sem instituições) → copy "O catálogo de instituições está sendo organizado. Volte mais tarde." + acesso ao Perfil.
  - *Erro* → toast laranja `#D97706` com mensagem do envelope `{"codigo","mensagem"}`.
  - *Sucesso* (vinculação salva) → feedback verde + redireciona para a Fila de Aprovação quando origem era onboarding.
- **Validação**: mínimo 0; máximo 4 (contador ativo); desabilitar itens não-selecionados ao atingir 4.

#### 4.4.2 Fila de Aprovação (`area-professor/fila`)
- **Propósito**: visualizar e julgar projetos das instituições selecionadas (FR-10/FR-11). **Qualquer professor vinculado à instituição do projeto pode aprovar/rejeitar** (D-4); o professor com e-mail igual a `emailProfessorResponsavel` aparece **destacado**, mas a ação não é exclusiva.
- **Elementos**: lista de `CardProjeto` em `AGUARDANDO_APROVACAO`; projetos em que o professor é o `emailProfessorResponsavel` são marcados com badge "Você é o responsável". Filtro por instituição (select do catálogo das vinculadas).
- **Estados**:
  - *Loading* → skeletons.
  - *Vazio sem vínculos* → copy de onboarding "Você ainda não vinculou instituições." + CTA "Selecionar instituições" → `area-professor`.
  - *Vazio com vínculos* → "Nenhum projeto aguardando aprovação nas suas instituições."
  - *Erro* → toast laranja.
- **Aprovar**: botão "Aprovar" → confirma ação → estado `APROVADO` (feedback verde).
- **Rejeitar**: abre modal com `motivoRejeicao` **obrigatório** (textarea, validação inline laranja, botão "Rejeitar" desabilitado até preencher); confirmação muda para `REJEITADO` e fecha o modal.
- **Micro-interações**: após julgar um projeto, o card sai da fila com animação de sucesso/remoção; não recarrega a página.

### 4.5 Admin (dono do sistema, acesso via MSAL por e-mail semeado)

#### 4.5.1 Admin Main (`admin-main`)
- **Propósito**: ponto de entrada do admin com acesso ao cadastro do catálogo.
- **Elementos**: resumo acessível via lista de ações; nav "Instituições" → Cadastro.

#### 4.5.2 Cadastro de Instituição (`admin-cadastro-instituicao`)
- **Propósito**: CRUD do catálogo oficial (FR-7) — **CRUD simples no MVP; importação em lote fica pós-MVP**.
- **Elementos**: tabela do catálogo (nome/editar/remover) e formulário de adicionar. Sem papel admin → 403 (rota protegida por guard).

### 4.6 Perfil (`perfil`) — aluno e professor

- **Propósito**: todo usuário autenticado (aluno ou professor) visualiza seus dados vindos do MSAL e acessa o contexto do seu papel (D-2). É acessível pelo **menu do usuário no Header** ("Meu Perfil").
- **Elementos compartilhados (ambos os papéis)**:
  - Bloco de identidade: avatar (inicial/do MSAL), **nome completo**, **e-mail institucional** (pref. `preferred_username`), badge de **papel** (Aluno verde / Professor cinza-escuro).
  - Dados são somente leitura, provenientes da sessão MSAL (CPF/e-mail/telefone não solicitados).
- **Contexto do papel:**
  - **Aluno**: link de acesso a `meus-projetos` com contador de projetos; CTA "Novo Projeto".
  - **Professor**: seção "Minhas Instituições" com **grid das instituições vinculadas (0 a 4)**, edição inline/completa → `area-professor`, e nota "Você pode alterar a qualquer momento (0 a 4)."; link para `area-professor/fila`.
- **Estados**:
  - *Loading* → skeleton do bloco de identidade.
  - *Erro ao carregar contexto* → toast laranja + dados de identidade do token ainda exibidos.
  - *Vazio* → professor sem vínculos: copy "Você ainda não vinculou instituições." + CTA "Selecionar instituições".
- **Regra de rota**: exige autenticação; disponível para aluno, professor e admin.

---

## 5. Roteamento & Consolidação de Telas

| Rota | Tela | Papel | Estado |
| :--- | :--- | :--- | :--- |
| `/` | Catálogo Público (home) | Visitante | Especificada acima |
| `login-instituicao` | Login MSAL (entrar CTA) | Visitante | Especificada acima |
| `catalogo-publico` | Catálogo Público | Visitante | Especificada acima |
| `ver-projeto` | Ver Projeto | Visitante | Especificada acima |
| `meus-projetos` | Meus Projetos | Aluno | Especificada acima |
| `projeto-forms` | Upload de Projeto (Story 3.1) | Aluno | Especificada acima |
| `area-professor` | Seleção de Instituições / Onboarding | Professor | Especificada acima |
| `area-professor/fila` | Fila de Aprovação | Professor | Especificada acima |
| `perfil` | Perfil (aluno e professor) | Autenticado | Especificada acima |
| `admin-main` | Admin Main | Admin | Especificada acima |
| `admin-cadastro-instituicao` | Cadastro de Instituição | Admin | Especificada acima |

*Rotas existentes hoje no código: `''`, `login-instituicao`, `admin-main`, `admin-cadastro-instituicao`, `projeto-forms`, `ver-projeto`. Rotas novas propostas conforme tabela: `catalogo-publico`, `meus-projetos`, `area-professor`, `area-professor/fila` e `perfil`. Telas de papéis com especificação acima devem ser **reconstruídas** sobre a especificação (o código atual não reflete a profundidade definida neste documento; o refactor que removeu telas antigas usa este DESIGN.md como fonte única).*

---

## 6. Artefatos de Referência

- Paleta e reset: `front-end/src/styles/paleta.css`, `front-end/src/styles/reset.css`.
- Story absorvida: `_bmad-output/specs/DESIGN-STORY-3.1-UPLOAD-PROJETO.md` (mantida como anexo/histórico, superada por este documento).