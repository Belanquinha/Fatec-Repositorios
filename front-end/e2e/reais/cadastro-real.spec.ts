import { test, expect } from '@playwright/test';
import { obterCodigoVerificacaoDocker } from './helpers/docker-helper';
import { limparSessao } from './helpers/sessao-helper';

/**
 * Suíte de Testes Reais de Cadastro com Back-end e Banco (Docker)
 *
 * Esta suíte se comunica de verdade com o Spring Boot e o PostgreSQL:
 * 1. O formulário é enviado para a API real.
 * 2. O back-end dev gera o código de 6 dígitos e registra no log do container fatec-backend.
 * 3. O teste captura o código real do container e submete na tela.
 * 4. O usuário é provisionado no banco de dados e autenticado.
 */
test.describe('Cadastro Real com Back-end e Banco de Dados (Docker)', () => {
  // E-mail único por worker: os testes rodam em paralelo e o cadastro tem
  // cooldown de 60s por e-mail no back-end (HTTP 429 em duplicidade).
  const timestamp = Date.now() * 1000 + Number(process.env.TEST_WORKER_INDEX ?? 0);

  test.beforeEach(async ({ page }) => {
    await limparSessao(page);
  });

  test('deve cadastrar um novo ALUNO de ponta a ponta com código real do Docker', async ({ page }) => {
    const emailAluno = `aluno.e2e.${timestamp}@aluno.cps.sp.gov.br`;
    const senhaForte = 'SenhaSegura@123';
    const nomeCompleto = 'Aluno Teste E2E Real';

    await page.goto('/entrar?modo=cadastro');

    // 1. Preenche dados cadastrais
    await page.locator('#cadastro-nome').fill(nomeCompleto);
    await page.locator('#cadastro-email').fill(emailAluno);
    await page.locator('#cadastro-senha').fill(senhaForte);
    await page.locator('#cadastro-confirmar-senha').fill(senhaForte);

    // 2. Envia para o back-end real gerar o código
    await page.locator('[data-submit-signup-init]').click();

    // 3. Valida transição para a etapa do código OTP
    await expect(page.locator('.email-destaque')).toContainText(emailAluno);
    const inputsOtp = page.locator('.otp-input');
    await expect(inputsOtp).toHaveCount(6);

    // 4. Captura o código de 6 dígitos gerado pelo Spring Boot nos logs do Docker
    const codigoReal = await obterCodigoVerificacaoDocker(emailAluno);
    expect(codigoReal).toMatch(/^\d{6}$/);

    // 5. Preenche os 6 dígitos na interface
    for (let i = 0; i < 6; i++) {
      await inputsOtp.nth(i).fill(codigoReal[i]);
    }

    // 6. Confirma o código no back-end
    await page.locator('[data-submit-signup-confirm]').click();

    // 7. Valida redirecionamento e criação de sessão real
    await expect(page).toHaveURL('/');

    const token = await page.evaluate(() => localStorage.getItem('accessToken'));
    const role = await page.evaluate(() => localStorage.getItem('usuarioRole'));
    const emailGravado = await page.evaluate(() => localStorage.getItem('usuarioEmail'));

    expect(token).toBeTruthy();
    expect(role).toBe('ALUNO');
    expect(emailGravado).toBe(emailAluno);
  });

  test('deve cadastrar um PROFESSOR pelo domínio @cps.sp.gov.br com papel correto', async ({ page }) => {
    const emailProf = `prof.e2e.${timestamp}@cps.sp.gov.br`;
    const senhaForte = 'DocenteSeguro@123';

    await page.goto('/entrar?modo=cadastro');

    await page.locator('#cadastro-nome').fill('Professor Teste Real');
    await page.locator('#cadastro-email').fill(emailProf);
    await page.locator('#cadastro-senha').fill(senhaForte);
    await page.locator('#cadastro-confirmar-senha').fill(senhaForte);

    await page.locator('[data-submit-signup-init]').click();

    await expect(page.locator('.email-destaque')).toContainText(emailProf);
    const inputsOtp = page.locator('.otp-input');

    // Captura o código real do Docker
    const codigoReal = await obterCodigoVerificacaoDocker(emailProf);

    for (let i = 0; i < 6; i++) {
      await inputsOtp.nth(i).fill(codigoReal[i]);
    }

    await page.locator('[data-submit-signup-confirm]').click();

    await expect(page).toHaveURL('/');

    // Valida que o back-end concedeu papel de PROFESSOR devido ao domínio institucional
    const role = await page.evaluate(() => localStorage.getItem('usuarioRole'));
    expect(role).toBe('PROFESSOR');
  });

  test('deve exibir erro real do back-end ao informar código incorreto', async ({ page }) => {
    const email = `teste.falha.${timestamp}@exemplo.com`;

    await page.goto('/entrar?modo=cadastro');

    await page.locator('#cadastro-nome').fill('Usuario Codigo Invalido');
    await page.locator('#cadastro-email').fill(email);
    await page.locator('#cadastro-senha').fill('SenhaValida123!');
    await page.locator('#cadastro-confirmar-senha').fill('SenhaValida123!');

    await page.locator('[data-submit-signup-init]').click();

    await expect(page.locator('.email-destaque')).toContainText(email);
    const inputsOtp = page.locator('.otp-input');

    // Preenche um código deliberadamente incorreto
    for (let i = 0; i < 6; i++) {
      await inputsOtp.nth(i).fill('0');
    }

    await page.locator('[data-submit-signup-confirm]').click();

    // Valida a mensagem de erro retornada pela API real (informa que o código está incorreto e as tentativas restantes)
    const alertaErro = page.locator('.painel-alerta-erro');
    await expect(alertaErro).toBeVisible();
    await expect(alertaErro).toContainText('Código incorreto');
  });
});
