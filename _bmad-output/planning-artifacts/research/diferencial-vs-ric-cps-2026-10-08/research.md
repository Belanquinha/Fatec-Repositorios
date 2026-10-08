---
title: 'deep-recon: Diferencial vs RIC-CPS — somos diferentes o suficiente para existir?'
type: 'competitive-differentiation'
topic: 'Nossa plataforma fará parte do CPS, não compete com o RIC-CPS, mas precisa provar que é diferente o suficiente do RIC para fazer sentido existir. Ideia: guardar projetos (ou ao menos sua fonte GitHub) da área de tecnologia como Jogos e ADS. Qual o diferencial? Eles já guardam isso ou não?'
decision: 'Definir posicionamento, naming/SEO e escopo para não ser "mais um repositório" e justificar existência dentro do CPS'
source: 'web (live 2026-10-08) + artefato prévio competitive-gap 2026-09-23'
status: complete
preset: 'deep (aprofundado)'
validation: 'normal + red-team interno'
created: '2026-10-08'
updated: '2026-10-08'
---

# Deep Recon: Nossa plataforma vs RIC-CPS — diferencial e razão de existir

**Pergunta do solicitante:** nossa plataforma competirá de certa forma com o RIC-CPS, fará parte do CPS, então não seria exatamente competição, mas preciso ter certeza que somos diferentes o suficiente do RIC para fazer sentido existirmos. No Google quando se pesquisa "repositório" aparece o RIC-CPS, então não podemos ser um repositório. Nossa ideia é guardar projetos ou ao menos uma de suas fontes (GitHub por exemplo) da área de tecnologia como Jogos e ADS — qual nosso diferencial? Eles já guardam essas informações ou não?

**Resposta curta:** sim, vocês são diferentes o suficiente — **desde que não se chamem nem se comportem como "repositório"**. E sim, o RIC **já guarda** projetos de ADS/Jogos/Software, mas guarda **de um jeito que não serve** ao seu caso de uso. O diferencial não está no *quê* (projetos de tecnologia), está no *como, quando, para quem e em que estado* (vivo vs morto).

- **RIC-CPS = arquivo morto, institucional, preservacional, pós-curso, curadoria bibliotecária, PDF + ZIP estático, só TCC final com nota ≥9 / B-MB em texto integral.**
- **Vocês = vitrine viva, pedagógica, contínua, curadoria docente pré-publicação, link GitHub vivo + demo + capa + Editor.js + perfis LinkedIn, foco empregabilidade/descoberta.**
- Posicionamento correto: **camada pré-RIC / complementar ao RIC**, não concorrente. Nunca usar a palavra "repositório" no nome, H1, title-tag ou pitch.

> Este artefato complementa (não substitui) o `competitive-gap-analise-competidores-ric-cps-zenodo-2026-09-23/research.md`. Aquele mapeou 5 lacunas técnicas vs RIC/Zenodo (arquivos, PID, metadados/OAI, acesso em camadas, métricas). Este responde à questão de existência/posicionamento.

## 1. Sumário executivo

