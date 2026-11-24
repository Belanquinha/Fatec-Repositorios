import { test, expect } from '@playwright/test';

test.describe('Navegação e Menu Lateral', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('deve abrir e fechar o menu lateral drawer', async ({ page }) => {
    const botaoMenu = page.locator('header [data-menu-toggle]');
    const menuLateral = page.locator('#menu-lateral');

    // Menu começa fechado
    await expect(menuLateral).not.toHaveClass(/menu-aberto/);

    // Clica para abrir
    await botaoMenu.click();
    await expect(menuLateral).toHaveClass(/menu-aberto/);

    // Clica no botão de fechar dentro do drawer
    const botaoFechar = menuLateral.locator('[data-menu-toggle]');
    await botaoFechar.click();
    await expect(menuLateral).not.toHaveClass(/menu-aberto/);
  });

  test('deve navegar para a página de Instituições pelo menu lateral', async ({ page }) => {
    const botaoMenu = page.locator('header [data-menu-toggle]');
    await botaoMenu.click();

    const linkInstituicoes = page.locator('#menu-lateral a[routerLink="/instituicoes"]');
    await expect(linkInstituicoes).toBeVisible();
    await linkInstituicoes.click();

    await expect(page).toHaveURL(/\/instituicoes/);
  });
});
