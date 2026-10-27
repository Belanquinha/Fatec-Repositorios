---
name: ARCHITECTURE-Fatec-Repositorios-Editor-TipTap-e-Midia
type: architecture-decision-record
purpose: build-substrate
altitude: feature
paradigm: layered (Controller-Service-Repository) REST API + Angular SPA + stateless JWT
scope: Substituição do Editor.js por TipTap, formato de conteúdo em HTML sanitizado e pipeline de imagens do projeto (capa + imagens inline na documentação)
status: final
created: 2026-10-20
updated: 2026-10-20
binds: [CAP-4, FR-8, FR-9, NFR-5, NFR-6]
sources:
  - ../prds/prd-Fatec-Repositorios-2026-09-16/prd.md (atributos do Projeto)
  - ../epics.md (Story 3.1 — postagem de projeto; Story 5.2 — detalhe do projeto)
  - ../../specs/spec-fatec-repositorios/SPEC.md (CAP-4, regra "sem imagensExtras")
  - ../../specs/spec-fatec-repositorios/SPEC.md (§Regras de domínio — NFR-5: sem estado RASCUNHO)
  - back-end/src/main/java/com/fatecrepository/service/FileStorageService.java (teto real de 10MB; magic-bytes)
  - back-end/src/main/java/com/fatecrepository/service/RedimensionadorDeAvatar.java (precedente de ImageIO no projeto)
  - back-end/src/main/java/com/fatecrepository/controller/UploadController.java (contrato POST /uploads)
  - back-end/src/main/java/com/fatecrepository/dto/request/ProjetoCreateRequest.java (conteudoEditorJs cru)
  - back-end/src/main/resources/application.yml (multipart 60MB)
  - front-end/nginx.conf (client_max_body_size 65m; proxy /uploads/)
  - front-end/src/app/features/upload-projeto/upload-projeto.ts (validação 10MB; upload Editor.js)
  - front-end/src/app/features/ver-projeto/ver-projeto.html (render de blocos Editor.js)
  - front-end/src/app/app.routes.ts (rota do formulário carregada de forma eager)
  - Spike manual 2026-10-20: paste de documentação .docx real — Editor.js reprovado, TipTap aprovado
companions: []
---

# Arquitetura — Editor TipTap e pipeline de mídia do projeto

## 1. Contexto e problema

### 1.1 O que muda e por quê

O formulário de projeto (`/projeto-forms`) usa Editor.js desde a Story 3.1. O critério de sucesso declarado pelo produto é:

> "a pessoa poder copiar a documentação .docx dela e colar no editor de texto"

O Editor.js **não atende esse critério**. Evidência:

- `codex-team/editor.js#729` — "Paste from Microsoft Word" aberta desde 2019, sem solução.
- `codex-team/editor.js#2909` — paste de Google Docs perde negrito/itálico/sublinhado; aberta em fev/2025 e atualizada em out/2025.
- `codex-team/editor.js#2350` — paste de Word insere quebras de linha aleatórias.
- **Spike manual de 2026-10-20** com documentação real: o Editor.js se saiu mal; o TipTap preservou a estrutura (títulos, listas, parágrafos e imagens) de forma aceitável.

Além disso, o pipeline de imagem atual tem **quatro limites divergentes** e nenhum deles é o limite de produto:

| Camada | Valor atual | Arquivo |
|---|---|---|
| nginx | `65m` | `front-end/nginx.conf:8` |
| Spring multipart | `60MB` | `back-end/src/main/resources/application.yml:49` |
| **Aplicação (teto real)** | **`10MB`** | `FileStorageService.java:39` |
| Front | `10MB` + payload `11MB` | `upload-projeto.ts:47,50` |

Subir o Spring para 60MB **não alterou o limite de upload**, porque o teto efetivo continua no `FileStorageService`. O comentário do front que cita `client_max_body_size 12m` (`upload-projeto.ts:49`) também está defasado.

### 1.2 Requisitos que passam a valer

1. A documentação aceita **texto e imagens intercalados** (`título → texto → imagem → texto → imagem…`). As imagens são **nós do documento**, não uma galeria paralela.
2. **Não existe `imagensExtras`** — a regra do `SPEC.md:57` permanece válida; muda apenas o editor e o formato do conteúdo.
3. O número de imagens tem **teto rígido de 18** e **piso recomendado de 5** (ver §9 — o "5" é média, não bloqueio).
4. A capa (etapa 1) e cada imagem inline têm limite de entrada de **5MB**, com compressão no cliente.
5. O sistema precisa continuar servindo o catálogo público com peso de página aceitável em rede de laboratório.

