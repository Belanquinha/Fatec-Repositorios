# D1 — RIC.CPS (competitive teardown, round 1)

Research assistant: general (bmad-deep-recon fan-out) · accessed 2026-09-23· topic: gap analise vs Fatec-Repositorios

## Findings

- [claim] RIC-CPS é o repositório institucional da rede Centro Paula Souza (CPS), definido como ferramenta para gerir, armazenar, preservar e disseminar produção científica, tecnológica, artístico-cultural e técnico-administrativa das comunidades CPS (Fatecs, Etecs, pós-graduação) | source: https://ric.cps.sp.gov.br/ | publisher: CPS/CGD (Centro de Gestão Documental) | pub_date: N/A | accessed: 2026-09-23 | confidence: high | class: scope
- [claim] Instituído por decreto estadual via Portaria CEETEPS-GDS nº 3793/2023 (sucede 3013/2021), com Comitê Gestor e comunidades de topo CGESG (graduação), CGETEC (médio/técnico), CGPEP (pós/extensão/pesquisa) | source: https://bkpsitecpsnew.blob.core.windows.net/uploadsitecps/sites/18/2024/01/Portaria-CEETEPS-GDS-3793_2023-11-10_.pdf | publisher: CEETEPS/CPS | pub_date: 2023-11-10 | accessed: 2026-09-23 | confidence: high | class: scope
- [claim] Política de depósito: somente TCCs de graduação com nota ≥9 e indicação de banca, e TCCs Etec com menção B/MB são abertos integralmente (com Termo de Autorização em PDF/A); demais são catalogados como "publicações institucionais" sem texto completo | source: https://ric.cps.sp.gov.br/bitstream/123456789/12386/1/PoliticasRICCPS_2021.pdf | publisher: CPS/NB-CGD | pub_date: 2021 | accessed: 2026-09-23 | confidence: high | class: feature
- [claim] Baseado no DSpace (software de código aberto MIT/HP; distribuído no Brasil via Ibict); rodapé do site credita "DSpace © By Fatec Americana"; inventário ~44.835 arquivos | source: https://ric.cps.sp.gov.br/bitstream/123456789/12386/1/PoliticasRICCPS_2021.pdf e https://ric.cps.sp.gov.br/ | publisher: CPS/NB-CGD | pub_date: 2021 / 2026-09-23 | accessed: 2026-09-23 | confidence: high | class: capability
- [claim] Estrutura hierárquica comunidades/subcomunidades/coleções/itens, navegação por "cursos" e vocabulário controlado (baseado no USP Dedalus) gerido por bibliotecários do CPS | source: https://ric.cps.sp.gov.br/ (home) e https://ric.cps.sp.gov.br/controlledvocabulary/info.jsp | publisher: CPS/CGD | pub_date: N/A | accessed: 2026-09-23 | confidence: high | class: capability
- [claim] Interoperável via OAI-PMH 2.0 expondo 12 formatos de metadados (oai_dc, qdc, etdms, mods, mets, marc, dim, uketd_dc, didl, ore, rdf, xoai); oai-identifier reportou "repositoryIdentifier=localhost" | source: https://ric.cps.sp.gov.br/oai/request?verb=Identify e ?verb=ListMetadataFormats | publisher: RIC-CPS | pub_date: acessado 2026-09-23 | accessed: 2026-09-23 | confidence: high | class: capability
- [claim] Oferece contas de usuário (My DSpace, atualizações por e-mail, edição de perfil), módulo "Relatórios" e UI multi-idioma; portaria manda integração com outros sistemas do CPS e hospedagem na nuvem institucional | source: https://ric.cps.sp.gov.br/ (nav) e Portaria 3793/2023 | publisher: CPS/CGD | pub_date: 2023-11-10 | accessed: 2026-09-23 | confidence: high | class: capability

## Leads

- Identificadores persistentes: handles em /handle/123456789/…; existência de DOI (ou correção do oai-identifier "localhost") não verificado.
- Conteúdo do módulo de relatórios/estatísticas (página não renderizou o corpo).
- Depósito mediado por bibliotecários vs auto-arquivamento (a portaria menciona "auto arquivamento").
- Versão do DSpace e presença de SWORD/REST API.

## Not found

- Sem evidência neste run de registro DOI, metadados de licença/CC explícitos nos itens, ou endpoints SWORD/REST de depósito; internals do módulo de estatísticas não legíveis.