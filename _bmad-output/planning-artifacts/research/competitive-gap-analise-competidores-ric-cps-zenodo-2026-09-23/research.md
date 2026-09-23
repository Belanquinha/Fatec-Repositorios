---
title: 'competitive research: O que os concorrentes (RIC.CPS, Zenodo) fazem que o Fatec-Repositorios nao faz'
type: 'competitive'
topic: 'Gap analysis: o que nossos concorrentes (RIC.CPS, Zenodo) fazem que o Fatec-Repositorios nao faz'
decision: 'Identificar ~5 lacunas de produto frente a RIC.CPS e Zenodo.org para guiar roadmapping'
source: 'web (run nativo)'
status: complete
preset: 'quick (superficial)'
validation: 'normal'
created: '2026-09-23'
updated: '2026-09-23'
---

# competitive research: Gap analysis vs RIC.CPS e Zenodo.org

**Decisão que esta pesquisa serve:** Identificar ~5 lacunas de produto frente a RIC.CPS e Zenodo.org para guiar roadmapping.

## 1. Sumário executivo

A análise superficial dos dois concorrentes mostra que o Fatec-Repositorios hoje é uma **vitrine de curadoria sem "corpo de repositório"**: não há armazenamento de arquivos, identificador persistente, metadados padronizados, interoperabilidade, controle de acesso em camadas nem métricas de uso — capacidades que **ambos** os concorrentes possuem como núcleo do produto. As 5 lacunas mais salientes são: (1) armazenamento de arquivos dos projetos [1][8]; (2) identificador persistente (handle/DOI) por registro [8][9][18]; (3) metadados estruturados + OAI-PMH/exportação [4][10][16]; (4) política de acesso em camadas (aberto/embargo/catálogo) [3][11]; (5) métricas de uso e navegação estruturada [8][1][9].

**Maior ressalva:** RIC.CPS e Zenodo são repositórios *institucionais* com mandato legal (RIC.CPS) ou escala global (Zenodo, CERN/CE). Não se trata de clonar nenhum dos dois, mas de usar essas capacidades como checklist de roadmapping — a recomendação não é competir de frente, e sim fechar as lacunas que um "depositório de trabalhos acadêmicos Fatec" deveria ter para ser citável, preservável e integrável.

**Limite de validade:** análise propositalmente superficial (pedido do usuário); ~16 fontes primárias; capacidades verificadas em fontes independentes no dia 2026-09-23.

## 2. Dimensões

### 2.1 RIC.CPS (repositório institucional do Centro Paula Souza)

O RIC.CPS é o repositório institucional da rede CPS gerenciado pelo Centro de Gestão Documental, instituído por portaria estadual (Portaria CEETEPS-GDS nº 3793/2023, que sucede a 3015/2021), com comunidades de topo CGESG (graduação), CGETEC (médio/técnico) e CGPEP (pós-graduação/extensão/pesquisa) [2]. É baseado em **DSpace** e conta com ~43–45 mil arquivos [1][6].

Capacidades relevantes para a comparação:
- **Depósito curatorial de PDFs de TCCs** (Fatec e Etec), com depósito obrigatório por portaria e Termo de Autorização em PDF/A [2][7].
- **Política de liberação seletiva**: apenas TCCs de graduação com nota ≥9 e indicação de banca, ou Etec com menção B/MB, ficam abertos integralmente; os demais são catalogados como "publicações institucionais" sem texto completo [3] — na prática, camadas de acesso.
- **Identificador persistente**: URIs `handle/123456789/…` por item (ex.: registro real de TCC com handle 123456789/10798) [7][verificação 1].
- **Interoperabilidade OAI-PMH 2.0** expondo 12 esquemas de metadados (oai_dc, qdc, etdms, mods, mets, marc, dim, uketd_dc, didl, ore, rdf, xoai) [4].
- **Descoberta estruturada**: navegação por data, autor, título, assunto e curso, com vocabulário controlado baseado no USP Dedalus gerido por bibliotecários [1][5].
- Contas de usuário ("My DSpace", assinatura por e-mail), módulo "Relatórios" e UI multi-idioma [1][2].