### 1.3 Fora de escopo

- Estado `RASCUNHO` no servidor (proibido por NFR-5 / `SPEC.md:56`).
- Galeria separada de imagens, álbum ou `imagensExtras`.
- Edição colaborativa em tempo real.
- Conversão de PDF/DOCX em anexo.
- Redimensionamento de avatar (já resolvido por `RedimensionadorDeAvatar`).

## 2. Decisões de arquitetura

### AD-ED-1 — TipTap substitui o Editor.js

**Decisão:** adotar TipTap (ProseMirror) como editor da documentação.

**Pacotes mínimos:** `@tiptap/core`, `@tiptap/starter-kit`, `@tiptap/extension-image`, `@tiptap/extension-link`, `@tiptap/extension-placeholder`. Nenhuma extensão além do necessário (Rule of Three).

**Trade-off:** +~100–150KB gzip no bundle do formulário e perda do modelo de blocos JSON do Editor.js. Em troca, o paste de `.docx` — o critério de sucesso do produto — funciona, e o render de leitura fica mais simples (HTML).

**Valor de negócio:** o aluno publica a documentação que já escreveu, em vez de reescrevê-la. Menos abandono no formulário de 4 seções.

### AD-ED-2 — Conteúdo armazenado como HTML sanitizado, não como JSON do TipTap

**Decisão:** persistir `editor.getHTML()`, sanitizado no servidor com **jsoup** no momento do `POST/PUT /projetos`.

**Contrato:**
- API/Java/TypeScript: `conteudoHtml`.
- Coluna física: **mantém** `conteudo_editor_js` via `@Column(name = "conteudo_editor_js")`, para não depender de renomeação de schema (`ddl-auto: update` não renomeia coluna).

**Por quê HTML e não JSON:**

| Critério | HTML sanitizado | JSON TipTap |
|---|---|---|
| Render no detalhe | `[innerHTML]` + sanitizador do Angular | precisa de `@tiptap/static-renderer` no cliente |
| Validação de imagens | `jsoup` conta e valida `src` | Jackson percorre a árvore |
| Export futuro (PDF/e-mail) | direto | exige TipTap em todo consumidor |
| Fidelidade | suficiente (schema do TipTap restringe) | total |

**Trade-off:** HTML é mais verboso e menos estruturado para manipulação programática. Em troca, existe **uma única fronteira de confiança** na escrita e o caminho de leitura não carrega o editor.

**Valor de negócio:** o conteúdo deixa de depender de uma biblioteca específica para ser lido — reduz risco de lock-in e de regressão no catálogo público.

### AD-ED-3 — Imagens são nós do documento; sem `imagensExtras`

**Decisão:** o limite de imagens conta os `<img>` **distintos** dentro do `conteudoHtml`.

- Teto rígido: **18** imagens distintas por projeto (servidor rejeita acima).
- Sem piso rígido: 0 é válido. O "5" é recomendação de UX (ver §9).
- Deduplicação por `src`: o limite protege armazenamento e peso de página, não a quantidade de ocorrências do mesmo arquivo.

**Trade-off:** não existe grid estruturado para moderação nem campo consultável de imagens. Em troca, o usuário tem a liberdade editorial que pediu, e não há dois caminhos de upload com limites diferentes.

**Valor de negócio:** uma única experiência de imagem, um único limite para explicar ao aluno, uma única superfície de validação.

### AD-ED-4 — Compressão no cliente + normalização no servidor

**Decisão:** o cliente comprime antes de enviar; o servidor **decodifica, valida e re-encoda**.

| Etapa | Regra |
|---|---|
| Cliente — capa e imagens inline | `createImageBitmap(file, { imageOrientation: 'from-image' })` + canvas → JPEG q0.82, lado maior 1600px |
| Cliente — validação | tipo em `image/png, image/jpeg, image/jpg, image/webp`; entrada ≤ 5MB (antes da compressão) |
| Servidor — validação | magic-bytes (já existe) + decode obrigatório; decode falhou → `400` |
| Servidor — saída | re-encode JPEG q0.82 (capa) / q0.80 (inline), lado maior 1600px; metadados EXIF descartados |
| Servidor — capa | gera também thumbnail 480px para o card do catálogo |
| Limite pós-processamento | rejeita se o arquivo resultante passar de 1MB |

**WebP:** `ImageIO` puro não lê WebP. Adicionar `com.twelvemonkeys.imageio:imageio-webp` (puro Java, sem binário nativo) para manter o `accept` atual.

