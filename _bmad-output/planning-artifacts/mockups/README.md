# institution-autocomplete — mock de composição

Arquivo único: `institution-autocomplete.html` (1:1, quatro frames anotados, HTML autocontido).
**DESIGN.md e EXPERIENCE.md vencem em conflito** — este mock é referência de composição, não fonte de verdade.

| Frame | Modo / superfície | O que ilustra |
|---|---|---|
| A | `single` · Seção 2 de `/projeto-forms` (aluna) | Popup aberto com consulta "ipir" (3 unidades, `primary-soft` + aresta 2px `accent-ink`, `aria-live` de contagem) e, abaixo, o valor confirmado: metadado "Cód. 291 · São Paulo · Região Litoral" + ação **Alterar**. |
| B | `multi` · aba "Instituições (0–4)" do `/perfil` (professor) | Chips quadrados com `border #6A6A6C`, contador "3 de 4 unidades" em tinta neutra, CTA primária "Salvar vínculos"; depois o teto: "4 de 4" em `accent-ink` e a 5ª unidade **recusada inline** em cinza (`#F0F0F0` + 2px `#202124`). Sem sair do Perfil. |
| C | `filter` · Home pública (anônimo, sem token) | Mesmo componente como filtro, sem `<select>` de 86 opções: `institution-chip` de contorno ao lado do `filter-chip` amarelo de palavra-chave, botão "Limpar". |
| D | `<md` (375px) | Popup 100% da largura do campo, ancorado, `z-index 30` **acima** do rodapé sticky de submit (`z-index 20`) — o rodapé nunca fica coberto. |

Regras fixas no arquivo: radius 0 em tudo, `#B20000` (cps) nunca em estado, foco/aresta/check em
`accent-ink #8A6802` (o `#DCA703` a 2.18:1 reprova 1.4.11), borda de UI em `#6A6A6C`.
Logotipos: 85 PNGs reais em `../../front-end/public/logos-fatec/`, com fallback no ícone `school`.
