import { test, expect } from '@playwright/test';
import { cadastrarUsuarioReal, loginUsuarioReal, resolverBaseUrlApi } from './helpers/auth-helper';
import { criarProjetoPendenteReal } from './helpers/projeto-helper';
import { limparSessao } from './helpers/sessao-helper';

/**
 * Suíte de Testes Reais: Catálogo Público e Catálogo de Instituições (Docker + Spring Boot)
 *
 * Tudo bate no back-end e no PostgreSQL reais — nenhum mock:
 *
 * CT12 (funcionalidades C1 a C5): vitrine pública em /catalogo-publico.
 *   C1 listagem do projeto aprovado · C2 busca por termo · C3 filtro por tag ·
 *   C4 limpar filtros · C5 card navega para /ver-projeto/:id.
 *
 * CT13 (funcionalidades C6 a C10): catálogo de Fatecs em /instituicoes.
 *   C6 cards de Fatec · C7 busca por nome/cidade/código · C8 filtro por região ·
 *   C9 perfil da instituição com "Cursos Oferecidos" · C10 curso filtra
 *   "Projetos Publicados" e o clique na mesma opção limpa o filtro.
 */
test.describe('Catálogo Público e Instituições (Docker + Back-end real)', () => {
  // Único por worker: os testes rodam em paralelo (`fullyParallel`) e, com um
  // timestamp compartilhado, os dois workers gerariam o mesmo e-mail e um
  // cadastro colidiria com o outro.
  const timestamp = Date.now() * 1000 + Number(process.env.TEST_WORKER_INDEX ?? 0);
  const senhaPadrao = 'SenhaSegura@123';

  let aluno: { email: string; senha: string; nome: string; accessToken: string };
  let professor: { email: string; senha: string; nome: string; accessToken: string };

  // Palavras-chave únicas deste teste: garantem que o filtro por tag (C3) isole
  // exatamente o projeto criado aqui, sem depender de outro registro do acervo.
  const tagExclusiva = `CatalogoCT${timestamp}`;
  const tituloProjetoCatalogo = `Vitrine Publica Catalogo Instituicoes - ${timestamp}`;

  // Unidade usada em CT13: "Fatec São Paulo" tem projetos aprovados em alguns
  // cursos e nenhum em outros, então o filtro por curso (C10) realmente muda a
  // lista. O projeto de CT12 também nasce nela, para os dois casos conversarem.
  const nomeUnidadeAlvo = 'Fatec São Paulo';
  const tituloProjetoInstituicao = `Portal de Projetos da unidade - ${timestamp}`;

  test.beforeAll(async ({ request }) => {
    const baseUrlApi = await resolverBaseUrlApi(request);

    // 1. Cria conta de Aluno real no banco (autor da submissão)
    const emailAluno = `aluno.catalogo.${timestamp}@aluno.cps.sp.gov.br`;
    aluno = await cadastrarUsuarioReal(request, 'Larissa Campos Aluna', emailAluno, senhaPadrao);

    // 2. Cria conta de Professor real no banco (@cps.sp.gov.br), orientador responsável
    const emailProf = `prof.catalogo.${timestamp}@cps.sp.gov.br`;
    professor = await cadastrarUsuarioReal(request, 'Prof. Dr. Ricardo Nogueira', emailProf, senhaPadrao);

    // 3. Cria o projeto já com a tag exclusiva usada no filtro (C3) e uma
    // descrição curta que também é termo de busca válido.
    const projeto = await criarProjetoPendenteReal(request, aluno.accessToken, professor.email, {
      titulo: tituloProjetoCatalogo,
      palavrasChave: [tagExclusiva],
      nomeUnidade: nomeUnidadeAlvo,
    });

    // 4. Um segundo projeto aprovado na Fatec São Paulo em OUTRO curso: é o que
    // faz o filtro por curso (C10) mudar de fato a lista de "Projetos Publicados".
    const resUnidade = await request.get(`${baseUrlApi}/instituicoes`);
    const unidadeAlvo = (await resUnidade.json()).find(
      (i: { id: string; nome: string }) => i.nome === nomeUnidadeAlvo,
    );
    expect(unidadeAlvo).toBeTruthy();
    const cursosDaUnidade = await (
      await request.get(`${baseUrlApi}/instituicoes/${unidadeAlvo.id}/cursos`)
    ).json();

    const projetoInstituicao = await criarProjetoPendenteReal(request, aluno.accessToken, professor.email, {
      titulo: tituloProjetoInstituicao,
      palavrasChave: ['Catalogo', 'Instituicoes'],
      instituicaoId: unidadeAlvo.id,
      cursoId: cursosDaUnidade[1].id,
    });

    // 5. Aprova os dois projetos pelo fluxo real do professor: POST /auth/login
    // (BCrypt + JWT) e PATCH /projetos/{id}/aprovar — o mesmo endpoint que o
    // botão "Aprovar projeto" da tela de avaliação chama. O fluxo completo de
    // UI está coberto por avaliar-projeto.spec.ts; aqui o beforeAll fica no
    // nível de API, que é o estado que CT12/CT13 precisam (projeto APROVADO).
    const tokenProfessor = await loginUsuarioReal(request, professor.email, senhaPadrao);

    for (const alvo of [projeto, projetoInstituicao]) {
      const resAprovacao = await request.patch(`${baseUrlApi}/projetos/${alvo.id}/aprovar`, {
        headers: { Authorization: `Bearer ${tokenProfessor}` },
        data: {},
      });
      expect(resAprovacao.ok()).toBeTruthy();
      expect((await resAprovacao.json()).estado).toBe('APROVADO');
    }
  });

  test.beforeEach(async ({ page }) => {
    await limparSessao(page);
  });

  test('deve expor o catálogo público com busca, filtro por tag, limpeza de filtros e abertura do projeto (C1-C5)', async ({
    page,
  }) => {
    // -------------------------------------------------------------------------
    // PASSO 1 (C1): o card do projeto aprovado aparece em /catalogo-publico
    // -------------------------------------------------------------------------
    await page.goto('/catalogo-publico');

    const cardProjeto = page.locator('.grid-projetos a.card-link').filter({
      has: page.locator('h3.nome-projeto', { hasText: tituloProjetoCatalogo }),
    });
    await expect(cardProjeto).toBeVisible({ timeout: 10000 });
    await expect(cardProjeto.locator('h3.nome-projeto')).toHaveText(tituloProjetoCatalogo);
    await expect(cardProjeto.locator('span.tag-chip')).toHaveText(tagExclusiva);

    // -------------------------------------------------------------------------
    // PASSO 2 (C2): busca por parte do título mantém o card; texto sem
    // resultado mostra "Nenhum projeto encontrado"
    // -------------------------------------------------------------------------
    const campoBusca = page.locator(
      'input.input-busca[placeholder="Buscar por título, resumo, tecnologia ou palavra-chave..."]',
    );
    await expect(campoBusca).toBeVisible();

    const termoParcial = tituloProjetoCatalogo.split('-')[0].trim();
    await campoBusca.fill(termoParcial);
    await expect(cardProjeto).toBeVisible();

    await campoBusca.fill(`TermoInexistenteQueNaoExiste-${timestamp}`);
    await expect(page.locator('.estado-vazio h3.vazio-titulo')).toHaveText('Nenhum projeto encontrado');
    await expect(cardProjeto).toHaveCount(0);

    // -------------------------------------------------------------------------
    // PASSO 3 (C3): clicar na tag filtra a lista (selecionarTag)
    // -------------------------------------------------------------------------
    await campoBusca.fill('');
    await expect(cardProjeto).toBeVisible();

    const chipTag = page.locator('button.chip-filtro').filter({ hasText: tagExclusiva });
    await expect(chipTag).toBeVisible();
    await chipTag.click();
    await expect(chipTag).toHaveClass(/chip-ativo/);
    await expect(cardProjeto).toBeVisible();
    await expect(page.locator('.grid-projetos a.card-link')).toHaveCount(1);

    // -------------------------------------------------------------------------
    // PASSO 4 (C4): o botão "Limpar Filtros" devolve a lista completa
    // -------------------------------------------------------------------------
    const botaoLimpar = page.locator('button.btn-limpar-todos');
    await expect(botaoLimpar).toBeVisible();
    await expect(botaoLimpar).toContainText('Limpar Filtros');
    await botaoLimpar.click();

    await expect(chipTag).not.toHaveClass(/chip-ativo/);
    await expect(cardProjeto).toBeVisible();
    await expect(page.locator('.grid-projetos a.card-link').first()).toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 5 (C5): clicar no card abre /ver-projeto/<id> com o título do projeto
    // -------------------------------------------------------------------------
    await cardProjeto.click();
    await expect(page).toHaveURL(new RegExp(`/ver-projeto/${'.+'}`));
    await expect(page.locator('h1.titulo')).toHaveText(tituloProjetoCatalogo);
  });

  test('deve listar as Fatecs com busca, filtro por região e detalhe da instituição com cursos e projetos (C6-C10)', async ({
    page,
    request,
  }) => {
    // Dados reais do catálogo (mesma API que alimenta /instituicoes) para que os
    // textos esperados venham do banco, não de suposição.
    const baseUrlApi = await resolverBaseUrlApi(request);
    const resInstituicoes = await request.get(`${baseUrlApi}/instituicoes`);
    expect(resInstituicoes.ok()).toBeTruthy();
    const instituicoes = (await resInstituicoes.json()) as {
      id: string;
      codigoUnidade: string;
      nome: string;
      cidade?: string;
      regiaoAdministrativa?: string;
    }[];

    const fatec = instituicoes.find((i) => i.nome === nomeUnidadeAlvo);
    expect(fatec).toBeTruthy();
    const nomeFatec = fatec!.nome;
    const regiaoFatec = fatec!.regiaoAdministrativa!;

    // -------------------------------------------------------------------------
    // PASSO 1 (C6): /instituicoes exibe cards de Fatec
    // -------------------------------------------------------------------------
    await page.goto('/instituicoes');

    await expect(page.locator('h1')).toHaveText('Faculdades de Tecnologia de São Paulo');
    await expect(page.locator('a[href="/instituicoes/' + fatec!.id + '"]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Ver perfil e cursos').first()).toBeVisible();

    // -------------------------------------------------------------------------
    // PASSO 2 (C7): busca por parte do nome filtra; texto inexistente mostra
    // "Nenhuma Fatec encontrada"
    // -------------------------------------------------------------------------
    const campoPesquisa = page.locator(
      'input[placeholder="Pesquisar por nome da unidade, cidade ou código..."]',
    );
    await expect(campoPesquisa).toBeVisible();

    const parteNome = nomeFatec.replace('Fatec ', '').split(' ')[0];
    await campoPesquisa.fill(parteNome);
    await expect(page.locator('a[href="/instituicoes/' + fatec!.id + '"]')).toBeVisible();

    await campoPesquisa.fill(`CidadeInexistenteNoCatalogo-${timestamp}`);
    await expect(page.locator('h3:has-text("Nenhuma Fatec encontrada")')).toBeVisible();
    await expect(page.locator('a[href^="/instituicoes/"]')).toHaveCount(0);

    // -------------------------------------------------------------------------
    // PASSO 3 (C8): o select "Filtrar por região administrativa" muda a lista
    // -------------------------------------------------------------------------
    await campoPesquisa.fill('');

    const selectRegiao = page.locator('select[aria-label="Filtrar por região administrativa"]');
    await expect(selectRegiao).toBeVisible();

    await selectRegiao.selectOption({ label: regiaoFatec });
    await expect(page.locator('a[href="/instituicoes/' + fatec!.id + '"]')).toBeVisible();

    // Uma região com outra composição de unidades deve mudar a lista exibida
    const outraRegiao = instituicoes.find((i) => i.regiaoAdministrativa && i.regiaoAdministrativa !== regiaoFatec)!
      .regiaoAdministrativa!;
    await selectRegiao.selectOption({ label: outraRegiao });
    await expect(page.locator('a[href="/instituicoes/' + fatec!.id + '"]')).toHaveCount(0);

    await selectRegiao.selectOption({ label: 'Todas as regiões' });

    // -------------------------------------------------------------------------
    // PASSO 4 (C9): o perfil da instituição traz "Cursos Oferecidos"
    // -------------------------------------------------------------------------
    await page.goto(`/instituicoes/${fatec!.id}`);
    await expect(page.locator('h1')).toHaveText(nomeFatec);
    await expect(page.locator('section h2:has-text("Cursos Oferecidos")')).toBeVisible();

    // O curso alvo é um dos que já têm projeto aprovado nesta unidade; o outro
    // projeto criado no beforeAll fica em um curso diferente, de modo que o
    // filtro realmente reduz a lista.
    const resPerfil = await request.get(`${baseUrlApi}/instituicoes/${fatec!.id}/perfil`);
    const perfil = await resPerfil.json();
    const cursosComProjeto = new Set(
      (perfil.projetos as { cursoId?: string }[]).map((p) => p.cursoId).filter(Boolean),
    );
    const cursoAlvo = (perfil.cursos as { id: string; nome: string }[]).find((c) =>
      cursosComProjeto.has(c.id),
    );
    expect(cursoAlvo).toBeTruthy();

    const totalInicial = (perfil.projetos as unknown[]).length;
    const totalFiltrado = (perfil.projetos as { cursoId?: string }[]).filter(
      (p) => p.cursoId === cursoAlvo?.id,
    ).length;
    expect(totalFiltrado).toBeGreaterThan(0);
    expect(totalFiltrado).toBeLessThan(totalInicial);

    // -------------------------------------------------------------------------
    // PASSO 5 (C10): clicar no curso filtra "Projetos Publicados"; clicar de
    // novo na mesma opção limpa o filtro
    // -------------------------------------------------------------------------
    await expect(page.locator('section h2:has-text("Projetos Publicados")')).toBeVisible();
    await expect(page.locator('app-card-projeto')).toHaveCount(totalInicial);

    const botaoCurso = page.locator('button.rounded-full').filter({ hasText: cursoAlvo!.nome });
    await expect(botaoCurso).toBeVisible();
    await botaoCurso.click();

    await expect(page.locator('app-card-projeto')).toHaveCount(totalFiltrado);
    await expect(page.locator('section h2:has-text("Projetos Publicados")')).toBeVisible();

    // Clicar na mesma opção volta para "Todos os cursos" e restaura a lista
    const botaoTodosCursos = page.locator('button.rounded-full').filter({
      hasText: `Todos os cursos (${totalInicial})`,
    });
    await expect(botaoTodosCursos).toBeVisible();
    await botaoTodosCursos.click();

    await expect(page.locator('app-card-projeto')).toHaveCount(totalInicial);
  });
});