**Por que re-encodar no servidor se o cliente já comprime:** o thumbnail da capa **já obriga** o servidor a decodificar a imagem. Depois do decode, o re-encode é praticamente gratuito — e é a única coisa que transforma o orçamento de bytes em **garantia**, não em melhor-esforço. Um `curl` ignora a compressão do cliente.

**Trade-off:** CPU de algumas dezenas de ms por imagem e uma dependência Java pequena. Em troca, o pior caso de armazenamento e de peso de página fica conhecido.

### AD-ED-5 — Teto de transporte e orçamento de bytes

**Decisão:**

| Camada | Valor | Observação |
|---|---|---|
| nginx | `client_max_body_size 15m` | atualizar o JSON do 413 (`nginx.conf:22`) |
| Spring multipart | `12MB` | mantém margem sobre o arquivo de 5MB |
| Guarda do front (`POST /projetos`) | **2MB** | payload é HTML com URLs, não binário |
| Imagem individual | 5MB de entrada / 1MB de saída | AD-ED-4 |

**Orçamento por projeto:**

| Cenário | Capa | 18 imagens inline | Total |
|---|---|---|---|
| Típico (~300KB/imagem) | ~300KB | ~5,4MB | **~5,7MB** |
| Pior caso permitido | 1MB | 18MB | **19MB** |
| Antes desta decisão (10MB cru) | 10MB | 180MB | **190MB** |

**Trade-off:** 5MB recusa fotos de celular muito grandes; a compressão no cliente torna isso raro. Em troca, 500 projetos passam de ~95GB para ~3GB.

**Valor de negócio:** custo de disco previsível e página de detalhe que abre em rede de laboratório.

### AD-ED-6 — Cache imutável para `/uploads/`

**Decisão:** `add_header Cache-Control "public, max-age=31536000, immutable";` no `location ^~ /uploads/` (`nginx.conf:47`).

**Justificativa:** os nomes são UUID e nunca são sobrescritos; cache imutável é seguro.

**Trade-off:** praticamente nenhum — o conteúdo de uma URL nunca muda por design.

**Valor de negócio:** sem esse header, cada visita ao detalhe baixa novamente 5–6MB de imagens.

### AD-ED-7 — Formulário em rota lazy e remoção do Editor.js

**Decisão:** `app.routes.ts` passa `projeto-forms` para `loadComponent`; remover as dependências `@editorjs/*` após a migração.

**Justificativa:** o catálogo público é a rota mais visitada e não usa editor. TipTap/ProseMirror não devem entrar no bundle inicial.

**Trade-off:** um chunk adicional e um carregamento sob demanda na primeira abertura do formulário.

**Valor de negócio:** visitantes (incluindo não autenticados) não pagam o custo do editor.

### AD-ED-8 — Autosave local, sem estado `RASCUNHO`

**Decisão:** autosave em **IndexedDB**, com debounce de ~1s, chave `rascunho-projeto:<email>`.

- Conteúdo salvo: HTML do TipTap + campos das 4 seções + `versao` do schema do rascunho.
- Restauração: ao abrir o formulário, oferecer "continuar rascunho" ou "começar novo".
- Limpeza: ao publicar com sucesso, ao descartar explicitamente e no logout.
- Rascunhos de `versao` desconhecida são descartados, nunca restaurados.

**Trade-off:** por dispositivo (sem multi-PC), some ao limpar o cache e exige cuidado em máquina compartilhada de laboratório (chave por usuário + limpeza no logout). Em troca, **não** altera a máquina de estados nem a fila do professor.

**Valor de negócio:** menos retrabalho e menos abandono. NFR-5 permanece intacto.

### AD-ED-9 — Faxineiro de arquivos órfãos com carência de 7 dias

**Decisão:** job diário que remove arquivos de `/uploads` **não referenciados** por nenhum projeto (capa ou `conteudoHtml`) e com mtime superior a **7 dias**.

**Trade-off:** uma varredura periódica e uma consulta que precisa considerar as duas fontes de referência (capa e HTML). Carência curta apagaria imagens de rascunhos locais antigos.

**Valor de negócio:** o volume não enche com uploads abandonados.

### AD-ED-10 — Limites anunciados pelo servidor (recomendado, não bloqueante)

**Decisão:** expor `GET /uploads/policy` com os limites vigentes e consumir no formulário, em vez de espelhar constantes em 4 arquivos.

**Status:** recomendado; pode ficar para depois da migração. Enquanto não existir, os valores de AD-ED-4/5 são a fonte da verdade e devem ter teste de paridade.

