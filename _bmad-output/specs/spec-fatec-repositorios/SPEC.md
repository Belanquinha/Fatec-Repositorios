---
id: SPEC-fatec-repositorios
companions: [roles.md]
sources:
  - ../planning-artifacts/architecture/architecture-Fatec-Repositorios-2026-09-22-catalogo-instituicoes/ARCHITECTURE-CATALOGO-INSTITUICOES.md
---

> **Contrato canônico.** Este SPEC e os arquivos em `companions:` são o contrato completo, validado por preservação, do que construir, testar e validar. Os documentos-fonte listados no frontmatter existem para rastreabilidade — consulte-os apenas se precisar de narrativa ou contexto que este contrato omite propositalmente.

# Plataforma Fatec Repositorios (postagem e aprovação de projetos)

## Why

Aplicação web full-stack (front-end + back-end + docker, Angular + Spring + MSAL) para **alunos da Fatec/CPS postarem projetos feitos em sala e no curso**, com **professores aprovando os projetos** — visão para realizar (uma plataforma que o projeto de TCC quer fazer existir). A direção "minimizar ao máximo o trabalho da secretaria/gestor" (pedido do orientador) torna o fluxo de aprovação leve: sem gestor institucional, o professor é dono do processo.

## Capabilities

- **CAP-1** — Login via MSAL (tenant único da CPS)
  - **intent:** Aluno e professor de qualquer unidade Fatec que pertença ao tenant da CPS autenticam-se na plataforma via MSAL da Microsoft.
  - **success:** Uma conta CPS válida completa o login e acessa a aplicação; uma conta de outro tenant Microsoft é rejeitada no login.

- **CAP-2** — Classificação de papel por domínio de e-mail, centralizada no back-end
  - **intent:** O back-end classifica cada usuário autenticado como aluno ou professor a partir do domínio do UPN/preferred_username retornado pelo login MSAL.
  - **success:** Um teste que fornece um token com UPN terminando em `@aluno.cps.sp.gov.br` obtém acesso de aluno; um com `@cps.sp.gov.br` obtém acesso de professor; qualquer outro domínio do tenant CPS cai em aluno (fallback) — tudo decidido no servidor, sem depender de regra no front-end.

- **CAP-3** — Seleção de 0 a 4 instituições pelo professor a partir de catálogo oficial
  - **intent:** O professor escolhe de 0 até 4 instituições de uma lista oficial pré-populada (importada ou curada com dados oficiais) de todas as instituições Fatec/CPS. A seleção não é obrigatória no primeiro login e pode ser feita ou alterada a qualquer momento.
  - **success:** Um professor vincula entre 0 e 4 instituições do catálogo e nunca mais que 4; a seleção inicial é opcional (mínimo 0); não existe fluxo de cadastrar instituição em texto livre.

- **CAP-4** — Postagem de projeto com e-mail do professor responsável, instituição e integrantes
  - **intent:** O aluno, ao postar um projeto, informa o e-mail do professor responsável (autocomplete no front-end para professores do tenant CPS, **com aceite de e-mail livre — "Professor Convidado" — como registro informativo, sem envio de notificações no MVP**), vincula a instituição e registra a lista de integrantes do projeto. O projeto nasce diretamente no estado AGUARDANDO_APROVACAO (sem estado RASCUNHO).
  - **success:** Todo projeto postado é salvo diretamente como AGUARDANDO_APROVACAO com `emailProfessorResponsavel` persistido, `instituicaoId` vinculado e lista de integrantes associada. O e-mail do professor pode ser digitado livremente mesmo sem estar no tenant.

- **CAP-5** — Aprovação de projetos pelos professores vinculados
  - **intent:** O(s) professor(es) aprovam ou rejeitam os projetos vinculados à(s) sua(s) instituição(ões) selecionada(s). **Qualquer professor vinculado à instituição do projeto pode julgar** (decisão de elicitação 2026-09-22); o professor com `emailProfessorResponsavel` é apenas destacado na fila.
  - **success:** Uma ação de aprovação/rejeição de um professor vinculado à instituição do projeto muda o estado (APROVADO ou REJEITADO com `motivoRejeicao` obrigatório) e o novo estado fica visível de forma consistente ao aluno e na listagem.

- **CAP-6** — Entidade Integrante do Projeto
  - **intent:** Permitir que o projeto registre múltiplos integrantes com nome e link do LinkedIn (MVP sem `papelNoProjeto`; campo pode voltar quando o modelo de papéis for definido).
  - **success:** Cada integrante é persistido associado ao projeto (Projeto 1 ── N Integrante) com seu id (UUID), `nome` e `linkLinkedin`.