Ausência de evidência neste run: **DOI** e **licenças/CC explícitas** nos itens (registro real mostra apenas "protected by copyright, unless otherwise indicated") [verificação 1] — confiança média: é ausência de evidência, não confirmação de ausência.

### 2.2 Zenodo (CERN)

O Zenodo é um repositório de pesquisa aberto e multidisciplinar operado pelo CERN em parceria com o projeto OpenAIRE; gratuito para upload e acesso, financiado por CERN + CE [9][10]. Aceita datasets, software, posters, apresentações, publicações e anais; comunidades podem hospedar coleções e curar registros [11].

Capacidades relevantes:
- **DOI DataCite para todo registro** — cada upload ganha DOI citável e rastreável [9][18]; registros reais confirmam `10.5281/zenodo.…`, com DOI conceitual "cite all versions" [8][17].
- **Versionamento de registros**: nova versão gera registro e identificador próprios, vinculados aos anteriores [12][8].
- **Controle de acesso**: metadados sempre públicos; arquivos podem ser restritos ou embargados [11].
- **Estatísticas de uso** por registro (views, downloads, volume de dados) [14][8].
- **Interoperabilidade**: OAI-PMH, API REST de depósito, exportação em MARCXML/Dublin Core JSON/DataCite/BibTeX etc., e indexação no OpenAIRE [10][16][17].
- **Integrações**: GitHub (arquiva releases automaticamente) e autenticação via ORCID/eduGAIN que autocompleta autores [13].
- **Limites**: 50 GB e máx. 100 arquivos por registro, com franquias adicionais; sem cota de número de registros ou comunidades [15].

## 3. Insights transversais

1. **A lacuna de fundo não é de UI, é de "ser um repositório"**: ambos os concorrentes têm como núcleo persistir arquivos + dar identidade persistente + expor metadados. O Fatec-Repositorios tem aprovação de professor (diferencial curatorial), mas nenhuma dessas três camadas de fundo.
2. **Dois modelos de identidade**: DOI (padrão global, Zenodo) vs handle (padrão institucional DSpace/BR, RIC.CPS). Para um produto Fatec, handle já resolve citabilidade mínima; DOI é o prêmio maior (presença em agregadores internacionais).
3. **O fluxo de aprovação do nosso app é um ativo único**: RIC.CPS faz curadoria *depois* do depósito (bibliotecário/política); nós fazemos *antes* (professor). Isso não resolve as lacunas técnicas, mas é o que justifica a aposta — a comparação com RIC.CPS/II é parcialmente sobreposição, mas nosso diferencial é curadoria pedagógica em tempo de publicação.

## 4. Evidência contrária

Red-team não executado (validation=normal, red_team=off). Nota verificadora: nenhuma claim foi contestada; a única disputa encontrada foi a contagem de arquivos do RIC.CPS (~44.835 na home vs ~43.177 no snapshot de busca desta sessão), que reflete disparidade temporal de snapshot — não é um conflito substancial. A claim ref=5 (RIC.CPS sem DOI/CC) permanece **unverified** por natureza (ausência de evidência).

## 5. Recomendações

Checklist de 5 lacunas (em ordem de prioridade para roadmapping), cada uma ancorada no que os concorrentes fazem:

