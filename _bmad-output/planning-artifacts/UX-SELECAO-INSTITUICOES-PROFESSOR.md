# UX — Seleção de Instituições do Professor (0–4 vínculos)

> Artefato `bmad-ux` (Sally · UX Designer) — escopo: **como o professor seleciona suas instituições**.
> Fonte única de design: `_bmad-output/planning-artifacts/DESIGN.md` + `EXPERIENCE.md` (status final, 2026-09-29).
> Este arquivo **consolida** o recorte do professor; em conflito, os dois spines vencem.
> Mock de composição: `mockups/institution-autocomplete.html` (Frame B · mode=multi).

Atualizado: 2026-10-07 · Sem mudança de contrato — apenas consolidação do recorte.

## 1. Resposta direta

**Sim — há documentos, e a decisão vigente é única:**

- O professor **NÃO tem página/rota própria** de seleção. A rota `/selecionar-instituicao` foi **removida do produto** (reversão de decisão 2026-09-29, DECISÃO 5-REV).
- A seleção vive **exclusivamente na aba "Instituições (0–4)" do Perfil** (`/perfil`, hub autenticado, padrão página de canal do YouTube).
- O controle é o **`institution-autocomplete` em `mode=multi`** — o mesmo componente do formulário do aluno (`mode=single`) e do filtro da Home (`mode=filter`).

Documentos que descrevem o fluxo (todos convergentes após a correção de curso 2026-09-23 + reversão 2026-09-29):

| Documento | O que diz sobre a seleção do professor |
|---|---|
| `planning-artifacts/EXPERIENCE.md` — IA, Component/State Patterns, Flow 3 (UJ-2) | Aba "Instituições (0–4)": combobox multi, chips, contador, Save, teto de 4, onboarding zerado |
| `planning-artifacts/DESIGN.md` — Brand, Components (`institution-autocomplete`, `combo-popup`, `combo-option`, `institution-chip`, `selection-counter`) | Visual canônico: cantos retos, `border #6A6A6C`, foco `accent-ink`, sem vermelho CPS |
| `planning-artifacts/mockups/institution-autocomplete.html` Frame B + `mockups/README.md` | Composição da aba: 3-de-4 + teto 4-de-4 recusado inline |
| `planning-artifacts/epics.md` — Story 2.2 / 2.3 (+ nota correção de curso) | Regras 0–4, sem texto livre, recusa da 5ª, alteração futura; superfície = aba do Perfil |
| `planning-artifacts/sprint-change-proposal-2026-09-23.md` §4.3 + Story 6.1 | "Seleção deixa de ser rota, vira aba"; landing: professor zerado → destaque "Instituições (0–4)" (não-bloqueante) |
| `specs/spec-fatec-repositorios/SPEC.md` CAP-3/CAP-8 + `roles.md` | 0–4 vínculos, catálogo fechado, sem cadastro livre; professor = `@cps.sp.gov.br` |
| `prds/prd-Fatec-Repositorios-2026-09-16/prd.md` FR-6/UJ-2 | Vincular 0–4, opcional no 1º login, 5ª recusada; fila = aba "Pendentes" |
| `planning-artifacts/.memlog.md` (entradas 2026-09-29) | Histórico: DECISÃO 5 (manter rota) → **anulada** pela DECISÃO 5-REV (remover rota, tudo na aba) |
| `DESIGN.md` (raiz do repo) §4.4.1 | **Desatualizado**: ainda descreve `area-professor` como rota — superado pelos spines de 2026-09-29 |
| `implementation-artifacts/sprint-plan.md` (P2 · story 2-3) | Superfície = aba "Instituições (0–4)" → depende de 6-1; ⚠️ endpoint professor↔instituição **não existe** no back-end |

## 2. Job do usuário (UJ-2 · Roberto, professor adjunto)

> "Como professor, quero vincular as unidades onde atuo (0–4), para julgar só os projetos das minhas instituições."

- **Landing:** 1º acesso com 0 vínculos → Perfil abre com a aba **"Instituições (0–4)" destacada** + onboarding inline. Não bloqueia as demais abas (FR-6).
- **Retorno com vínculos:** Perfil abre com a aba **"Pendentes"** pré-selecionada; a Home logada oferece atalho.
- **Nunca sai do Perfil** para vincular: sem rota, sem modal, sem "Passo 1 de 2".