- **CAP-7** — Acesso público ao catálogo de projetos para visitantes
  - **intent:** Permitir que usuários não autenticados (visitantes) pesquisem e visualizem detalhes de projetos que estejam no estado APROVADO sem necessidade de login.
  - **success:** Um visitante não autenticado pesquisa projetos aprovados na busca/catálogo público e visualiza seus detalhes completos sem requerer autenticação ou token JWT.

- **CAP-8** — Perfil como hub do usuário autenticado e landing única pós-login
  - **intent:** Todo usuário autenticado aterrissa no **Perfil** (`/perfil`, rota protegida, privado — só o próprio vê) — um único hub com layout compartilhado entre ALUNO e PROFESSOR (padrão estrutural de página de canal: bloco de identidade + atalhos + listas de conteúdo dirigidas por papel). Identidade do MSAL (nome, e-mail institucional, papel) **somente leitura**; as superfícies avulsas "Meus Projetos", "Fila de Aprovação" e "Seleção de Instituições" **deixam de ser rotas de destino e viram abas** do hub. **Landing única (correção de curso 2026-09-23):** após o login, todo papel aterrissa no Perfil com aba/atalho pré-selecionado pelo estado do papel — aluno → aba "Meus projetos" (agrupada por estado: Aguardando aprovação / Publicados / Rejeitados com `motivoRejeicao` inline) + CTA "Novo Projeto"; professor sem vínculos → destaque/aba "Instituições (0–4)" (onboarding opcional, **não bloqueante**); professor com vínculos → aba "Pendentes" (fila) e histórico "Aprovados"; admin → atalho Admin Main.
  - **success:** Aluno e professor logados acessam o Perfil (identidade MSAL somente leitura + abas por papel). O retorno do MSAL leva **sempre** ao Perfil (idempotente — professor zerado tratado como primeiro acesso com onboarding não-bloqueante); aprovação/rejeição permanece ação dedicada (dialog de `motivoRejeicao`) a partir da lista — o perfil é **hub, não workspace**; nenhuma das superfícies consolidadas existe como rota paralela duplicada.

## Constraints

- Login MSAL restrito ao **tenant único da CPS** — se aberto a qualquer conta Microsoft, qualquer pessoa de fora logaria como aluno. (descarta: login multi-tenant/contas pessoais)
- A regra de classificação por domínio é **centralizada e autoritativa no back-end** — nunca confiada no front-end; aluno é quem tem UPN terminando em `@aluno.cps.sp.gov.br` (checado antes), professor quem termina em `@cps.sp.gov.br`; demais membros do tenant → aluno; não há lista separada de professores (o domínio é a lista). (descarta: CRUD de professores, classificação no cliente)
- Limite flexível de **0 a 4 instituições por professor** (mínimo 0, teto 4) — seleção opcional no primeiro login e alterável a qualquer momento. (descarta: vínculo obrigatório no onboarding / vínculo superior a 4)
- Instituições são **dado de catálogo** (lista oficial pré-populada), mantidas por um **admin do sistema**, não por secretaria; professor não cadastra instituição em texto livre. (descarta: cadastro livre de instituição)
- Projeto **não possui estado RASCUNHO** — nasce diretamente como `AGUARDANDO_APROVACAO`. Estados válidos na máquina de estados: `AGUARDANDO_APROVACAO`, `APROVADO`, `REJEITADO`. No estado `REJEITADO`, o campo `motivoRejeicao` é obrigatório.
- O projeto persiste o e-mail do professor responsável (`emailProfessorResponsavel: string`), além de `instituicaoId`, `titulo`, `descricaoCurta`, `conteudoEditorJs`, `linkRepositorio`, `imagemCapaUrl`, `palavrasChave`, `anoPublicado`, `criadoEm` e `atualizadoEm`. As imagens complementares do projeto são **blocos nativos do Editor.js** dentro de `conteudoEditorJs` — **não existe atributo `imagensExtras`**.
- O Integrante contém `nome` e `linkLinkedin`; **não possui `papelNoProjeto` no MVP**.
- `imagemCapaUrl` é um arquivo enviado ao **back-end** (multipart) e servido por ele (URL interna).
- **Notificações fora do MVP**: o e-mail do professor responsável é registro informativo; não há envio de e-mail/convite.
- **Admin**: entra via **MSAL como os demais**; o papel `ADMIN` é definido por e-mail semeado no banco (seed), sem senha de formulário.
- Visitantes não autenticados têm acesso somente leitura aos projetos no estado `APROVADO`.