## 3. Contratos

### 3.1 API de projeto

`ProjetoCreateRequest` / `ProjetoResponse` passam a usar `conteudoHtml` (string, HTML sanitizado). `POST /projetos`:

1. Valida o DTO (Bean Validation).
2. Sanitiza `conteudoHtml` com jsoup.
3. Conta imagens distintas; rejeita `> 18` com `400` e mensagem acionável.
4. Valida cada `src` de imagem.
5. Persiste o HTML sanitizado.

O endpoint `POST /uploads` **não muda** de contrato: multipart com campo `file`, resposta `{ url, nomeArquivo }`.

### 3.2 Regras de sanitização (jsoup Safelist)

**Tags permitidas:** `p, h2, h3, h4, strong, em, s, code, pre, blockquote, ul, ol, li, a, img, br, hr, figure, figcaption`.

**Atributos permitidos:**
- `a`: `href`, `target`, `rel` — protocolos `http`/`https`; forçar `rel="noopener noreferrer"`.
- `img`: `src`, `alt`, `title`.
- Demais: nenhum.

**Removidos:** `script`, `iframe`, `object`, `embed`, `style` (tag e atributo), `form`, `input`, e todo atributo `on*`.

**Regras específicas de imagem:**
- `src` deve casar com `^/uploads/[0-9a-fA-F-]+\.(png|jpg|jpeg|webp)$`.
- `data:` e URL externa são rejeitados (o primeiro infla o payload; o segundo é tracking pixel).
- Na escrita, adicionar `loading="lazy"` e `decoding="async"`.

**Por que na escrita e não na leitura:** hoje o backend grava o conteúdo cru (`ProjetoCreateRequest:28`) e a proteção depende só do sanitizador do Angular. Sanitizar na escrita cria uma fronteira única para qualquer consumidor futuro (export, e-mail, PDF).

### 3.3 Modelo

```java
@Column(name = "conteudo_editor_js", columnDefinition = "TEXT")
private String conteudoHtml;
```

A coluna física mantém o nome antigo. O nome lógico passa a refletir o conteúdo.

### 3.4 Leitura

- `ver-projeto` substitui o renderizador de blocos (`ver-projeto.html:131-177`) por um único container com `[innerHTML]`.
- Em **dev**, reescrever `src="/uploads/..."` para `environment.apiUrl` antes do bind (mesmo padrão de `ver-projeto.ts:133`), porque não há proxy de `/uploads` no `ng serve`.
- Em **produção**, o nginx já proxia `/uploads/` e o caminho relativo funciona.
- Nunca usar `bypassSecurityTrustHtml`. O sanitizador do Angular é a segunda linha de defesa.

## 4. Fluxos

### 4.1 Submissão

1. Valida seções 1, 2 e 4 no cliente.
2. Aguarda o `save`/`getHTML` do editor.
3. Comprime e sobe capa e imagens pendentes em `POST /uploads` (máx. 2 em paralelo).
4. Substitui os `src` temporários pelas URLs retornadas.
5. Valida o payload (≤ 2MB, sem `data:image`).
6. `POST /projetos` → sanitização e contagem no servidor.
7. Limpa o autosave local.

### 4.2 Paste de `.docx` (o caso crítico)

O Word envia imagens no clipboard como **arquivos** e como HTML com **base64**. O editor deve:

1. Interceptar `handlePaste`/`handleDrop`.
2. Extrair `File` objects de `clipboardData.files`/`items`.
3. Subir cada imagem em `POST /uploads`.
4. Substituir os `src` base64 pelas URLs retornadas.
5. Nunca inserir `data:` no documento (`Image.configure({ allowBase64: false })`).

**Sem esse passo, uma documentação de 5 páginas com screenshots vira um payload de dezenas de MB** — o mesmo 413 que já foi corrigido no Editor.js (`upload-projeto.ts:788`) volta pela porta do paste.

### 4.3 Restauração de rascunho

1. `ngOnInit` lê `rascunho-projeto:<email>` do IndexedDB.
2. Se existir e `versao` for conhecida, oferece continuar.
3. Ao continuar, restaura campos e chama `editor.commands.setContent(html)`.
4. O autosave roda em `onUpdate` do TipTap + `ngModelChange` das seções, com debounce de 1s.

## 5. Migração

**Banco:** pode e será resetado — **não há script de migração de dados**. O `ddl-auto: update` mantém a coluna existente.