## 3. Anatomia da aba "Instituições (0–4)" (`/perfil`)

1. **Título da aba:** "Vincule as unidades onde você atua" (nunca o título mecânico "Seleção de Instituições").
2. **Campo `institution-autocomplete` (`mode=multi`):** input único de consulta; **nada renderiza antes de digitar** (tipo-para-revelar); casamento por `nome`, `cidade`, `codigoUnidade`, `regiaoAdministrativa`, accent-insensitive.
3. **Popup (`combo-popup`):** `{surface}`, borda `1px {border}`, `elevation.raised`, teto `320px` com rolagem interna; linhas (`combo-option`) com logo 24px quadrado + título `1rem/700` + metadado `body-sm` ("Cód. 291 · São Paulo · Região Litoral"); ativa = `{primary-soft}` + aresta esquerda `2px {accent-ink}`; selecionada = `check` em `{accent-ink}` + `aria-selected`. Máx **20 linhas** + "… e mais N unidades. Refine a busca."
4. **Fileira de chips (`institution-chip`):** cada confirmação (`Enter`/clique) **acrescenta um chip e reabre a consulta**. Remoção = estado local imediato (sem chamada ao servidor) até o Save.
5. **Contador (`selection-counter`):** "X de 4 unidades" por extenso (nunca "X/4" seco). No teto ("4 de 4 unidades") vira `{accent-ink}`.
6. **CTA primária "Salvar vínculos"** (`button-primary` amarelo `{primary}`) + "Cancelar" secundário. Sair/trocar de aba com alterações não salvas → pede confirmação.
7. **Estado zerado (onboarding inline):** "Sem instituições vinculadas ainda — escolha até 4 unidades." + contador "0 de 4". Mínimo 0 permitido (CAP-3).

## 4. Visual (tokens de `DESIGN.md` — sem inventar nada)

- Geometria: `border-radius: 0` em tudo (exceção `full` só para foto de perfil).
- Campo/chip/popup: borda `1px {colors.border}` (`#6A6A6C`, 5.35:1). `border-soft` (2.83:1) **nunca** como fronteira de controle.
- Foco/aresta ativa/check/aba ativa/contador no teto: `{colors.accent-ink}` (`#8A6802`, 5.13:1). `primary #DCA703` (2.18:1) é **preenchimento, nunca traço**.
- **Vermelho CPS (`#B20000`) proibido** em recusa, teto, "não encontrado" ou erro — é só marca institucional.
- Erro de sistema = **neutro**: borda `2px {colors.error-border}` (`#202124`) + mensagem em `{colors.error-ink}` sobre `{colors.error-surface}` (`#F0F0F0`).

## 5. Estados (comportamento — `EXPERIENCE.md` State Patterns)

| Estado | Tratamento |
|---|---|
| Consulta vazia | **Sem popup.** Texto de ajuda: "Buscamos no catálogo oficial do Centro Paula Souza (86 unidades em operação)." Foco/clique/`Alt+Down` não abrem. |
| Carregando catálogo | Skeleton rows **dentro** do popup; campo continua digitável. |
| Catálogo indisponível | Popup: "Catálogo indisponível no momento. [Tentar novamente]" (`button-secondary`); mantém a consulta. Sem vermelho, sem lista vazia muda. |
| Sem correspondência | Empty state **dentro** do popup: "Nada encontrado por nome, cidade, código ou região." + "Limpar busca" (cinza). |
| Teto atingido (5ª) | Recusa **inline**: "Máximo de 4 instituições por professor" (AD-4) + contador em `accent-ink`; popup continua aberto; sem modal, sem vermelho. Troca = remover + adicionar. |
| Alterações não salvas | Save habilitado + indicador "alterações não salvas"; sair/trocar de aba pede confirmação. |
| Erro ao salvar | Painel neutro **acima** do conteúdo da aba, **mantendo chips e contagem**, Save reabilitado. Nada some sozinho. |
| Unidade fora do catálogo | Beco sem saída honesto (cinza + campos buscados + contato de suporte genérico). Sem texto livre, sem auto-cadastro. |
| Salvar 0 vínculos no 1º acesso | Permitido (FR-6); onboarding reapresentado até vincular. |

## 6. Teclado + acessibilidade (piso WCAG 2.2 AA)