1. **Eles já guardam software/jogos/ADS — comprovado.** A Portaria CEETEPS-GDS nº 4069/2024, Art. 5º VIII, lista explicitamente "jogos digitais, softwares, aplicativos e EULA" como *produto complementar* do TCC, submetido em "arquivo único zipado, máx. 10 GB" [3][4]. O Termo de Autorização (TA) padrão tem checkbox para isso [5]. Há itens reais no ar: SPPG (ADS Prudente, portfólio p/ profissionais de games) [6], Smart Pursuer IA em jogos (ADS Franca, Unity+C#) [7], Imersão 2D (Jogos Digitais Americana) [8], Lost Descent pixel-art (DS Etec Albert Einstein) [9], além de artigo ADS sobre PBL [10].
2. **Mas guardam mal para o seu job-to-be-done.** O ZIP é um anexo opaco ao PDF: sem preview, sem link clicável para GitHub, sem demo jogável, sem busca por stack/linguagem/engine, sem perfil do aluno, sem atualização após depósito. Descoberta é por autor/título/assunto/curso/data em DSpace JSPUI 6.x antigo [1][2][11] — UX bibliotecária, não vitrine.
3. **O filtro do RIC exclui a maioria do que vocês querem mostrar.** Só vai para texto integral o TCC Fatec com nota ≥9 + indicação da banca, ou Etec com B/MB; o resto é cadastrado como "publicação institucional" **sem texto completo** [2][3]. Ou seja: bons projetos de 1º–5º semestre, projetos interdisciplinares, game jams, iniciação, extensão — ficam de fora por definição. Vocês cobrem exatamente esse vazio.
4. **O fluxo do RIC é lento e mediado.** Depósito via orientador/coordenador → e-mail da biblioteca → catalogação por bibliotecário, até 90 dias após fim do semestre [3][12]. Vocês propõem fluxo direto aluno → professor responsável (aprovação pedagógica) → publicação imediata, com MSAL e fila de aprovação (cf. DESIGN.md). É curadoria *antes* (pedagógica) vs curadoria *depois* (arquivística).
5. **SEO confirma o risco:** "repositório Fatec/CPS" leva ao RIC (45.172 arquivos, domínio ric.cps.sp.gov.br bem indexado) [1]. Se vocês usarem "repositório" no nome/H1/title, perdem no Google e confundem o usuário e o gestor. Posicionem-se como **vitrine / catálogo / portfólio / showcase** — termos que o RIC nunca usa.

**Veredito:** faz sentido existir, como **produto-irmão complementar dentro do CPS**. Fórmula de pitch: *"Não somos um repositório. Somos a vitrine viva dos projetos de tecnologia das Fatecs — do primeiro semestre ao TCC. O RIC preserva o TCC final em PDF; nós mostramos o projeto funcionando, com código vivo no GitHub, para alunos, professores e empresas. Quando o TCC vai para o RIC, linkamos o handle de volta."*

## 2. O que o RIC-CPS é (fatos verificados hoje)

| Dimensão | RIC-CPS (verificado 2026-10-08) | Fonte |
|---|---|---|
| Natureza | Repositório institucional do CPS, gerido pelo Centro de Gestão Documental, instituído Portaria 3793/2023 (sucede 3013/2021) | [13][2] |
| Tecnologia | DSpace (doc. oficial cita 6.3 + JSPUI + Solr + Tomcat + Postgres); home no ar com comunidades CGESG/CGETEC/CGPEP, browse por data/autor/título/assunto/curso | [1][2][11] |
| Volume | **45.172 arquivos** na home hoje (era ~43–44k em 2026-09-23 — crescimento ativo) | [1] |
| Coleção atual | Essencialmente **TCC/TG** (primeira coleção; "vasto leque futuro" prometido desde 2021 mas TCC segue dominante) | [12] |
| Depósito | Obrigatório por portaria; via professor → e-mail biblioteca → bibliotecário cataloga; prazo 90 dias pós-semestre | [3][12] |
| Acesso integral | **Seletivo:** Fatec nota ≥9 + banca + TA; Etec B/MB + TA. Demais: só metadados/resumo | [3] |
| Identidade | Handle `123456789/...` por item; OAI-PMH 2.0 com 12 esquemas; vocabulário controlado Dedalus | prior recon + [11] |
| Formato produto software | PDF/A TCC (máx. 10 GB) + produto em **ZIP único (máx. 10 GB)** + TA PDF/A | [3][4] |

## 3. Eles já guardam ADS / Jogos / Software? Sim — provas

1. **Norma permite e prevê:** Art. 4º (formatos TCC: artigo, PDI, projeto técnico, portfólio/webfólio etc.) + Art. 5º VIII (produtos: jogos digitais, softwares, aplicativos e EULA) + Art. 6º (ZIP único 10 GB, naming `nomedocurso_ano_semestre_primeiroautor_titulo`) [3][4].
2. **TA tem checkbox:** "Jogos Digitais, Softwares, Aplicativos e EULA" — exemplo real TA 2025 Etec Dra. Ruth Cardoso, curso DS, projeto Yapure [5].
3. **Itens reais no ar:**
   - ADS Prudente 2023 — *Sistema de Portfólios para Profissionais de Games (SPPG)* — PDF integral no RIC [6].
   - ADS Franca 2022 — *Smart Pursuer: IA em Jogos Digitais* (Unity + C# + ML-Agents) — PDF integral [7].
   - Jogos Digitais Americana 2024 — *Imersão 2D* (relatório técnico) — handle 123456789/21711 [8].
   - Jogos/ADS Prudente 2024 — *Análise das fases de desenvolvimento de jogos + IA* (artigo) — handle 123456789/28778 [10b].
   - DS Etec Albert Einstein 2025 — *Lost Descent* (pixel-art, C# + Game Maker) — PDF integral [9].
4. **Limite do que foi achado:** todos são **PDFs descrevendo** o software/jogo. Nenhum registro examinado expõe **link GitHub clicável, demo, README renderizado, stack filtrável ou versionamento**. O produto, quando existe, é anexo ZIP opaco (conforme norma). Não há evidência de integração GitHub/ORCID no RIC (ao contrário do Zenodo) — ausência de evidência com alta plausibilidade, dado DSpace 6.3 JSPUI sem essas integrações nativas.

**Implicação:** a objeção "mas o RIC já faz isso" é respondida com: "guarda o *relato* em PDF + ZIP morto do TCC final nota 9+; não mostra o *projeto vivo* nem os 90% de projetos que não são TCC nota 9".

## 4. Mapa de diferenciação (onde vocês ganham)

| # | Dimensão | RIC-CPS | Fatec-Repositorios (proposto, cf. DESIGN.md) | Força do diferencial |
|---|---|---|---|---|
| 1 | **Metáfora de produto** | Repositório/arquivo (preservar) | **Vitrine/catálogo/portfólio/showcase** (mostrar, descobrir, contratar) | ★★★★★ — é o que justifica o nome e o SEO |
| 2 | **Estado do código** | ZIP estático anexado, congelado, sem diff | **Link GitHub/GitLab vivo** + campo URL validado; futuro: embed README, stars, linguagem via API | ★★★★★ — pedido explícito do usuário |
| 3 | **Momento no ciclo** | Só TCC/TG final, 90 dias depois | **Qualquer semestre**: PBL, inter-disciplinar, jam, extensão, esteira até o TCC | ★★★★★ — cobre o vazio da política ≥9/B-MB |
| 4 | **Curadoria** | Bibliotecária, pós-depósito, metadados/ABNT/TA | **Docente, pré-publicação** (fila aprovação, motivo rejeição, badge responsável) | ★★★★ — ativo único já no DESIGN.md |
| 5 | **Quem publica** | Professor/bibliotecário deposita pelo aluno | **Aluno publica direto** (Upload Projeto, Meus Projetos), professor aprova | ★★★★ — protagonismo discente |
| 6 | **Descoberta** | Por autor/título/assunto/curso/data (Dedalus) | **Por tecnologia, instituição, palavra-chave, capa visual, busca livre** (catálogo público + CardProjeto) | ★★★★ — job de recrutador/empresa |
| 7 | **Narrativa do projeto** | Resumo Dublin Core + PDF | **Editor.js rico** (headers, código, citações, imagens inline), capa, descrição 144c, integrantes+LinkedIn | ★★★★ — storytelling de portfólio |
| 8 | **Conexão mercado** | Inexistente (acesso aberto cidadão/pesquisador) | **Azul Mercado**: links repo, LinkedIn, perfis — ponte estágio/emprego | ★★★★ — valor CPS→empresa |
| 9 | **Identidade/persistência** | Handle + OAI-PMH (ponto forte deles) | Não tentar replicar agora; **linkar handle RIC** quando TCC for arquivado (complementaridade) | ★★★ — evita reinventar |
| 10 | **UX/tech** | DSpace JSPUI legado, responsividade/acessibilidade limitadas (literatura DSpace confirma) | **Web moderna, cantos retos, mobile-first, toasts, skeletons** (DESIGN.md) | ★★★ — hygiene, não pitch |

**O que NÃO é diferencial (não prometer):** preservação de longo prazo, DOI/handle próprio, OAI-PMH, vocabulário controlado, cobertura de todos os cursos/Etecs, acesso embargado sofisticado — tudo isso o RIC/Zenodo fazem melhor. Reconhecer isso fortalece a credibilidade interna no CPS.

## 5. Evidência contrária / red-team

- **"ZIP de 10 GB já é suficiente para software?"** Para preservação, sim; para descoberta/reuso/emprego, não. ZIP não é executável-descobrível, não tem histórico, não mostra stack, não atualiza. A literatura de portfólios GitHub-API (ex.: IFRO 2026, GitFolio) mostra que o valor está no **link vivo + metadados extraídos**, não no blob [14].
- **"E se o RIC modernizar para DSpace 7/8 Angular e abrir coleção de software?"** Possível (migrações DSpace 6→9 documentadas trazem UI melhor e ORCID/GitHub via plugins), mas não muda mandato (TCC final, nota de corte, fluxo bibliotecário) nem velocidade institucional. A defesa é **posicional (vitrine vs arquivo)**, não tecnológica — sobrevive a upgrade deles.
- **"E se disserem que é duplicação de esforço?"** Resposta com norma na mão: o RIC *por desenho* exclui do integral tudo abaixo de 9/B-MB e tudo que não é TCC. Vocês cobrem o excluído + adicionam camada viva. Propor **integração explícita** (campo `ricHandle` no projeto + link bidirecional + futura colheita OAI) transforma acusação de duplicação em elogio de sinergia.
- **Limite desta pesquisa:** não foi feita auditoria quantitativa (% de itens ADS/Jogos no RIC, % com ZIP anexo, % com URL GitHub no PDF). Recomendado como follow-up barato via OAI-PMH (`ListRecords`) + amostragem.

## 6. Recomendações acionáveis

### P0 — Naming e SEO (fazer agora, custo zero)
1. **Banir "repositório"** do nome, logo, `<title>`, H1, meta description, README e pitch. Usar **Vitrine / Catálogo / Portfólio / Showcase de Projetos de Tecnologia das Fatecs**. Sugestão de tagline: *"A vitrine viva dos projetos de tecnologia do CPS — do primeiro semestre ao TCC."*
2. **Página "Como somos diferentes do RIC-CPS"** pública (útil para gestor, professor e Google): tabela arquivo-morto vs vitrine-viva + link para ric.cps.sp.gov.br. Isso captura a busca comparativa e sinaliza complementaridade.
3. **SEO defensivo:** `robots/sitemap`, títulos por projeto (`{titulo} — {curso} {Fatec} | Vitrine CPS`), JSON-LD `CreativeWork/Code`, Open Graph com capa. Nunca competir pelo termo "repositório"; competir por "{curso} {fatec} projetos", "jogos digitais fatec", "ADS fatec portfólio".

### P1 — Produto (escopo que prova a diferença)
4. **Link de código vivo como cidadão de 1ª classe:** campo URL repo (GitHub/GitLab/Bitbucket) obrigatório-ou-opcional com validação, badge Azul Mercado, card com ícone; roadmap: fetch server-side de stars/linguagem/README via API GitHub (espelho do padrão IFRO/GitFolio [14]).
5. **Filtro por tecnologia/stack:** tags estruturadas (linguagem, engine — Unity/Godot/GameMaker —, framework) — busca que o RIC não tem. É o diferencial mais demonstrável em demo.
6. **Linha do tempo contínua:** permitir projetos de qualquer período + status (em andamento / concluído / TCC). Marcar explicitamente "projetos que o RIC não cobre".
7. **Campo `ricHandle` (URL handle) opcional** no projeto + botão "Ver no RIC-CPS" quando o TCC for arquivado. Custo mínimo, ganho político máximo (prova que vocês somam).
8. **Manter curadoria docente pré-publicação** como está no DESIGN.md (fila, motivo rejeição obrigatório) — é o ativo de governança pedagógica que o RIC não tem.

### P2 — Institucional (viabilidade dentro do CPS)
9. **Memorando de complementaridade** citando Portarias 3793/2023 e 4069/2024: vocês operam *antes e ao lado* do RIC (mostra), o RIC opera *depois* (preserva). Anexar este recon + o gap 2026-09-23.
10. **Métrica de sucesso distinta do RIC:** não contar "arquivos preservados" (eles têm 45k); contar "projetos vivos com repo ativo", "cliques GitHub/demo", "contatos LinkedIn", "projetos pré-TCC publicados". Se medirem igual, perdem.
11. **Piloto focado:** começar por ADS + Jogos Digitais de 2–3 Fatecs (os cursos com mais evidência de software no RIC). Evita espalhar e gera showcase denso.

### O que NÃO fazer
- Não prometer DOI/handle próprio, OAI-PMH ou preservação perpétua no MVP (é o jogo deles).
- Não importar PDFs do RIC em massa (vira espelho e confirma duplicação). Integração = link, não cópia.
- Não aceitar "só PDF sem repo" como projeto ideal — exigir ao menos URL ou demo para reforçar "vivo".

## 7. Perguntas em aberto (follow-up barato)
- Qual % dos TCCs ADS/Jogos no RIC tem ZIP anexo? Quantos citam GitHub no PDF? (amostra 50 itens via OAI + grep) — quantifica o gap "vivo".
- Há coleções não-TCC no RIC hoje (jogos como item independente) ou tudo é TCC? (browse por tipo/extensão).
- Gestores CPS aceitam o termo "vitrine" oficialmente? Validar naming com CGD/biblioteca para evitar atrito.
- Limite GitHub API / embed demo (itch.io, Vercel) — viabilidade técnica robo-roadmap.

## 8. Apêndice — fontes (tudo verificável)

| [n] | Afirmação suportada | Fonte | Acesso |
|---|---|---|---|
| [1] | RIC home: definição, 45.172 arquivos, DSpace, comunidades CGESG/CGETEC/CGPEP, browse | ric.cps.sp.gov.br/ | 2026-10-08 |
| [2] | Base DSpace, alimentação por bibliotecários, política ≥9/B-MB, DSpace 6.3 JSPUI/Solr | dados.cps.sp.gov.br — ficha RIC-CPS (PDA) | 2026-10-08 |
| [3] | Art. 4º–6º, 13–16: formatos TCC, produto complementar VIII (jogos/software/EULA), ZIP 10 GB, naming, TA, corte ≥9/B-MB, prazo 90 dias | Portaria CEETEPS-GDS 4069/2024 (PDF) | 2026-10-08 |
| [4] | Idem — espelho DGUI | dgui.cps.sp.gov.br — Portaria 4069/2024 | 2026-10-08 |
| [5] | TA com checkbox "Jogos Digitais, Softwares, Aplicativos e EULA" (ex. DS Etec Ruth Cardoso, Yapure, 2025) | Scribd — Termo Autorização RIC-CPS | 2026-10-08 |
| [6] | ADS Prudente 2023 — SPPG portfólio p/ games (PDF integral RIC) | ric.cps.sp.gov.br bitstream 123456789/17290 | 2026-10-08 |
| [7] | ADS Franca 2022 — Smart Pursuer IA em jogos (Unity, C#, ML-Agents) | ric.cps.sp.gov.br bitstream 123456789/11534 | 2026-10-08 |
| [8] | Jogos Digitais Americana 2024 — Imersão 2D (handle 21711) | ric.cps.sp.gov.br/handle/123456789/21711 | 2026-10-08 |
| [9] | DS Etec Albert Einstein 2025 — Lost Descent pixel-art (C#, Game Maker) | ric.cps.sp.gov.br bitstream 123456789/37462 | 2026-10-08 |
| [10] | ADS sob PBL — artigo periódicos CPS (handle 8561) | ric.cps.sp.gov.br/handle/123456789/8561 | 2026-10-08 |
| [10b] | Jogos+IA artigo ADS Prudente 2024 (handle 28778) | ric.cps.sp.gov.br/handle/123456789/28778 | 2026-10-08 |
| [11] | Sobre RIC, Portarias 3013/3014/3793, objetivo unificado, coleções | ric.cps.sp.gov.br/about | 2026-10-08 |
| [12] | 1ª coleção = TCC via bibliotecários; fluxo entrega; Portaria 3015/2021 | Memorando CEETEPSMEM202103548A_RICCPS (2021) | 2026-10-08 |
| [13] | Instituição RIC + Comitê Gestor (Portaria 3793/2023) | blob CPS / DGUI | 2026-10-08 |
| [14] | Padrão portfólio-vivo via GitHub API (IFRO 2026, GitFolio, portfólios Next.js) | repositorio.ifro.edu.br, github.com/lucianakyoko/gitfolio | 2026-10-08 |
| [15] | DSpace legado: JSPUI/XMLUI limitada vs Angular moderna; busca imprecisa padrão | PUC-Campinas migração 6.2→9.1; estudo DSpace 8 (Solr EdisMax) | 2026-10-08 |
| [prior] | 5 lacunas técnicas (arquivos, PID, OAI, camadas, métricas) — contexto | `_bmad-output/.../competitive-gap-...-2026-09-23/research.md` | 2026-09-23 |

## 9. Mapa de staleness
| Claim | Classe | Pub | Re-checar |
|---|---|---|---|
| RIC tem 45.172 arquivos / DSpace / comunidades | capability/snapshot | 2026-10-08 | 2027-01-08 |
| Portaria 4069/2024: ZIP software + corte ≥9/B-MB | norm | 2024-07-19 | sob mudança de portaria |
| ZIP é opaco / sem GitHub vivo no RIC | capability (ausência de evidência) | 2026-10-08 | 2027-01-08 (amostra OAI) |
| DSpace JSPUI limitado vs vitrine moderna | capability | 2026-10-08 | 2027-01-08 |

*Uso downstream:* alimentar PRD (seção diferenciação/posicionamento), naming, tagline SEO, memorando CPS e backlog (campo `ricHandle`, filtro stack, fetch GitHub). Não competir de frente; complementar por desenho.