1. **Armazenamento real de arquivos de projeto** — hoje as imagens são base64 in-browser e não há endpoints de arquivo; ambos os concorrentes persistem o documento integral (Zenodo: 50 GB/registro [9][15]; RIC.CPS: PDFs de TCC, ~43–45 mil arquivos [1][6]). *Confiança: alta (verificado).*
2. **Identificador persistente por projeto (handle ou DOI)** — sem ele um projeto não é citável/preservável; RIC.CPS usa handle/123456789 [verificação 1]; Zenodo minta DOI DataCite [8][18]. *Confiança: alta (DOI e handle — verificados).*
3. **Metadados estruturados + interoperabilidade (OAI-PMH e export DC/JSON)** — porta de entrada para agregadores (OpenAIRE etc.); RIC.CPS expõe OAI-PMH 2.0/12 esquemas [4]; Zenodo exporta Dublin Core/DataCite/MARCXML e indexa OpenAIRE [10][16]. *Confiança: alta (ref=3, 4 verificados).*
4. **Política de acesso em camadas (aberto / embargado / só-catálogo)** — conversa direta com nosso fluxo de aprovação: Zenodo embarga/restringe [11]; RIC.CPS só abre o que passa na política da banca (nota ≥9 / B/MB) [3]. *Confiança: alta.*
5. **Métricas do registro + descoberta estruturada** — contadores de views/downloads e navegação por curso/autor/assunto; Zenodo expõe estatísticas [8][14]; RIC.CPS tem módulo Relatórios e browse por curso/assunto [1]. *Confiança: alta.*

**Extras em cima da mesa** (não contados nas 5, mas observados): versionamento de versões (Zenodo [12]), integração GitHub e ORCID (Zenodo [13]), comunidades/coleções com curadoria (ambos [11][1]).

*Consumo downstream:* recomendações podem alimentar o epic backlog do sprint-status e a seção de diferenciação do PRD — sem compromisso de competir de frente com repositórios institucionais.

## 6. Perguntas em aberto

- RIC.CPS gera DOI em algum fluxo (hoje só handle)? Não respondido neste run.
- O RIC.CPS possui SWORD/REST API de depósito além do OAI-PMH? Não evidenciado.
- Volume/limites de armazenamento do RIC.CPS (nuvem institucional) e do Fatec-Repositorios-alvo? Fora do escopo superficial.
- É desejável (ou politicamente viável no CPS) que o Fatec-Repositorios colha/espelhe conteúdo do próprio RIC.CPS via OAI-PMH? Decisão de produto, não de pesquisa.

## 7. Apêndice de fontes