## Non-goals

- Papel **gestor**: não existe login institucional de gestor, tela de instituição como área de trabalho de papel, aprovação de professores pelo gestor nem cadastro de instituição pelo gestor — papel removido por decisão (minimizar trabalho de secretaria/gestor).
- Cadastro/suporte de usuários fora do tenant da CPS para postagem ou aprovação de projetos.
- Edição ou criação de projetos por visitantes não autenticados.
- Estado RASCUNHO para projetos.
- **Notificações/convites por e-mail** (incluindo para "Professores Convidados").
- **`papelNoProjeto`** no Integrante (MVP).
- **Upload/importação em runtime** do catálogo de instituições (endpoint de importação) no MVP — mantém-se CRUD simples pelo admin. A **carga do catálogo oficial no boot** (seed idempotente a partir dos recursos versionados) está **no escopo do MVP** como dado oficial de catálogo — ver §"Seed do catálogo de instituições".

## Success signal

Um professor real da CPS faz login e associa de 0 a 4 instituições; um aluno real do mesmo tenant posta um projeto informando o e-mail do professor responsável; o projeto nasce como `AGUARDANDO_APROVACAO`; o professor aprova o projeto na fila da sua instituição e o estado muda para `APROVADO`. Um visitante não autenticado acessa o catálogo público, pesquisa e visualiza os detalhes do projeto aprovado. Tentativas de login de conta fora do tenant falham. Isso é demonstrável de ponta a ponta sobre a pilha (front + back + docker) do repositório.

## Assumptions

- O domínio de classificação é lido do `preferred_username`/UPN retornado pelo MSAL; se a fonte real do e-mail diferir, a regra de CAP-2 deve ser ajustada.
- Sufixos de domínio confirmados pelo usuário: aluno `@aluno.cps.sp.gov.br`, professor `@cps.sp.gov.br` (checagem de aluno antes de professor).

## Open Questions

- Os domínios `@cps.sp.gov.br` (professor) e `@aluno.cps.sp.gov.br` (aluno) são **exclusivos** na prática — nenhum aluno/estagiário/secretaria no primeiro, nenhum professor no segundo? Se quebrar, a regra de classificação precisa de segunda validação.
- ~~Quem aprova o projeto: somente o professor responsável ou qualquer professor da instituição vinculada?~~ **Resolvida (elicitação 2026-09-22):** qualquer professor vinculado à instituição aprova; o responsável fica destacado (ver CAP-5).

---

# Seed do catálogo de instituições (especificação técnica)

> Contrato técnico da carga do catálogo oficial de instituições Fatec/CPS no boot (decisão `ARCHITECTURE-CATALOGO-INSTITUICOES.md`, 2026-09-22; binds: CAP-3, FR-5/6/7, NFR-4, AD-5). Cobre apenas a carga/curadoria do catálogo; seleção 0–4 e fila de aprovação seguem nos artefatos correspondentes.

## Dados e transformação

- Fonte oficial: `exportacao_cursos_etec_22_09_2026.csv` (raiz do repo; 95 linhas, 17 colunas, `;`, aspas). Qualidade medida: 86 unidades em operação, 9 planejadas 2027, 3 pares de código duplicado (309/275/204) e 6 códigos vazios — **todos eliminados pela exclusão das unidades 2027**; após o corte: 86 códigos únicos, 0 vazios; CNPJ/endereço/município/região/logotipo sem vazios; telefone vazio em 1 unidade (Fatec Votorantim — permitido); 2 sites vazios; 70/86 endereços com CEP.
- Regras determinísticas (uma vez, na geração do CSV versionado): selecionar só unidades em operação; linha com "2027" no nome é descartada (e linhas sem/duplicando código após o corte); `nome` com trim + colapso de espaços mantendo denominação completa; `endereco` limpo mantendo CEP; `municipio` → `cidade`; `telefone` sem o sufixo "Discagem Abreviada"; `cnpj` normalizado; `site` URL validada com vazio permitido; `linkLogo` mantida como URL externa (não baixada).
- Artefato versionado: `resources/seeds/instituicoes.csv` (UTF-8 com BOM, `;`, cabeçalho fixo).

## Modelo (AD-CI-1)

`Instituicao` ganha campos (id UUID da PK permanece; nada de narrativa/PII):