**Seed:** existe **1 projeto** em `back-end/src/main/resources/seeds/projetos.json`, com `conteudoEditorJs` em JSON de blocos. Converter para HTML os tipos presentes: `header`, `paragraph`, `list`, `code`, `quote`, `image`, `delimiter`. Atualizar a chave do JSON para `conteudoHtml`.

**Rollout:** como o banco será resetado, não é necessário caminho de leitura duplo (blocos + HTML). O renderizador de blocos pode ser removido no mesmo PR.

## 6. Riscos e mitigações

| Risco | Impacto | Mitigação |
|---|---|---|
| Paste de Word insere base64 | payload explode / 413 | interceptar paste/drop + `allowBase64:false` + rejeição de `data:` no servidor |
| HTML com `<script>`/`on*` | XSS armazenado | jsoup Safelist na escrita + sanitizador do Angular na leitura |
| `src` externo/tracking pixel | privacidade + peso | regex de `src` restrita a `/uploads/` |
| Imagem rotacionada (EXIF) | UX ruim | `imageOrientation:'from-image'` no cliente + decode no servidor |
| WebP não decodifica | 400 indevido | TwelveMonkeys `imageio-webp` |
| Rascunho local vaza em PC compartilhado | privacidade | chave por usuário + limpeza no logout |
| Faxineiro apaga imagem de rascunho | imagem quebrada | carência de 7 dias |
| Limites divergentes voltam | mensagens contraditórias | teste de paridade ou `GET /uploads/policy` (AD-ED-10) |
| Regressão no catálogo público | página lenta | rota lazy + `loading="lazy"` + cache imutável + thumb da capa |

## 7. Definition of Done

- [ ] TipTap configurado com StarterKit + Image + Link + Placeholder; `allowBase64: false`.
- [ ] Paste/drop de imagem sobe para `/uploads` e insere URL.
- [ ] `@editorjs/*` removido do `package.json`.
- [ ] `projeto-forms` carregado via `loadComponent`.
- [ ] Backend sanitiza `conteudoHtml` com jsoup e aplica o Safelist de §3.2.
- [ ] Backend rejeita `> 18` imagens distintas e `src` fora de `/uploads/`.
- [ ] Capa: 5MB de entrada, 1600px/q0.82 na saída, thumb 480px gerado.
- [ ] Imagens inline: 5MB de entrada, 1600px/q0.80, `loading="lazy"`.
- [ ] Spring `12MB` + nginx `15m` + JSON do 413 atualizado.
- [ ] `Cache-Control: immutable` em `/uploads/`.
- [ ] Autosave em IndexedDB com versão, restauração e limpeza.
- [ ] Seed convertido para HTML (1 projeto).
- [ ] Renderizador de blocos removido.
- [ ] Testes: sanitização (script/onerror), limite 18/19, 5MB/6MB, paste com imagem, restauração de rascunho.

## 8. Notas

### 8.1 O "5" não é mínimo obrigatório

O produto estipulou **5 como média esperada**, não como regra de publicação. Portanto:

- **18** é teto rígido, validado no servidor.
- **0 a 4** imagens são permitidas; o formulário apenas exibe um aviso suave ("recomendado 5 ou mais") e o contador `X de 18`.
- Nenhuma story deve bloquear a publicação por falta de imagens.

### 8.2 Por que não `imagensExtras`

O pedido original de "escolher entre texto e imagem no texto" é satisfeito por **nós de imagem dentro do documento**. Uma galeria separada criaria dois caminhos de upload, dois limites e a dúvida "essa imagem conta?". A regra do `SPEC.md:57` permanece: imagens complementares vivem dentro do conteúdo; `imagensExtras` não existe.

### 8.3 Decisões adiadas

- `GET /uploads/policy` (AD-ED-10).
- Faxineiro de órfãos (AD-ED-9) — pode entrar em story separada.
- Download/export em PDF do projeto.

## 9. Referências

- `back-end/src/main/java/com/fatecrepository/service/FileStorageService.java`
- `back-end/src/main/java/com/fatecrepository/service/RedimensionadorDeAvatar.java`
- `back-end/src/main/java/com/fatecrepository/controller/UploadController.java`
- `back-end/src/main/resources/application.yml`
- `front-end/nginx.conf`
- `front-end/src/app/features/upload-projeto/upload-projeto.ts`
- `front-end/src/app/features/ver-projeto/ver-projeto.html`
- `front-end/src/app/app.routes.ts`
- `../../specs/spec-fatec-repositorios/SPEC.md`
- `../epics.md` (Story 3.1, Story 5.2)