- Combobox ARIA 1.2: `role=combobox` + `aria-expanded`/`aria-controls`/`aria-autocomplete=list`; popup `role=listbox`; opções `role=option` + `aria-selected`; `aria-activedescendant` (foco **nunca sai do input**); `aria-describedby` → ajuda + mensagem; `aria-required` onde aplicável.
- Teclas: `Down/Up` abre e move; `Home/End`; `Enter` confirma e fecha; `Esc` fecha mantendo consulta, 2º `Esc` limpa; `Tab` confirma e sai (nunca descarta opção realçada); fecha por clique fora sem corrida de `setTimeout`.
- Anúncios `aria-live=polite`: contagem do catálogo ("12 unidades para 'ipir'" — número **real**, não linhas renderizadas); recusa do teto anunciada (nunca só cor).
- Chips focáveis, `aria-label` na remoção ("Remover Fatec Ipiranga"); abas do Perfil como `tablist/tab/tabpanel` com `←/→/Home/End`; foco visível anel `2px accent-ink`; toque ≥ 40×40px; `prefers-reduced-motion` respeitado.

## 7. Responsivo

- `≥lg`: popup na largura do campo; chips em linha; contador à direita.
- `md`: chips quebram em 2 linhas; contador visível.
- `<md`: popup **100% da largura do campo**, ancorado, sem overflow; chips quebram livremente; abas com scroll horizontal; sticky-footer nunca coberto pelo popup (z-index popup > footer).

## 8. Microcopy (pt-BR institucional, verbo na frente)

- "Vincule as unidades onde você atua" · "2 de 4 unidades" · "Máximo de 4 instituições por professor" · "Sem instituições vinculadas ainda — escolha até 4 unidades" · "Buscamos no catálogo oficial do Centro Paula Souza (86 unidades em operação)." · "Sua unidade não está no catálogo oficial." · "Salvar vínculos".
- Banidos: "Seleção de Instituições" (título mecânico), "2/4" seco, "Limite excedido — erro", "Instituição inválida", qualquer vermelho/alarme.

## 9. Fluxo completo (Flow 3 condensado)

1. Roberto loga (`@cps.sp.gov.br`) → cai no **Perfil**, aba "Instituições (0–4)" destacada, onboarding inline, "0 de 4".
2. Digita "ipe" → popup filtra → `Enter` → chip 1 + contador "1 de 4", consulta reabre. Repete até N (≤4). **Sem sair do Perfil.**
3. Clica **"Salvar vínculos"** → aba "Pendentes" vira landing dos próximos acessos; fila lista `AGUARDANDO_APROVACAO` das suas unidades (responsável destacado).
4. Tenta a 5ª → recusa inline + "4 de 4 unidades" em `accent-ink`; remove uma, adiciona outra.
5. Falha no Save → painel neutro, chips preservados, tenta de novo.
6. Salva 0 → sem bloqueio; onboarding persiste.

## 10. Mapeamento de aceite (para `bmad-correct-course` / dev)

- **Story 2.2** (AC atual "vejo a lista do catálogo") → **invalidado** pelo tipo-para-revelar: trocar por "digito e o catálogo filtra; sem texto livre".
- **Story 2.3** (superfície "Seleção de Instituições") → apontar para a **aba "Instituições (0–4)"** com contador + Save; 5ª recusada inline + 400/409 no servidor.
- **Story 6.1** → já correta (sem rotas paralelas); nenhuma ação.
- ⚠️ **Gap bloqueante:** endpoint de persistência professor↔instituição **não existe** (sem controller no back-end) — comportamento especificado, implementação pendente da Story 2.3.
- Gaps não-bloqueantes: foco do dialog de aprovação, `aria-live` duplicado Home×catálogo, typeahead de e-mail ainda handmade (dono: Story 3.2).

## 11. O que foi / não foi alterado nesta rodada

- **Não alterado:** `_bmad-output/planning-artifacts/DESIGN.md` e `EXPERIENCE.md` (status final 2026-09-29) — já contêm a especificação completa; este arquivo é consolidação do recorte, não segunda fonte.
- **Divergência conhecida:** `DESIGN.md` da raiz (§4.4.1 `area-professor`) está superado pelos spines — alinhar em rodada futura, não aqui.
- Referência de composição: `mockups/institution-autocomplete.html` Frame B.
