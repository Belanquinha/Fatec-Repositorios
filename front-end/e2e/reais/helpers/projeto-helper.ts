import { APIRequestContext, expect } from '@playwright/test';
import { resolverBaseUrlApi } from './auth-helper';

export interface ProjetoCriado {
  id: string;
  titulo: string;
  instituicaoId: string;
  cursoId: string;
  emailProfessorResponsavel: string;
}

/** Ajustes opcionais da submissão; tudo que fica ausente usa o padrão do helper. */
export interface OpcoesProjetoPendente {
  descricaoCurta?: string;
  palavrasChave?: string[];
  anoPublicado?: number;
  /** Se ausente, usa a unidade pedida por `nomeUnidade` ou a primeira do catálogo. */
  instituicaoId?: string;
  /** Se ausente, usa o primeiro curso oferecido pela unidade. */
  cursoId?: string;
  /** Nome exato da unidade no catálogo, quando se quer mirar uma Fatec específica. */
  nomeUnidade?: string;
}

/**
 * Cria e submete um projeto acadêmico real via API do Spring Boot.
 * O projeto é criado em nome do aluno e nasce no estado AGUARDANDO_APROVACAO,
 * pronto para avaliação pelo professor indicado.
 *
 * O quarto parâmetro aceita o título (string) ou um objeto de opções com
 * `titulo` junto dos ajustes acima.
 */
export async function criarProjetoPendenteReal(
  request: APIRequestContext,
  tokenAluno: string,
  emailProfessor: string,
  tituloOuOpcoes?: string | ({ titulo?: string } & OpcoesProjetoPendente),
): Promise<ProjetoCriado> {
  const baseUrl = await resolverBaseUrlApi(request);

  const opcoes: { titulo?: string } & OpcoesProjetoPendente =
    typeof tituloOuOpcoes === 'string' || tituloOuOpcoes === undefined
      ? { titulo: tituloOuOpcoes }
      : tituloOuOpcoes;

  // 1. Obtém as instituições cadastradas
  const resInstituicoes = await request.get(`${baseUrl}/instituicoes`);
  expect(resInstituicoes.ok()).toBeTruthy();
  const instituicoes = (await resInstituicoes.json()) as { id: string; nome: string }[];
  expect(instituicoes.length).toBeGreaterThan(0);

  const instituicaoId =
    opcoes.instituicaoId ??
    (opcoes.nomeUnidade ? instituicoes.find((i) => i.nome === opcoes.nomeUnidade)?.id : undefined) ??
    instituicoes[0].id;

  // 2. Obtém os cursos oferecidos pela instituição
  const resCursos = await request.get(`${baseUrl}/instituicoes/${instituicaoId}/cursos`);
  expect(resCursos.ok()).toBeTruthy();
  const cursos = (await resCursos.json()) as { id: string; nome: string }[];
  expect(cursos.length).toBeGreaterThan(0);
  const cursoId = opcoes.cursoId ?? cursos[0].id;

  const nomeProjeto = opcoes.titulo ?? `Projeto de Teste Automatizado - ${Date.now()}`;

  // 3. Submete o projeto acadêmico
  const resProjeto = await request.post(`${baseUrl}/projetos`, {
    headers: {
      Authorization: `Bearer ${tokenAluno}`,
      'Content-Type': 'application/json',
    },
    data: {
      titulo: nomeProjeto,
      descricaoCurta:
        opcoes.descricaoCurta ??
        'Projeto acadêmico submetido para validação do orientador nos testes automatizados.',
      instituicaoId,
      cursoId,
      emailProfessorResponsavel: emailProfessor,
      integrantes: [
        { nome: 'Mariana Silva Aluna', linkedin: 'https://linkedin.com/in/mariana-aluna' },
      ],
      palavrasChave: opcoes.palavrasChave ?? ['Playwright', 'E2E', 'Avaliação'],
      anoPublicado: opcoes.anoPublicado ?? new Date().getFullYear(),
      conteudoEditorJs: JSON.stringify({
        time: Date.now(),
        blocks: [
          {
            type: 'header',
            data: { text: 'Detalhamento do Projeto para Avaliação', level: 2 },
          },
          {
            type: 'paragraph',
            data: { text: 'Este projeto foi submetido via integração para teste do fluxo de avaliação do orientador.' },
          },
        ],
        version: '2.31.7',
      }),
    },
  });

  expect(resProjeto.ok()).toBeTruthy();
  const dados = await resProjeto.json();

  return {
    id: dados.id,
    titulo: dados.titulo,
    instituicaoId,
    cursoId,
    emailProfessorResponsavel: emailProfessor,
  };
}