| CSV | Modelo | Norma |
| --- | --- | --- |
| `Cód. da Unidade` | `codigoUnidade` | `String`, `@Column(unique = true, nullable = false)` — chave de negócio do seed |
| `Unidade` | `nome` | normalizado, not null |
| `Endereço` | `endereco` | texto limpo (CEP preservado) |
| `Município` | `cidade` | normalizado |
| — | `estado` | constante `"SP"` |
| `Região Administrativa` | `regiaoAdministrativa` | normalizado (filtro por região) |
| `CNPJ` | `cnpj` | formato da fonte |
| `Telefone` | `telefone` | só os telefones (ex.: `(18) 3522-4181 / 3502-4500`); vazio permitido (1 caso: Votorantim) |
| `Site` | `site` | URL validada; vazio ok (2 casos) |
| `Logotipo` | `linkLogo` | URL externa (link) |
| — | `ativo` | `true` nas seedadas; reserva ativação futura de 2027 |
| — | `criadoEm`/`atualizadoEm` | padrão existente |

Não persiste `Histórico`, `Direção*`, `Aniversário da Cidade`, `Redes Sociais`, `Arquivos`, `Cursos` (coluna inconcatenável — futuro modelo próprio).

`InstituicaoRepository` + `Optional<Instituicao> findByCodigoUnidade(String)`. DTOs `*Request`/`*Response` espelham os campos (Request: `codigoUnidade` e `nome` obrigatórios). `SecurityConfig` já exige `ADMIN` em escrita — sem mudança.

## Mecanismo de carga (AD-CI-3)

- **`CatalogoSeeder`** (`CommandLineRunner`, pacote `config` ou `seed`), após o schema do Hibernate, lê recursos versionados em ordem declarada: `instituicoes.csv` → (futuro) `projetos.csv`. Parsing via OpenCSV/apache-commons-csv (adicionar ao `pom.xml`).
- **FK por chave de negócio:** future seed de `Projeto` resolve instituição via `findByCodigoUnidade` e associa o `instituicaoId` — nunca hard-coda UUID (UUID é gerado pelo banco a cada carga). Ordem sequencial no mesmo runner garante instituições antes de projetos no primeiro boot.
- **Idempotência:** `faterepo.seed.mode` — `upsert` (padrão: `INSERT ... ON CONFLICT (codigoUnidade) DO NOTHING`, insere só registros ausentes; **preserva edições do admin** em linhas já semeadas) | `replace` (`DELETE` + `INSERT` no mesmo transaction, resync total a partir da fonte; uso pontual em banco dev zerado; `DELETE` de instituição com `Projeto` apontando falha por FK → fail-fast com mensagem clara).
- **Validação na carga:** linha inválida (código vazio/duplicado) é rejeitada com log **sem abortar o boot**.
- **Por que boot:** reproduzível no docker, zero UI nova, mantém o non-goal de importação em runtime (endpoint `POST /importar` e script SQL one-off foram rejeitados).

## Configuração

```yaml
faterepo:
  seed:
    mode: upsert        # upsert (padrão) | replace
    files:              # ordem declarada = ordem de carga (FK-safe)
      - seeds/instituicoes.csv
      # - seeds/projetos.csv   # futuro
```

`ddl-auto: update` cria as novas colunas; sem migração manual.

## Critérios de aceite

- **AC-1:** boot em banco zerado (`upsert`) → exatamente 86 `Instituicao`, `codigoUnidade` único/not null, `ativo=true`, `estado="SP"`.
- **AC-2:** re-boot não duplica nem sobrescreve alterações admin (upsert insere só o ausente).
- **AC-3:** `replace` em banco dev zerado recarrega 86; com `Projeto` referenciando, falha em `DELETE` com mensagem clara.
- **AC-4:** linha inválida → rejeitada com log, boot continua (demais persistem).
- **AC-5:** `findByCodigoUnidade` resolve corretamente um future seed de projeto no primeiro boot.
- **AC-6:** `GET /instituicoes` liberado; escrita segue exigindo `ADMIN`.

## Open questions

- Coluna de conflito do seed de projeto (`titulo`+`instituicao_id` vs chave própria) — decidir quando o seed existir.
- Manutenção do CSV versionado: procedimento manual junto a `resources/seeds/README` quando o CPS publicar novo export.
- Unidades 2027: manter fora hoje; `ativo=false` apenas se houver necessidade futura de pré-cadastro.
- Telefone com múltiplos números: manter string da fonte por ora.