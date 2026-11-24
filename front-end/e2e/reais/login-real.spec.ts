import { test, expect } from '@playwright/test';
import { cadastrarUsuarioReal } from './helpers/auth-helper';

/**
 * Suíte de Testes Reais de Login com Back-end e Banco (Docker)
 *
 * Utiliza o back-end Spring Boot e banco PostgreSQL reais:
 * - Cria um usuário de teste real no banco antes de rodar os testes.
 * - Testa a autenticação real por senha (BCrypt) e emissão de JWT.
 * - Valida respostas de erro reais de credenciais inválidas.
 */
test.describe('Login Real com Back-end e Banco de Dados (Docker)', () => {
  // Único por worker: `beforeAll` roda uma vez por worker e, se dois workers
  // importarem o arquivo no mesmo milissegundo, o e-mail colide e a segunda
  // chamada cai no cooldown de 60s do back-end (HTTP 429).
  const timestamp = Date.now() * 1000 + Number(process.env.TEST_WORKER_INDEX ?? 0);
  const emailLoginValido = `usuario.login.${timestamp}@aluno.cps.sp.gov.br`;
  const senhaCorreta = 'MinhaSenhaSegura123!';
  const nomeUsuario = 'Usuario Login Teste Real';

  // Cadastra um usuário real no banco antes de iniciar os testes de login
  test.beforeAll(async ({ request }) => {
    await cadastrarUsuarioReal(request, nomeUsuario, emailLoginValido, senhaCorreta);
  });

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.clear();
    });
  });

  test('deve realizar login com sucesso usando credenciais reais gravadas no Postgres', async ({ page }) => {
    await page.goto('/entrar');

    await page.locator('#login-email').fill(emailLoginValido);
    await page.locator('#login-senha').fill(senhaCorreta);

    await page.locator('[data-submit-login]').click();

    // Redireciona para a página inicial
    await expect(page).toHaveURL('/');

    // Valida que o token JWT real e o e-mail foram armazenados na sessão
    const token = await page.evaluate(() => localStorage.getItem('accessToken'));
    const emailStorage = await page.evaluate(() => localStorage.getItem('usuarioEmail'));
    const roleStorage = await page.evaluate(() => localStorage.getItem('usuarioRole'));

    expect(token).toBeTruthy();
    expect(emailStorage).toBe(emailLoginValido);
    expect(roleStorage).toBe('ALUNO');
  });

  test('deve exibir erro real do back-end quando a senha estiver errada (401)', async ({ page }) => {
    await page.goto('/entrar');

    await page.locator('#login-email').fill(emailLoginValido);
    await page.locator('#login-senha').fill('SenhaIncorreta999!');

    await page.locator('[data-submit-login]').click();

    const alertaErro = page.locator('.painel-alerta-erro');
    await expect(alertaErro).toBeVisible();
    await expect(alertaErro).toContainText('E-mail ou senha inválidos');
  });

  test('deve exibir erro real do back-end ao tentar logar com e-mail inexistente', async ({ page }) => {
    await page.goto('/entrar');

    await page.locator('#login-email').fill('conta.inexistente.999@naoexiste.com');
    await page.locator('#login-senha').fill('QualquerSenha123');

    await page.locator('[data-submit-login]').click();

    const alertaErro = page.locator('.painel-alerta-erro');
    await expect(alertaErro).toBeVisible();
    await expect(alertaErro).toContainText('E-mail ou senha inválidos');
  });
});
