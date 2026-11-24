# 🎭 Testes End-to-End (E2E) com Playwright — Fatec Repositórios

Este diretório contém a suíte de testes automatizados E2E reais integrados com o back-end Spring Boot e banco PostgreSQL via Docker.

---

## 📁 Estrutura de Arquivos

```text
front-end/e2e/
├── reais/                          # 🚀 TESTES REAIS (Back-end + Docker + Postgres)
│   ├── helpers/
│   │   ├── auth-helper.ts          # Criação e confirmação de contas reais via API
│   │   ├── docker-helper.ts        # Captura o código de 6 dígitos dos logs do container Docker
│   │   ├── projeto-helper.ts       # Criação direta de projetos pendentes via API
│   │   └── sessao-helper.ts        # Limpa a sessão do navegador sem usar addInitScript
│   ├── cadastro-real.spec.ts       # Cadastro real de Aluno/Professor com leitura do código no Docker
│   ├── login-real.spec.ts          # Login real com validação BCrypt no banco de dados e sessão
│   ├── publicar-projeto.spec.ts    # Publicação completa (capa, Editor.js com 9 imagens, perfil Aluno e Professor)
│   ├── avaliar-projeto.spec.ts     # Avaliação pelo orientador (Aprovar/Aceitar e Rejeitar com motivo)
│   └── logout-protecao-rotas.spec.ts # Logout e proteção de rotas privadas (AuthGuard e returnUrl)
│
├── auth.spec.ts                    # Testes estruturais da tela /entrar
├── home.spec.ts                    # Testes da página inicial, catálogo e tema
├── navigation.spec.ts              # Testes de navegação e rotas
├── tsconfig.json                   # Configuração TypeScript dos testes E2E
└── README.md                       # Este guia
```

---

## 🚀 Como Executar os Testes

Execute os comandos a partir da pasta `front-end/`:

> **Pré-requisito:** A stack Docker deve estar em execução (`docker compose up -d`).

| Ação | Comando npm | Comando direto Playwright |
|---|---|---|
| **Rodar todos os testes reais** | `npm run test:e2e:real` | `npx playwright test e2e/reais` |
| **Rodar apenas avaliação de projetos (Professor)** | — | `npx playwright test e2e/reais/avaliar-projeto.spec.ts` |
| **Rodar apenas logout e proteção de rotas** | — | `npx playwright test e2e/reais/logout-protecao-rotas.spec.ts` |
| **Rodar apenas publicação de projeto** | — | `npx playwright test e2e/reais/publicar-projeto.spec.ts` |
| **Interface Visual Interativa (UI)** | `npm run test:e2e:ui` | `npx playwright test e2e/reais --ui` |
| **Ver o navegador executando na tela** | `npm run test:e2e:headed` | `npx playwright test e2e/reais --headed` |
| **Depuração passo a passo** | — | `npx playwright test e2e/reais --debug` |
| **Gravar teste visualmente (Codegen)** | `npm run test:e2e:codegen` | `npx playwright codegen http://localhost:4200/entrar` |
| **Ver relatório HTML de execução** | `npm run test:e2e:report` | `npx playwright show-report` |

---

## 💡 Fluxos Cobertos em `e2e/reais/`

1. **`cadastro-real.spec.ts`**:
   - Criação de Aluno com leitura de código do Docker e validação de sessão.
   - Criação de Professor (`@cps.sp.gov.br`) com atribuição do papel `PROFESSOR`.
   - Bloqueio com código incorreto (`Código incorreto...`).

2. **`login-real.spec.ts`**:
   - Login com senha real gravada com hash BCrypt no PostgreSQL.
   - Rejeição de senha inválida (401).
   - Rejeição de e-mail inexistente.

3. **`publicar-projeto.spec.ts`**:
   - **Seção 1:** Título, Imagem de Capa e Descrição Curta.
   - **Seção 2:** Unidade Fatec, Curso, E-mail do Professor Responsável, Repositório e Tags.
   - **Seção 3 (Editor.js):**
     - Cabeçalho H2 (`Header`)
     - Parágrafo de texto (`Paragraph`)
     - Lista não ordenada (`List`)
     - Bloco de código-fonte (`Code`)
     - Bloco de citação (`Quote`)
     - **9 imagens reais** com upload no servidor (`Image`) e legendas.
   - **Seção 4:** Integrantes dinâmicos (Nome + LinkedIn).
   - **Validação no Perfil do Aluno (`/perfil`):** Card visível na aba *Pendentes*.
   - **Validação no Perfil do Professor:** Autenticação como o orientador e validação na fila de avaliação (*Pendentes*).

4. **`avaliar-projeto.spec.ts`**:
   - **Aprovar (Aceitar):**
     - Login com conta de Professor (`@cps.sp.gov.br`).
     - Acesso à tela de perfil (`/perfil`) e aba *Pendentes*.
     - Clique em *Ver Projeto* no card (`/ver-projeto/:id?avaliar=1`).
     - Clique em *Aprovar projeto* no painel de decisão do orientador.
     - Confirmação de mensagem de sucesso (`aprovado e publicado na vitrine`).
     - Retorno ao perfil e validação do projeto na aba *Aprovados*.
   - **Rejeitar (com Justificativa):**
     - Login com conta de Professor e navegação para a aba *Pendentes*.
     - Abertura da tela de avaliação em *Ver Projeto*.
     - Clique em *Rejeitar* e preenchimento da justificativa obrigatória.
     - Confirmação de rejeição e validação do feedback (`rejeitado com retorno ao aluno`).
     - Retorno ao perfil e garantia de que o projeto foi retirado da fila de pendências.

5. **`logout-protecao-rotas.spec.ts`**:
   - **Logout via Menu Lateral:**
     - Autenticação na aplicação via `/entrar`.
     - Abertura do menu lateral drawer (`[data-menu-toggle]`).
     - Clique no botão *Sair* (`logout`).
     - Validação de retorno para a rota `/`, reaparição do botão *Entrar* no cabeçalho e descarte do `accessToken` e chaves do `localStorage`.
   - **Proteção de Rotas (`authGuard`):**
     - Tentativa de acesso sem sessão a `/perfil` com redirecionamento para `/entrar?returnUrl=%2Fperfil`.
     - Tentativa de acesso sem sessão a `/projeto-forms` com redirecionamento para `/entrar?returnUrl=%2Fprojeto-forms`.
     - Autenticação a partir do redirecionamento garantindo que o usuário é levado automaticamente ao destino protegido original.
