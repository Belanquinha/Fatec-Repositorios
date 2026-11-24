# PATTERNS — Fatec-Repositorios (Front-End)

> Contexto para agentes e desenvolvedores. Documenta a paleta, os padrões
> visuais e o que **evitar** ao trabalhar no front-end.

## Paleta de cores (CSS variables)

Definida em `front-end/src/styles/paleta.css` (root + `.dark`).

| Role | Variável | Exemplo |
|---|---|---|
| **Vermelho CPS** | `--vermelho-500` | `#B20000` |
| **Azul repositório** | `--azul-500` | `#4345CB` |
| **Amarelo primário** | `--amarelo-500` | `#DCA703` |
| **Verde sucesso** | `--verde-500` | `#00B32A` |
| **Branco** | `--branco-500` | `#F4F6F9` |
| **Cinza** | `--cinza-500` | `#202124` |

### Uso correto

- **Amarelo** → destaque, CTA, breadcrumb, badges de papel do usuário.
- **Vermelho** → erros, rejeições, alertas, *_removido/_rejeitado*.
- **Azul** → links, aviso de navegação, destaque de repositório.
- **Verde** → sucesso, aprovação, estado _ativo_.
- **Cinza** → texto secundário, fundos de sobreposto, skeletons.

### Modo escuro

`paleta.css` mapeia variáveis semânticas. Exceções são listadas em
`front-end/src/styles/tema-escuro.css` (texto escuro sobre amarelo, fundos fixos,
cores chapadas, links azuis). **Nunca** usar cores hardcoded em componentes.

## Layout e componentes

- **Mobile-first**: breakpoint `--mobile-brakepoint: 768px` (paleta.css).
- **Angular 21** (standalone), TypeScript ~5.9, Tailwind CSS 4.
- **Editor.js** para rich text em projetos pendentes/aprovados.
- **Azure MSAL Angular** (`@azure/msal-angular`, v5) para login.
- **Nginx** serve o build estático (Docker `front-end`).

## O que evitar

- ❌ Cores com hex (ex: `#B20000`) em componentes — usar variáveis semânticas.
- ❌ Criar novas classes de cor sem consultar `paleta.css`.
- ❌ Usar `v-if`/`hidden` para rotas protegidas — usar `auth.guard.ts`.
- ❌ Carregar imagens via `http://` — usar `https://` ou `/uploads`.
- ❌ Alteração de `theme-escuro.css` sem rodar `npm run build` e testar.
- ❌ Duplicar estilos de palette no CSS — extrar para variáveis.

## Componentes essenciais (matíz)

| Componente | Responsabilidade | Caminho |
|---|---|---|
| `header/header` | Navegação, logo, auth e alternância de tema | `src/app/shared/components/header` |
| `footer/footer` | Rodapé, links institucionais e gerenciamento de cookies | `src/app/shared/components/footer` |
| `cookie-banner/cookie-banner` | Aviso e preferências de cookies (LGPD) | `src/app/shared/components/cookie-banner` |
| `card-projeto/card-projeto` | Card de projeto no catálogo | `src/app/shared/components/card-projeto` |
| `auth/microsoft-login-button` | Botão SSO Microsoft CPS | `src/app/shared/components/microsoft-login-button` |
| `features/autenticacao/autenticacao` | Tela `/entrar` (login, cadastro com código e recuperação) | `src/app/features/autenticacao` |
| `features/upload-projeto/upload-projeto` | Upload de projeto | `src/app/features/upload-projeto` |
| `features/ver-projeto/ver-projeto` | Exibição de projeto | `src/app/features/ver-projeto` |

## Testes

- **Vitest** para unitários (`*.spec.ts`), **Playwright** para E2E (`e2e/*.spec.ts`).
- Rodar `npm run test` (unitários) ou `npm run test:e2e` (E2E) antes de commitar.

## Regras para agentes

- Sempre usar variáveis de paleta, nunca hex.
- Sempre proteger rotas com `auth.guard.ts`.
- Sempre testar modo escuro ao adicionar novo estilo.
- Ao adicionar componente, criar `spec.ts` correspondente.
