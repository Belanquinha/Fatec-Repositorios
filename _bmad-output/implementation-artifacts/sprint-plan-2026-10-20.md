# Sprint Plan — Fatec-Repositorios (2026-10-20)

> Comando: `bmad-sprint-planning`. Escopo: decomposição dos Épicos 7–8 em
> `_bmad-output/planning-artifacts/epics.md` e tracking em
> `_bmad-output/implementation-artifacts/sprint-status.yaml`.

- **Sprint goal:** trocar o Editor.js por TipTap com HTML sanitizado e fechar a
  fundação do pipeline de mídia (5MB→1MB, capa obrigatória, 18 imagens inline),
  sem regredir o catálogo público.
- **Data:** 2026-10-20
- **Estado de partida:** Épicos 1–6 `done`; Épicos 7–8 `backlog` (15 stories,
  nenhuma com story file). Nenhuma alteração de código nesta rodada.

## 1. Prontidão

| Item | Estado | Evidência |
|---|---|---|
| Arquitetura TipTap/mídia | final | `ARCHITECTURE-EDITOR-TIPTAP-E-MIDIA.md` (AD-ED-1…10, §7 DoD) |
| Epics/stories 7–8 | prontos para criar story files | `epics.md`, 15 stories com AC Given/When/Then |
| Capa obrigatória | decidido | capa exigida no cliente e no servidor (`400`); fora da contagem de 18 |
| Tracking | sincronizado | `sprint-status.yaml`: 8 epics, 29 stories, 8 retrospectives |
| Decisão pendente | bloqueia ST-8.1 | forma de sinalizar capa vs inline no `POST /uploads` (default proposto: `finalidade=capa|inline`) |
| Banco | reset obrigatório no rollout | sem script de migração e sem leitura dupla (arquitetura §5) |

## 2. Escopo proposto

Sem capacidade informada, o plano assume **uma sprint de fundação (ondas 0–1)**
e trata a onda 2 como stretch. Ajustar antes de criar os story files.

### Must — fecha a sprint

| Story | Entrega | Depende de |
|---|---|---|
| `ST-8.1` | Upload normalizado: 5MB entrada, JPEG 1600px/q0.82 capa e q0.80 inline, thumb 480px, saída ≤1MB, WebP, decode obrigatório; capa obrigatória e fora da contagem | decisão `finalidade` |
| `ST-8.2` | nginx `15m`, Spring `12MB`, guarda do front em 5MB/2MB, JSON do 413 e teste de paridade | ST-8.1 |
| `ST-8.3` | `Cache-Control: public, max-age=31536000, immutable` em `/uploads/` | ST-8.1 |
| `ST-7.1` | Backend `conteudoHtml` com jsoup, Safelist, `rel`, `loading/decoding`; coluna física legada | — |
| `ST-7.2` | Teto de 18 `<img>` distintos, allowlist `/uploads/<uuid>.<ext>`, rejeição de `data:`/externo | ST-7.1 |

### Stretch — se a onda 0–1 fluir

| Story | Entrega | Depende de |
|---|---|---|
| `ST-7.3` | Editor TipTap, toolbar, `getHTML()`, `allowBase64:false`, remoção de `@editorjs/*` | ST-7.1, ST-8.1 |
| `ST-7.5` | Compressão no cliente, capa obrigatória, contador `X de 18` e aviso 0–4 | ST-7.3 |

### Fora desta sprint

`ST-7.4` (paste de `.docx`), `ST-7.6` (render do detalhe), `ST-7.7` (autosave),
`ST-7.8` (seed HTML), `ST-7.9` (rota lazy), `ST-8.6` (rollout/regressão) e as
P2 `ST-8.4`/`ST-8.5`. Elas formam as ondas 3–6 do `epics.md`.

## 3. Ordem de execução

1. **Decisão:** validar `finalidade=capa|inline` + thumb `<uuid>-thumb.jpg`
   (aditiva; mantém `file`, `url`, `nomeArquivo`).
2. **Paralelo:** `ST-8.1`, `ST-8.2`, `ST-8.3` e `ST-7.1`.
3. **Sequencial:** `ST-7.2` depois de `ST-7.1`.
4. **Stretch:** `ST-7.3` e `ST-7.5` depois de `ST-7.1` + `ST-8.1`.

`ST-8.2` depende dos valores de `ST-8.1`; `ST-8.3` pode ser feito assim que o
nome UUID/thumb estiver estável. `ST-7.2` não depende do editor.

## 4. Riscos e bloqueios

1. **Contrato do upload de capa:** sem a decisão, `ST-8.1` não começa. O default
   do `epics.md` é aditivo e não quebra clientes existentes.
2. **Capa obrigatória:** o DTO atual aceita `imagemCapaUrl` nulo; `ST-3.1`,
   `ST-8.1` e `ST-7.5` exigem validação no cliente e `400` no servidor. Testar
   publicação sem capa.
3. **Decompression bomb:** `ST-8.1` recusa lado >12.000px ou >40MP antes do
   decode completo.
4. **Paridade de limites:** sem `GET /uploads/policy` (P2), o teste de `ST-8.2`
   é o gate contra a volta dos quatro valores divergentes.
5. **Reset do banco:** `ST-7.8` e `ST-8.6` exigem janela de reset; não construir
   leitura dupla nem script de migração.
6. **PC compartilhado:** o autosave (`ST-7.7`) fica para a onda 4; a chave por
   e-mail e a limpeza no logout são requisitos, não opcionais.

## 5. Definition of Done (sprint)

- [ ] `ST-8.1`, `ST-8.2`, `ST-8.3`, `ST-7.1` e `ST-7.2` em `review` (ou `done`
      após code review).
- [ ] Testes de sanitização, 18/19, `src` inválido, 5MB/5,1MB, decode, thumb e
      paridade de limites verdes.
- [ ] `mvn test`, `npm test`, `npm run build`, `docker compose config` e
      `nginx -t` verdes.
- [ ] Capa obrigatória coberta por teste de cliente e de servidor; capa fora da
      contagem de 18.
- [ ] `sprint-status.yaml` atualizado sem downgrade dos Épicos 1–6.
- [ ] Story files criados para as stories puxadas (`bmad-create-story`).

## 6. Handoff

- **Dono da decisão `finalidade`:** usuário/arquiteto.
- **Primeiro passo:** criar os story files de `ST-8.1` e `ST-7.1` e rodar em
  paralelo; `ST-8.2`/`ST-8.3`/`ST-7.2` entram na sequência.
- **Fim da sprint:** `bmad-code-review` nas frentes concluídas, atualizar
  `sprint-status.yaml` e replanejar as ondas 2–3.
- **Não reabrir:** Épicos 1–6 (`done`), spines de UX e contrato de vínculo.
