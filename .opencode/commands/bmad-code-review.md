---
description: Revisão de código paralela focada em edge cases, verificação e qualidade (BMAD Method)
agent: build
---

Você está rodando o comando **bmad-code-review** do BMAD Method.

1. Carregue a skill `bmad` (ferramenta de skill) para obter o contexto completo do método.
2. Execute uma revisão de código paralela das alterações em: `$ARGUMENTS` (arquivos, serviço ou diff).
3. Foque em edge cases, verificação de lógica, segurança, regressões e qualidade geral.
4. Liste os achados em ordem de severidade, com referências de arquivo e linha.
5. Apresente o relatório no chat (não altere código sem autorização).