| [n] | Suporta | Editor | Acessado | Confiança |
|---|---|---|---|---|
| [1] | RIC.CPS home, comunidades, ~44,8k arquivos, browse | [CPS/CGD](https://ric.cps.sp.gov.br/) | 2026-09-23 | alta |
| [2] | Portaria CEETEPS-GDS 3793/2023 (instituição, comitê, integrações) | [CEETEPS/CPS](https://bkpsitecpsnew.blob.core.windows.net/uploadsitecps/sites/18/2024/01/Portaria-CEETEPS-GDS-3793_2023-11-10_.pdf) | 2026-09-23 (pub 2023-11-10) | alta |
| [3] | Política de liberação seletiva (Fatec nota ≥9 + banca / Etec B-MB; demais "publicação institucional" sem texto; Termo PDF/A) | [CPS/DGUI — Portaria CEETEPS-GDS 4069/2024](https://dgui.cps.sp.gov.br/dguidocumentos/portaria-ceeteps-gds-no-4069-de-15-de-julho-de-2024/) | 2026-09-23 (pub 2024-07-15) | alta |
| [4] | OAI-PMH 2.0 + 12 esquemas de metadados | [RIC-CPS](https://ric.cps.sp.gov.br/oai/request?verb=Identify) | 2026-09-23 | alta |
| [5] | Vocabulário controlado baseado no Dedalus | [CPS/CGD](https://ric.cps.sp.gov.br/controlledvocabulary/info.jsp) | 2026-09-23 | alta |
| [6] | RIC-CPS = DSpace alimentado por bibliotecários (fonte terceira gov) | [Dados Abertos SP](https://dadosabertos.sp.gov.br/dataset/repositorio-institucional-do-conhecimento-ric-cps) | 2026-09-23 | alta |
| [7] | Depósito obrigatório de TCC (Portaria 3015/2021), fluxo Fatec Franca, handle 123456789/10798 | [Fatec Franca](https://site.fatecfranca.edu.br/estudante/tg?catid=2&id=147%3Arepositorio-institucional-do-conhecimento-do-centro-paula-souza-ric-cps&view=article) | 2026-09-23 | alta |
| [8] | Registro real Zenodo: DOI, versões, stats, export, OpenAIRE | [CERN/Zenodo](https://zenodo.org/records/14083199) | 2026-09-23 | alta |
| [9] | Zenodo: operado por CERN+OpenAIRE, DOI por upload, gratuito | [CERN/Zenodo](https://zenodo.org/) | 2026-09-23 | alta |
| [10] | Zenodo: aberto a todos, tipos de conteúdo | [CERN/Zenodo](https://about.zenodo.org/) | 2026-09-23 | alta |
| [11] | Zenodo: acesso restrito/embargo/links secretos, comunidades | [CERN/Zenodo docs](https://help.zenodo.org/docs/deposit/about-records) | 2026-09-23 | alta |
| [12] | Zenodo: versionamento de registros | [CERN/Zenodo docs](https://help.zenodo.org/docs/deposit/manage-versions/) | 2026-09-23 | alta |
| [13] | Zenodo: ORCID/GitHub/eduGAIN, autocomplete de autores | [CERN/Zenodo docs](https://help.zenodo.org/docs/profile/linking-accounts/) | 2026-09-23 | alta |
| [14] | Zenodo: estatísticas de uso (views/downloads) | [CERN-Zenodo FAQ](https://support.zenodo.org/help/en-gb/4-usage-statistics/20-why-does-my-restricted-records-shows-views-and-downloads) | 2026-09-23 (pub 2025-09) | alta |
| [15] | Zenodo: limites 50 GB / 100 arquivos + franquias | [CERN-Zenodo FAQ](https://support.zenodo.org/help/en-gb/1-upload-deposit/80-what-are-the-size-limitations-of-zenodo) | 2026-09-23 (pub 2026-04) | alta |
| [16] | Zenodo: metadados JSON/DataCite/Dublin Core/MARCXML, OpenAIRE | [CERN/Zenodo](https://about.zenodo.org/policies/) | 2026-09-23 | alta |
| [17] | Zenodo: DOI conceitual "cite all versions", ISO exports (ex.: registrar-se 2º) | [CERN/Zenodo](https://zenodo.org/records/16537184) | 2026-09-23 | alta |
| [18] | CERN: Zenodo minta DOIs, 50 GB gratuito | [CERN Library](https://library.cern/submit-and-publish/persistent-identifiers/doi) | 2026-09-23 | alta |

## 8. Mapa de staleness

Janela aplicada (pack competitive; classe `capability` → 3 meses), gerado em 2026-09-23:

| Claim | Classe | Pub | Re-checar |
|---|---|---|---|
| Zenodo DOI para cada upload | capability | 2026-09 | 2026-12-01 |
| Zenodo versões/embargo/estatísticas | capability | 2026-09 | 2026-12-01 |
| Zenodo OAI-PMH/export/OpenAIRE | capability | 2026-09 | 2026-12-01 |
| RIC.CPS = DSpace + handle + deposição curatorial | capability | 2021 | **2021-04-01 (stale)** |
| RIC.CPS sem DOI/CC evidenciados | capability | 2026-09 | 2026-12-01 |

**Revisão mais antiga: 2021-04-01 (claim ref=4)** — a política/documento é de 2021, mas a capacidade (DSpace + handle + depósito por bibliotecários) foi **reconfirmada hoje** por fonte independente [6][7]. Na prática, o que precisa re-check em ~2 meses (2026-12-01) é a primeira linha da lista (DOI Zenodo e limites de storage, itens que mudam com frequência). O comando Refresh/Deepen cobre essa atualização.