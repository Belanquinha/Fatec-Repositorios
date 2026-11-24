import { test, expect } from '@playwright/test';
import { cadastrarUsuarioReal } from './helpers/auth-helper';
import { criarProjetoPendenteReal } from './helpers/projeto-helper';
import { limparSessao } from './helpers/sessao-helper';

/**
 * Suíte de Testes Reais: Avaliação de Projetos pelo Professor (Docker + Spring Boot)
 *
 * Cobre os fluxos acadêmicos de revisão:
 * 1. Login com conta de Professor (@cps.sp.gov.br).
 * 2. Navegação até a tela de perfil (/perfil) e consulta da fila de 'Pendentes'.
 * 3. Acesso à tela de detalhes e avaliação acadêmica (/ver-projeto/:id?avaliar=1).
 * 4. Aprovação (Aceite) do projeto com publicação na vitrine e verificação na aba 'Aprovados'.
 * 5. Rejeição do projeto com preenchimento obrigatório de justificativa e remoção da fila.
 */
test.describe('Avaliar Projeto — Professor (Aprovar e Rejeitar)', () => {
  // Único por worker: `beforeAll` roda por worker e os e-mails não podem colidir.
  const timestamp = Date.now() * 1000 + Number(process.env.TEST_WORKER_INDEX ?? 0);
  const senhaPadrao = 'SenhaSegura@123';

  let aluno: { email: string; senha: string; nome: string; accessToken: string };
  let professor: { email: string; senha: string; nome: string; accessToken: string };

  const tituloProjetoAprovar = `Sistema de Monitoramento com Visão Computacional - ${timestamp}`;
  const tituloProjetoRejeitar = `Plataforma de Otimização Logística e Redes - ${timestamp}`;

  test.beforeAll(async ({ request }) => {
    // 1. Cria conta de Aluno real no banco para submissão dos projetos
    const emailAluno = `aluno.avaliar.${timestamp}@aluno.cps.sp.gov.br`;
    aluno = await cadastrarUsuarioReal(request, 'Lucas Mendes Aluno', emailAluno, senhaPadrao);

    // 2. Cria conta de Professor real no banco (@cps.sp.gov.br) para avaliar
    const emailProf = `prof.avaliador.${timestamp}@cps.sp.gov.br`;
    professor = await cadastrarUsuarioReal(request, 'Prof. Dra. Camila Rocha', emailProf, senhaPadrao);

    // 3. Cria dois projetos acadêmicos pendentes indicando este orientador
    await criarProjetoPendenteReal(request, aluno.accessToken, professor.email, tituloProjetoAprovar);
    await criarProjetoPendenteReal(request, aluno.accessToken, professor.email, tituloProjetoRejeitar);
  });

  test.beforeEach(async ({ page }) => {
    await limparSessao(page);
  });

  test('deve realizar login como professor, acessar perfil, abrir ver projeto e aprovar (aceitar)', async ({ page }) => {
    // -------------------------------------------------------------------------
    // PASSO 1: Login como Professor
    // -------------------------------------------------------------------------
    await page.goto('/entrar');
    await page.locator('#login-email').fill(professor.email);
    await page.locator('#login-senha').fill(professor.senha);
    await page.locator('[data-submit-login]').click();
    await expect(page).toHaveURL('/');

    // -------------------------------------------------------------------------
    // PASSO 2: Acessa o perfil do Professor (/perfil)
    // -------------------------------------------------------------------------
    await page.goto('/perfil');
    await expect(page).toHaveURL('/perfil');

    // -------------------------------------------------------------------------
    // PASSO 3: Acessa a aba 'Pendentes' e localiza o projeto
    // -------------------------------------------------------------------------
    const abaPendentes = page.locator('button[role="tab"]:has-text("Pendentes")');
    await expect(abaPendentes).toBeVisible();
    await abaPendentes.click();

    const cardProjeto = page.locator('.card-projeto', {
      has: page.locator('.card-titulo', { hasText: tituloProjetoAprovar }),
    });
    await expect(cardProjeto).toBeVisible({ timeout: 10000 });

    // -------------------------------------------------------------------------
    // PASSO 4: Clica em 'Ver Projeto' para entrar em modo de avaliação (?avaliar=1)
    // -------------------------------------------------------------------------
    const botaoVerProjeto = cardProjeto.locator('a:has-text("Ver Projeto")');
    await expect(botaoVerProjeto).toBeVisible();
    await botaoVerProjeto.click();

    // Valida carregamento da página de detalhes com query param de avaliação
    await expect(page).toHaveURL(new RegExp('/ver-projeto/.*avaliar=1'));
    await expect(page.locator('.avaliacao-painel')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('#avaliacao-titulo')).toHaveText('Avaliação do projeto');

    // -------------------------------------------------------------------------
    // PASSO 5: Aprova (Aceita) o projeto acadêmico
    // -------------------------------------------------------------------------
    const botaoAprovar = page.locator('.btn-aprovar:has-text("Aprovar projeto")');
    await expect(botaoAprovar).toBeVisible();
    await botaoAprovar.click();

    // -------------------------------------------------------------------------
    // PASSO 6: Valida mensagem de sucesso e confirmação
    // -------------------------------------------------------------------------
    const alertaSucesso = page.locator('.avaliacao-sucesso');
    await expect(alertaSucesso).toBeVisible({ timeout: 10000 });
    await expect(alertaSucesso).toContainText('aprovado e publicado na vitrine');

    // -------------------------------------------------------------------------
    // PASSO 7: Retorna ao perfil e confirma que o projeto saiu da fila de
    // pendentes do professor.
    //
    // A aba "Aprovados" do professor (`/projetos/meus` filtrado por APROVADO)
    // lista apenas projetos submetidos pelo próprio usuário, e quem submete é
    // o aluno — o orientador nunca aparece como autor. Então a verificação
    // possível hoje é a saída da fila, que é o efeito real da aprovação.
    // -------------------------------------------------------------------------
    await page.locator('a.btn-voltar-perfil').click();
    await expect(page).toHaveURL('/perfil');

    await page.locator('button[role="tab"]:has-text("Pendentes")').click();

    const cardAindaPendente = page.locator('.card-projeto', {
      has: page.locator('.card-titulo', { hasText: tituloProjetoAprovar }),
    });
    await expect(cardAindaPendente).toHaveCount(0);
  });

  test('deve realizar login como professor, acessar perfil, abrir ver projeto e rejeitar com justificativa', async ({ page }) => {
    // -------------------------------------------------------------------------
    // PASSO 1: Login como Professor
    // -------------------------------------------------------------------------
    await page.goto('/entrar');
    await page.locator('#login-email').fill(professor.email);
    await page.locator('#login-senha').fill(professor.senha);
    await page.locator('[data-submit-login]').click();
    await expect(page).toHaveURL('/');

    // -------------------------------------------------------------------------
    // PASSO 2: Acessa o perfil do Professor (/perfil) e aba 'Pendentes'
    // -------------------------------------------------------------------------
    await page.goto('/perfil');
    await page.locator('button[role="tab"]:has-text("Pendentes")').click();

    const cardProjeto = page.locator('.card-projeto', {
      has: page.locator('.card-titulo', { hasText: tituloProjetoRejeitar }),
    });
    await expect(cardProjeto).toBeVisible({ timeout: 10000 });

    // -------------------------------------------------------------------------
    // PASSO 3: Clica em 'Ver Projeto'
    // -------------------------------------------------------------------------
    await cardProjeto.locator('a:has-text("Ver Projeto")').click();
    await expect(page).toHaveURL(new RegExp('/ver-projeto/.*avaliar=1'));
    await expect(page.locator('.avaliacao-painel')).toBeVisible({ timeout: 10000 });

    // -------------------------------------------------------------------------
    // PASSO 4: Clica em 'Rejeitar' para abrir o campo de justificativa
    // -------------------------------------------------------------------------
    const botaoRejeitar = page.locator('.btn-rejeitar:has-text("Rejeitar")');
    await expect(botaoRejeitar).toBeVisible();
    await botaoRejeitar.click();

    const campoMotivo = page.locator('#motivo-rejeicao');
    await expect(campoMotivo).toBeVisible();

    const motivoJustificativa = 'Projeto precisa aprofundar a metodologia experimental e adicionar mais referências bibliográficas atualizadas.';
    await campoMotivo.fill(motivoJustificativa);

    // -------------------------------------------------------------------------
    // PASSO 5: Confirma a rejeição
    // -------------------------------------------------------------------------
    const botaoConfirmarRejeicao = page.locator('.avaliacao-rejeicao .btn-aprovar:has-text("Confirmar rejeição")');
    await expect(botaoConfirmarRejeicao).toBeVisible();
    await botaoConfirmarRejeicao.click();

    // -------------------------------------------------------------------------
    // PASSO 6: Valida mensagem de sucesso de rejeição
    // -------------------------------------------------------------------------
    const alertaSucesso = page.locator('.avaliacao-sucesso');
    await expect(alertaSucesso).toBeVisible({ timeout: 10000 });
    await expect(alertaSucesso).toContainText('rejeitado com retorno ao aluno');

    // -------------------------------------------------------------------------
    // PASSO 7: Retorna ao perfil e valida que o projeto não consta mais nos pendentes
    // -------------------------------------------------------------------------
    await page.locator('a.btn-voltar-perfil').click();
    await expect(page).toHaveURL('/perfil');

    await page.locator('button[role="tab"]:has-text("Pendentes")').click();
    const cardRejeitadoNaLista = page.locator('.card-projeto', {
      has: page.locator('.card-titulo', { hasText: tituloProjetoRejeitar }),
    });
    await expect(cardRejeitadoNaLista).not.toBeVisible();
  });
});
