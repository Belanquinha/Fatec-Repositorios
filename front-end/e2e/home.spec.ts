import { test, expect } from '@playwright/test';

test.describe('Página Inicial / Catálogo Público', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('deve carregar a página inicial com cabeçalho e título da aplicação', async ({ page }) => {
    // Verifica título ou elemento de marca no cabeçalho
    const logoTexto = page.locator('header a', { hasText: 'Fatec Repositório' });
    await expect(logoTexto).toBeVisible();

    // Verifica presença do botão de entrar para usuário deslogado
    const botaoEntrar = page.locator('[data-header-login]');
    await expect(botaoEntrar).toBeVisible();
    await expect(botaoEntrar).toHaveText(/Entrar/);
  });

  test('deve exibir e permitir fechar o banner de cookies (LGPD)', async ({ page }) => {
    const banner = page.locator('[data-cookie-banner]');
    // Se o banner estiver visível na primeira visita:
    if (await banner.isVisible()) {
      await expect(banner).toContainText('Privacidade e uso de cookies');
      const botaoAceitar = page.locator('[data-cookie-accept]');
      await expect(botaoAceitar).toBeVisible();

      await botaoAceitar.click();
      await expect(banner).not.toBeVisible();
    }
  });

  test('deve permitir alternar entre tema claro e escuro', async ({ page }) => {
    const toggleTema = page.locator('[data-header-theme-toggle]');
    await expect(toggleTema).toBeVisible();

    // Verifica estado inicial do tema
    const htmlElement = page.locator('html');
    const estavaEscuro = await htmlElement.evaluate((el) => el.classList.contains('dark'));

    // Clica para alternar
    await toggleTema.click();

    // Verifica se o estado da classe dark mudou
    if (estavaEscuro) {
      await expect(htmlElement).not.toHaveClass(/dark/);
    } else {
      await expect(htmlElement).toHaveClass(/dark/);
    }
  });

  test('deve navegar para a página de autenticação ao clicar em Entrar', async ({ page }) => {
    const botaoEntrar = page.locator('[data-header-login]');
    await botaoEntrar.click();

    await expect(page).toHaveURL(/\/entrar/);
  });
});
