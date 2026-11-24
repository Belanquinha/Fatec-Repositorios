import { test, expect } from '@playwright/test';

test.describe('Tela de Autenticação (/entrar)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/entrar');
  });

  test('deve renderizar a estrutura inicial com abas de login e cadastro', async ({ page }) => {
    // Valida painel institucional
    await expect(page.locator('.auth-headline')).toBeVisible();

    // Valida aba de login ativa por padrão
    const abaLogin = page.locator('[data-tab="login"]');
    await expect(abaLogin).toBeVisible();
    await expect(abaLogin).toHaveClass(/aba-ativa/);

    // Valida campos do formulário de login
    const inputEmail = page.locator('#login-email');
    const inputSenha = page.locator('#login-senha');
    const botaoSubmit = page.locator('[data-submit-login]');

    await expect(inputEmail).toBeVisible();
    await expect(inputSenha).toBeVisible();
    await expect(botaoSubmit).toBeVisible();
  });

  test('deve alternar entre as abas de Login e Criar conta', async ({ page }) => {
    const abaCadastro = page.locator('[data-tab="cadastro"]');
    await abaCadastro.click();

    await expect(abaCadastro).toHaveClass(/aba-ativa/);
    await expect(page.locator('text=Criar nova conta')).toBeVisible();

    // Volta para login
    const abaLogin = page.locator('[data-tab="login"]');
    await abaLogin.click();
    await expect(abaLogin).toHaveClass(/aba-ativa/);
    await expect(page.locator('#login-email')).toBeVisible();
  });

  test('deve exibir mensagem de erro ao tentar submeter formulário com campos vazios ou inválidos', async ({ page }) => {
    const botaoSubmit = page.locator('[data-submit-login]');

    // Tenta submeter sem preencher
    await botaoSubmit.click();

    // Valida se o alerta de erro ou mensagem de validação é exibida
    const alertaErro = page.locator('.painel-alerta-erro');
    if (await alertaErro.isVisible()) {
      await expect(alertaErro).toBeVisible();
    } else {
      // Caso a validação seja via HTML5:
      const inputEmail = page.locator('#login-email');
      const isValid = await inputEmail.evaluate((el: HTMLInputElement) => el.checkValidity());
      expect(isValid).toBe(false);
    }
  });

  test('deve alternar a visibilidade da senha ao clicar no botão de olho', async ({ page }) => {
    const inputSenha = page.locator('#login-senha');
    const botaoToggleSenha = page.locator('.btn-toggle-senha');

    await expect(inputSenha).toHaveAttribute('type', 'password');
    await botaoToggleSenha.click();
    await expect(inputSenha).toHaveAttribute('type', 'text');
    await botaoToggleSenha.click();
    await expect(inputSenha).toHaveAttribute('type', 'password');
  });
});
