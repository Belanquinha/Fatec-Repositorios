import { test, expect } from '@playwright/test';
import { cadastrarUsuarioReal, resolverBaseUrlApi } from './helpers/auth-helper';
import { limparSessao } from './helpers/sessao-helper';

/**
 * Suíte de Testes Reais: Publicação de Projeto Completo pelo Aluno
 *
 * Valida o ciclo acadêmico integrado de ponta a ponta:
 * 1. Aluno se autentica e acessa /projeto-forms.
 * 2. Seção 1: Preenche dados básicos e faz upload da imagem de capa.
 * 3. Seção 2: Seleciona Unidade Fatec, Curso e indica o Professor Orientador (@cps.sp.gov.br).
 * 4. Seção 3: Interage com o Editor.js testando suas ferramentas (Header, Paragraph, List,
 *    Code, Quote) e insere pelo menos 9 imagens reais com upload no servidor.
 * 5. Seção 4: Adiciona integrantes e submete para validação acadêmica.
 * 6. Perfil do Aluno (/perfil): Confirma que o card do projeto está listado na aba 'Pendentes'.
 * 7. Perfil do Professor: Autentica como o orientador e valida o projeto na fila de pendentes.
 */
test.describe('Publicar Projeto (Aluno) e Validar na Fila do Professor (Docker)', () => {
  // Único por worker: `beforeAll` roda por worker e os e-mails não podem colidir.
  const timestamp = Date.now() * 1000 + Number(process.env.TEST_WORKER_INDEX ?? 0);
  const senhaPadrao = 'SenhaSegura@123';

  let aluno: { email: string; senha: string; nome: string; accessToken: string };
  let professor: { email: string; senha: string; nome: string; accessToken: string };
  let urlsImagensEditor: string[] = [];
  const tituloProjeto = `Plataforma de Visão Computacional e IoT - ${timestamp}`;

  test.beforeAll(async ({ request }) => {
    const baseUrlApi = await resolverBaseUrlApi(request);

    // 1. Cria conta de Aluno real no banco
    const emailAluno = `aluno.autor.${timestamp}@aluno.cps.sp.gov.br`;
    aluno = await cadastrarUsuarioReal(request, 'Mariana Silva Aluna', emailAluno, senhaPadrao);

    // 2. Cria conta de Professor real no banco (@cps.sp.gov.br)
    const emailProf = `prof.orientador.${timestamp}@cps.sp.gov.br`;
    professor = await cadastrarUsuarioReal(request, 'Prof. Dr. Marcos Souza', emailProf, senhaPadrao);

    // 3. Faz o upload antecipado de 9 imagens reais via API com o token do aluno
    const bufferPng = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFUlEQVR42mNk+M9QDwAEhAGgY5wV8QAAAABJRU5ErkJggg==',
      'base64',
    );

    for (let i = 1; i <= 9; i++) {
      const resUpload = await request.post(`${baseUrlApi}/uploads`, {
        headers: {
          Authorization: `Bearer ${aluno.accessToken}`,
        },
        multipart: {
          file: {
            name: `evidencia-figura-${i}.png`,
            mimeType: 'image/png',
            buffer: bufferPng,
          },
        },
      });
      if (!resUpload.ok()) {
        const corpoErro = await resUpload.text().catch(() => '');
        throw new Error(`Falha no upload da imagem ${i} para ${baseUrlApi}/uploads: HTTP ${resUpload.status()} - ${corpoErro}`);
      }
      expect(resUpload.ok()).toBeTruthy();
      const dados = await resUpload.json();
      urlsImagensEditor.push(dados.url);
    }

    expect(urlsImagensEditor.length).toBe(9);
  });

  test.beforeEach(async ({ page }) => {
    await limparSessao(page);
  });

  test('deve submeter o projeto com capa, 9 imagens no editor e verificar no perfil do aluno e professor', async ({ page }) => {
    // -------------------------------------------------------------------------
    // PASSO 1: Login como Aluno
    // -------------------------------------------------------------------------
    await page.goto('/entrar');
    await page.locator('#login-email').fill(aluno.email);
    await page.locator('#login-senha').fill(aluno.senha);
    await page.locator('[data-submit-login]').click();
    await expect(page).toHaveURL('/');

    // -------------------------------------------------------------------------
    // PASSO 2: Acessa o formulário de cadastro de projeto
    // -------------------------------------------------------------------------
    await page.goto('/projeto-forms');
    await expect(page.locator('text=Seção 1: Capa & Informações Básicas')).toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 3: Seção 1 — Capa & Informações Básicas
    // -------------------------------------------------------------------------
    await page.locator('input[name="titulo"]').fill(tituloProjeto);

    // Upload da Imagem de Capa
    const bufferCapa = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFUlEQVR42mNk+M9QDwAEhAGgY5wV8QAAAABJRU5ErkJggg==',
      'base64',
    );
    await page.locator('#capaInput').setInputFiles({
      name: 'capa-residuos-ia.png',
      mimeType: 'image/png',
      buffer: bufferCapa,
    });
    // Aguarda o preview da capa ser renderizado
    await expect(page.locator('img[src^="data:image/"]')).toBeVisible();

    // Descrição Curta (até 144 caracteres)
    await page.locator('textarea[name="descricaoCurta"]').fill(
      'Solução inovadora baseada em visão computacional e IoT para automatizar a triagem de recicláveis nas unidades da Fatec.',
    );

    // Avança para a Seção 2
    await page.locator('button:has-text("Próximo")').click();
    await expect(page.locator('text=Seção 2: Validação Acadêmica')).toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 4: Seção 2 — Instituição, Curso & Professor Orientador
    // -------------------------------------------------------------------------
    // Busca a unidade FATEC
    const inputBuscaInst = page.locator('input[name="buscaInstituicao"]');
    await inputBuscaInst.fill('Fatec');
    await expect(page.locator('#lista-instituicoes')).toBeVisible();
    await page.locator('#lista-instituicoes li').first().click();

    // Confirma que a FATEC foi selecionada e exibe o botão de alterar
    await expect(page.locator('text=Alterar Unidade')).toBeVisible();

    // Seleciona o primeiro curso disponível
    const selectCurso = page.locator('select[name="cursoId"]');
    await expect(selectCurso).toBeEnabled();
    await expect(selectCurso.locator('option')).not.toHaveCount(1);

    const opcoes = selectCurso.locator('option');
    const totalOpcoes = await opcoes.count();
    for (let i = 0; i < totalOpcoes; i++) {
      const valor = await opcoes.nth(i).getAttribute('value');
      if (valor && valor.trim() !== '') {
        await selectCurso.selectOption(valor);
        break;
      }
    }

    // E-mail do Professor Responsável
    await page.locator('input[name="professorEmail"]').fill(professor.email);

    // Link do Repositório e Palavras-chave
    await page.locator('input[name="linkRepositorio"]').fill('https://github.com/fatec/projeto-residuos-ia');
    await page.locator('input[name="palavrasChave"]').fill('Visão Computacional, IA, IoT, Sustentabilidade, TCC Fatec');

    // Avança para a Seção 3
    await page.locator('button:has-text("Próximo")').click();
    await expect(page.locator('text=Seção 3: Conteúdo Detalhado do Projeto')).toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 5: Seção 3 — Editor.js com todas as funções + 9 Imagens
    // -------------------------------------------------------------------------
    await expect(page.locator('#editorjs-container .codex-editor')).toBeVisible();

    // A instância fica disponível em `window.__editorjs` só depois de criada
    // (e antes de `editor.isReady`). Em execução paralela isso pode levar mais
    // que o render do container, então esperamos a instância de forma
    // explícita em vez de confiar no DOM.
    await page.waitForFunction(
      () => Boolean((window as any).__editorjs?.blocks),
      null,
      { timeout: 15000 },
    );

    // Insere os blocos no Editor.js testando todas as ferramentas disponíveis
    await page.evaluate(async (imagens) => {
      const editor = (window as any).__editorjs;
      if (!editor) {
        throw new Error('Instância do Editor.js não encontrada');
      }

      // 1. Função Header (Cabeçalho H2)
      await editor.blocks.insert('header', {
        text: '1. Introdução e Contextualização do Projeto',
        level: 2,
      });

      // 2. Função Parágrafo (Texto descritivo)
      await editor.blocks.insert('paragraph', {
        text: 'Este trabalho acadêmico desenvolve um pipeline de detecção em tempo real para classificação e triagem de materiais descartados.',
      });

      // 3. Função Lista (List)
      await editor.blocks.insert('list', {
        style: 'unordered',
        items: [
          'Coleta do dataset fotográfico em laboratório da Fatec',
          'Treinamento do modelo convolucional com TensorFlow',
          'Integração dos atuadores eletromecânicos via microcontrolador ESP32',
        ],
      });

      // 4. Função Código (Code)
      await editor.blocks.insert('code', {
        code: 'def classificar_residuo(imagem_stream):\n    modelo = carregar_modelo_tcc()\n    return modelo.predict(imagem_stream)',
      });

      // 5. Função Citação (Quote)
      await editor.blocks.insert('quote', {
        text: 'A sustentabilidade aliada à inteligência artificial gera impacto direto nas cidades inteligentes.',
        caption: 'Referencial Teórico - Fatec',
      });

      // 6. Função Imagem (Image) — Inserção de 9 imagens reais com legendas
      for (let i = 0; i < imagens.length; i++) {
        await editor.blocks.insert('image', {
          file: {
            url: imagens[i],
          },
          caption: `Figura ${i + 1}: Evidência Experimental e Testes de Campo (${i + 1} de 9)`,
          withBorder: false,
          withBackground: false,
          stretched: false,
        });
      }
    }, urlsImagensEditor);

    // Valida que os blocos e as 9 imagens foram visualmente renderizados na tela
    await expect(page.locator('#editorjs-container h2')).toBeVisible();
    await expect(page.locator('#editorjs-container ul.cdx-list')).toBeVisible();
    await expect(page.locator('#editorjs-container .cdx-block.ce-code')).toBeVisible();
    await expect(page.locator('#editorjs-container .cdx-quote')).toBeVisible();
    await expect(page.locator('#editorjs-container .image-tool')).toHaveCount(9);

    // Avança para a Seção 4
    await page.locator('button:has-text("Próximo")').click();
    await expect(page.locator('text=Seção 4: Integrantes do Projeto')).toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 6: Seção 4 — Integrantes & Submissão Acadêmica
    // -------------------------------------------------------------------------
    await page.locator('input[name="nome_0"]').fill('Mariana Silva Aluna');
    await page.locator('input[name="linkedin_0"]').fill('https://linkedin.com/in/mariana-silva-fatec');

    // Adiciona o segundo integrante
    await page.locator('button:has-text("Adicionar Integrante")').click();
    await page.locator('input[name="nome_1"]').fill('Lucas Ferreira de Castro');
    await page.locator('input[name="linkedin_1"]').fill('https://linkedin.com/in/lucas-castro-fatec');

    // Clica para enviar para a avaliação acadêmica
    await page.locator('button:has-text("Enviar para Avaliação Acadêmica")').click();

    // Valida tela de sucesso pós-envio
    await expect(page.locator('text=Projeto Enviado para Avaliação!')).toBeVisible({ timeout: 15000 });

    // -------------------------------------------------------------------------
    // PASSO 7: Validação na Tela de Perfil do ALUNO (/perfil)
    // -------------------------------------------------------------------------
    await page.locator('button:has-text("Ver meus projetos")').click();
    await expect(page).toHaveURL('/perfil');

    // Clica na aba 'Pendentes'
    await page.locator('button[role="tab"]:has-text("Pendentes")').click();

    // Valida que o card do projeto está presente na tela do Aluno
    const cardProjetoAluno = page.locator('.card-titulo', { hasText: tituloProjeto });
    await expect(cardProjetoAluno).toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 8: Validação na Tela de Perfil do PROFESSOR (/perfil)
    // -------------------------------------------------------------------------
    // Desloga o Aluno: a limpeza precisa acontecer ANTES do `goto`. Indo para
    // /entrar ainda logado, o `visitanteGuard` redireciona para `/` e o
    // formulário de login nunca chega a renderizar.
    await page.evaluate(() => localStorage.clear());
    await page.goto('/entrar');

    // Login com o Professor Orientador
    await page.locator('#login-email').fill(professor.email);
    await page.locator('#login-senha').fill(professor.senha);
    await page.locator('[data-submit-login]').click();
    await expect(page).toHaveURL('/');

    // Acessa o perfil do Professor
    await page.goto('/perfil');

    // Na aba 'Pendentes', verifica a fila de projetos aguardando avaliação
    await page.locator('button[role="tab"]:has-text("Pendentes")').click();

    const cardProjetoProf = page.locator('.card-titulo', { hasText: tituloProjeto });
    await expect(cardProjetoProf).toBeVisible();
  });
});
