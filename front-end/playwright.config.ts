import { defineConfig, devices } from '@playwright/test';

/**
 * Configuração do Playwright para o Fatec-Repositorios (Front-End)
 * Documentação: https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './e2e',
  /* Tempo máximo para cada teste */
  timeout: 30 * 1000,
  expect: {
    timeout: 5000,
  },
  /* Executa os testes em paralelo */
  fullyParallel: true,
  /* Impede subir commits com test.only no CI */
  forbidOnly: !!process.env['CI'],
  /* Tentativas em caso de falha transitória (apenas no CI por padrão) */
  retries: process.env['CI'] ? 2 : 0,
  /* Número de workers paralelos */
  workers: process.env['CI'] ? 1 : undefined,
  /* Relatórios de execução: lista no terminal + relatório interativo em HTML */
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }]
  ],
  /* Configurações padrão para todas as suítes e navegadores */
  use: {
    /* URL base da aplicação (Angular dev server ou container) */
    baseURL: process.env['BASE_URL'] || 'http://localhost:4200',

    /* Grava rastreamento (trace), screenshots e vídeos em falhas para depuração */
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  /* Navegadores configurados */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /* Sobe automaticamente o servidor Angular antes de rodar os testes, se não estiver rodando */
  webServer: {
    command: 'npm run start',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env['CI'],
    timeout: 120 * 1000,
  },
});
