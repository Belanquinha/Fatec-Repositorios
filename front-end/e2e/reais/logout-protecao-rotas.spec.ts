import { test, expect } from '@playwright/test';
import { cadastrarUsuarioReal } from './helpers/auth-helper';
import { limparSessao } from './helpers/sessao-helper';

/**
 * Suíte de Testes Reais: Logout e Proteção de Rotas com AuthGuard
 *
 * Cobre os fluxos de segurança e encerramento de sessão:
 * 1. Logout via Menu Lateral (Drawer): limpeza do localStorage e retorno ao estado público.
 * 2. Proteção de Rotas: garantia de que acessos sem sessão ativa a rotas restritas (/perfil e /projeto-forms)
 *    são bloqueados pelo authGuard e redirecionados para /entrar com returnUrl.
 * 3. Fluxo de retorno pós-login: confirma que após autenticar-se na tela de redirecionamento,
 *    o usuário é levado automaticamente ao destino protegido original.
 */
test.describe('Logout e Proteção de Rotas (AuthGuard)', () => {
  // Único por worker: `beforeAll` roda por worker e o e-mail não pode colidir.
  const timestamp = Date.now() * 1000 + Number(process.env.TEST_WORKER_INDEX ?? 0);
  const senhaPadrao = 'SenhaSegura@123';

  let usuario: { email: string; senha: string; nome: string; accessToken: string };

  test.beforeAll(async ({ request }) => {
    // Cria conta real no banco para testar login, logout e rotas protegidas
    const emailUsuario = `usuario.rotas.${timestamp}@aluno.cps.sp.gov.br`;
    usuario = await cadastrarUsuarioReal(request, 'Carlos Silva', emailUsuario, senhaPadrao);
  });

  test.beforeEach(async ({ page }) => {
    await limparSessao(page);
  });

  test('deve realizar logout via menu lateral, limpar tokens da sessão e exibir estado deslogado', async ({ page }) => {
    // -------------------------------------------------------------------------
    // PASSO 1: Realiza login na aplicação
    // -------------------------------------------------------------------------
    await page.goto('/entrar');
    await page.locator('#login-email').fill(usuario.email);
    await page.locator('#login-senha').fill(usuario.senha);
    await page.locator('[data-submit-login]').click();
    await expect(page).toHaveURL('/');

    // Confirma que o usuário está reconhecido no cabeçalho
    await expect(page.locator('header a[href="/perfil"]')).toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 2: Abre o menu lateral (Drawer)
    // -------------------------------------------------------------------------
    const botaoMenu = page.locator('header [data-menu-toggle]');
    await botaoMenu.click();

    const menuLateral = page.locator('#menu-lateral');
    await expect(menuLateral).toHaveClass(/menu-aberto/);

    // -------------------------------------------------------------------------
    // PASSO 3: Clica no botão 'Sair' (Logout)
    // -------------------------------------------------------------------------
    const botaoSair = menuLateral.locator('button:has-text("Sair")');
    await expect(botaoSair).toBeVisible();
    await botaoSair.click();

    // -------------------------------------------------------------------------
    // PASSO 4: Valida redirecionamento para Home e atualização do Header
    // -------------------------------------------------------------------------
    await expect(page).toHaveURL('/');

    // Botão de login volta a ser exibido no header
    const botaoEntrarHeader = page.locator('header [data-header-login]');
    await expect(botaoEntrarHeader).toBeVisible();

    // O link do perfil não deve mais existir no cabeçalho
    await expect(page.locator('header a[href="/perfil"]')).not.toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 5: Garante que os dados da sessão foram descartados do localStorage
    // -------------------------------------------------------------------------
    const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
    const usuarioNome = await page.evaluate(() => localStorage.getItem('usuarioNome'));
    expect(accessToken).toBeNull();
    expect(usuarioNome).toBeNull();
  });

  test('deve bloquear acesso direto a rotas protegidas sem sessão e redirecionar para /entrar com returnUrl', async ({ page }) => {
    // Garante que a sessão está vazia
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());

    // -------------------------------------------------------------------------
    // Teste 1: Tentativa de acessar /perfil sem sessão
    // -------------------------------------------------------------------------
    await page.goto('/perfil');

    // Valida que o authGuard interceptou e redirecionou para /entrar com o returnUrl codificado
    await expect(page).toHaveURL(/\/entrar\?returnUrl=%2Fperfil/);
    await expect(page.locator('#login-email')).toBeVisible();
    await expect(page.locator('[data-submit-login]')).toBeVisible();

    // -------------------------------------------------------------------------
    // Teste 2: Tentativa de acessar /projeto-forms sem sessão
    // -------------------------------------------------------------------------
    await page.goto('/projeto-forms');

    // Valida redirecionamento de /projeto-forms
    await expect(page).toHaveURL(/\/entrar\?returnUrl=%2Fprojeto-forms/);
    await expect(page.locator('#login-email')).toBeVisible();
    await expect(page.locator('[data-submit-login]')).toBeVisible();

    // -------------------------------------------------------------------------
    // Teste 3: Login na tela redirecionada deve levar de volta ao destino pretendido
    // -------------------------------------------------------------------------
    await page.locator('#login-email').fill(usuario.email);
    await page.locator('#login-senha').fill(usuario.senha);
    await page.locator('[data-submit-login]').click();

    // Como o returnUrl era /projeto-forms, o usuário deve ser redirecionado diretamente para lá
    await expect(page).toHaveURL('/projeto-forms');
  });
});
