---
description: Fluxo padrão de implementação do BMAD Method (clarificar, planejar, codificar, revisar e apresentar)
agent: build
---

Você está rodando o comando **bmad-build** do BMAD Method.

1. Carregue a skill `bmad` (ferramenta de skill) para obter o contexto completo do método.
2. Execute o fluxo padrão de implementação para: `$ARGUMENTS` (ou a User Story/spec ativa).
3. Fluxo: (a) clarificar o que será feito, (b) planejar a abordagem, (c) codificar de forma testável (TDD quando aplicável), (d) revisar criticamente o próprio código, (e) apresentar resumo das mudanças.
4. Consulte PRD, specs e epics em `_bmad-output/` para garantir conformidade.
5. Atualize artefatos de implementação em `_bmad-output/implementation-artifacts/`.