import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Perfil } from './perfil';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { ProjetoService } from '../../core/services/projeto.service';

describe('Perfil', () => {
  let component: Perfil;
  let fixture: ComponentFixture<Perfil>;

  const aluno = {
    nome: 'Aluno Teste',
    email: 'aluno.teste@aluno.cps.sp.gov.br',
    role: 'ALUNO',
  };

  const professor = {
    nome: 'Professor Teste',
    email: 'professor.teste@cps.sp.gov.br',
    role: 'PROFESSOR',
  };

  const fatecA = { id: 'a', codigoUnidade: '291', nome: 'Fatec Ipiranga', cidade: 'São Paulo', ativo: true };
  const fatecB = { id: 'b', codigoUnidade: '296', nome: 'Fatec São Paulo', cidade: 'São Paulo', ativo: true };
  const fatecC = { id: 'c', codigoUnidade: '251', nome: 'Fatec Mauá', cidade: 'Mauá', ativo: true };
  const fatecD = { id: 'd', codigoUnidade: '269', nome: 'Fatec Santos', cidade: 'Santos', ativo: true };
  const fatecE = { id: 'e', codigoUnidade: '290', nome: 'Fatec Barueri', cidade: 'Barueri', ativo: true };

  async function configurar(authMock: unknown, projetosMock: unknown) {
    await TestBed.configureTestingModule({
      imports: [Perfil],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authMock },
        { provide: ProjetoService, useValue: projetosMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Perfil);
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function projetosProfessor(vinculos: unknown[] = [], catalogo: unknown[] = [fatecA, fatecB]) {
    return {
      obterMeusProjetos: vi.fn(),
      listarInstituicoes: vi.fn().mockReturnValue(of(catalogo)),
      obterMeusVinculos: vi.fn().mockReturnValue(of(vinculos)),
      salvarMeusVinculos: vi.fn().mockReturnValue(of(vinculos)),
      listarPendentesAprovacao: vi.fn().mockReturnValue(of([])),
      aprovarProjeto: vi.fn().mockReturnValue(of({})),
      rejeitarProjeto: vi.fn().mockReturnValue(of({})),
    };
  }

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('exibe identidade e badge de papel do aluno', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(of([])) }
    );

    expect(component.usuario()).toEqual(aluno);
    expect(component.rotuloPapel()).toBe('Aluno');
    expect(component.isAluno()).toBe(true);

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Aluno Teste');
    expect(texto).toContain('aluno.teste@aluno.cps.sp.gov.br');
  });

  it('mostra o contador de projetos do aluno', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(of([{ id: '1' }, { id: '2' }])) }
    );

    expect(component.totalProjetos()).toBe(2);
    expect(fixture.nativeElement.textContent as string).toContain('2');
  });

  it('mantém a identidade quando o contexto falha (aviso laranja)', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(throwError(() => new Error('backend fora do ar'))) }
    );

    expect(component.usuario()).toEqual(aluno);
    expect(component.erroContexto()).toContain('Não foi possível carregar');
  });

  it('professor zerado vê onboarding inline sem chamar /projetos/meus', async () => {
    const mock = projetosProfessor([], [fatecA, fatecB]);
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    expect(component.isProfessor()).toBe(true);
    expect(component.rotuloPapel()).toBe('Professor');
    expect(mock.obterMeusProjetos).not.toHaveBeenCalled();
    expect(mock.obterMeusVinculos).toHaveBeenCalled();
    expect(component.selecao()).toEqual([]);
    expect(fixture.nativeElement.textContent as string).toContain(
      'Sem instituições vinculadas ainda'
    );
    expect(fixture.nativeElement.textContent as string).toContain('0 de 4');
  });

  it('tipo-para-revelar: sem consulta não sugere nada', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(professor) },
      projetosProfessor([], [fatecA, fatecB])
    );

    component.onBuscaVinculo('');
    expect(component.sugestoesVinculo).toEqual([]);
    expect(component.mostrarDropdownVinculo()).toBe(false);
  });

  it('adiciona chip por clique e marca alterações não salvas', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(professor) },
      projetosProfessor([], [fatecA, fatecB])
    );

    component.adicionarVinculo(fatecA as never);
    fixture.detectChanges();

    expect(component.selecao().map((s) => s.id)).toEqual(['a']);
    expect(component.temAlteracoes()).toBe(true);
    expect(component.contadorVinculos()).toBe('1 de 4 unidades');
    expect(fixture.nativeElement.textContent as string).toContain('alterações não salvas');
  });

  it('recusa a 5ª instituição inline (teto 4, sem chamar o servidor)', async () => {
    const mock = projetosProfessor([fatecA, fatecB, fatecC, fatecD] as never[], [
      fatecA, fatecB, fatecC, fatecD, fatecE,
    ]);
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);
    fixture.detectChanges();

    expect(component.selecao()).toHaveLength(4);
    component.adicionarVinculo(fatecE as never);
    fixture.detectChanges();

    expect(component.selecao()).toHaveLength(4);
    expect(component.tetoRecusado()).toBe(true);
    expect(mock.salvarMeusVinculos).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent as string).toContain(
      'Máximo de 4 instituições por professor.'
    );
  });

  it('salvar persiste via PUT e mostra feedback', async () => {
    const mock = projetosProfessor([], [fatecA, fatecB]);
    mock.salvarMeusVinculos.mockReturnValue(of([fatecA]));
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    component.adicionarVinculo(fatecA as never);
    component.salvarVinculos();
    fixture.detectChanges();

    expect(mock.salvarMeusVinculos).toHaveBeenCalledWith(['a']);
    expect(component.vinculos().map((v) => v.id)).toEqual(['a']);
    expect(component.temAlteracoes()).toBe(false);
    expect(component.sucessoVinculos()).toContain('1 de 4 unidades');
  });

  it('falha no catálogo mostra painel neutro com retry', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(professor) },
      {
        obterMeusProjetos: vi.fn(),
        listarInstituicoes: vi.fn().mockReturnValue(throwError(() => new Error('fora do ar'))),
        obterMeusVinculos: vi.fn().mockReturnValue(of([])),
        salvarMeusVinculos: vi.fn(),
        listarPendentesAprovacao: vi.fn().mockReturnValue(of([])),
        aprovarProjeto: vi.fn(),
        rejeitarProjeto: vi.fn(),
      }
    );
    fixture.detectChanges();

    expect(component.erroVinculos()).toContain('Não foi possível carregar');
    expect(fixture.nativeElement.textContent as string).toContain('Tentar novamente');
  });

  it('falha do catálogo preserva vínculos salvos (sem PUT [] acidental)', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(professor) },
      {
        obterMeusProjetos: vi.fn(),
        listarInstituicoes: vi.fn().mockReturnValue(throwError(() => new Error('fora do ar'))),
        obterMeusVinculos: vi.fn().mockReturnValue(of([fatecA])),
        salvarMeusVinculos: vi.fn(),
        listarPendentesAprovacao: vi.fn().mockReturnValue(of([])),
        aprovarProjeto: vi.fn(),
        rejeitarProjeto: vi.fn(),
      }
    );
    fixture.detectChanges();

    expect(component.erroVinculos()).toContain('Não foi possível carregar');
    expect(component.vinculos().map((v) => v.id)).toEqual(['a']);
    expect(component.selecao().map((v) => v.id)).toEqual(['a']);
  });

  it('falha dos vínculos preserva o catálogo carregado', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(professor) },
      {
        obterMeusProjetos: vi.fn(),
        listarInstituicoes: vi.fn().mockReturnValue(of([fatecA, fatecB])),
        obterMeusVinculos: vi.fn().mockReturnValue(throwError(() => new Error('fora do ar'))),
        salvarMeusVinculos: vi.fn(),
        listarPendentesAprovacao: vi.fn().mockReturnValue(of([])),
        aprovarProjeto: vi.fn(),
        rejeitarProjeto: vi.fn(),
      }
    );
    fixture.detectChanges();

    expect(component.erroVinculos()).toContain('Não foi possível carregar');
    expect(component.catalogo().map((v) => v.id)).toEqual(['a', 'b']);
  });

  it('professor carrega pendentes para aprovação ao abrir o perfil', async () => {
    const pendente = {
      id: 'proj-1',
      titulo: 'Projeto Pendente',
      descricaoCurta: 'Descrição',
      instituicaoNome: 'Fatec Ipiranga',
      autorNome: 'Aluno',
      estado: 'AGUARDANDO_APROVACAO',
      criadoEm: '2026-09-11T00:00:00Z',
    };
    const mock = projetosProfessor([], [fatecA, fatecB]);
    mock.listarPendentesAprovacao.mockReturnValue(of([pendente]));
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    expect(mock.listarPendentesAprovacao).toHaveBeenCalled();
    expect(component.pendentes()).toHaveLength(1);
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Projeto Pendente');
    expect(texto).toContain('Ver projeto');
    expect(texto).toContain('Enviado em');
  });

  it('card de pendente leva ao ver-projeto em modo de avaliação', async () => {
    const pendente = {
      id: 'proj-1',
      titulo: 'Projeto Pendente',
      descricaoCurta: 'Descrição',
      instituicaoNome: 'Fatec Ipiranga',
      autorNome: 'Aluno',
      estado: 'AGUARDANDO_APROVACAO',
    };
    const mock = projetosProfessor([], [fatecA, fatecB]);
    mock.listarPendentesAprovacao.mockReturnValue(of([pendente]));
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);
    fixture.detectChanges();

    const link = fixture.nativeElement.querySelector('.pendente-acoes a') as HTMLAnchorElement;
    expect(link).toBeTruthy();
    expect(link.getAttribute('href')).toContain('/ver-projeto/proj-1');
    expect(link.getAttribute('href')).toContain('avaliar=1');
  });

  it('rejeitar exige motivo e remove da lista', async () => {
    const pendente = {
      id: 'proj-1',
      titulo: 'Projeto Pendente',
      descricaoCurta: 'Descrição',
      instituicaoNome: 'Fatec Ipiranga',
      autorNome: 'Aluno',
      estado: 'AGUARDANDO_APROVACAO',
    };
    const mock = projetosProfessor([], [fatecA, fatecB]);
    mock.listarPendentesAprovacao.mockReturnValue(of([pendente]));
    mock.rejeitarProjeto.mockReturnValue(of({ ...pendente, estado: 'REJEITADO' }));
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    component.abrirRejeicao('proj-1');
    component.confirmarRejeicao('proj-1');
    expect(component.erroPendentes()).toContain('motivo');
    expect(mock.rejeitarProjeto).not.toHaveBeenCalled();

    component.onMotivoRejeicao('proj-1', 'Falta referencial teórico');
    component.confirmarRejeicao('proj-1');

    expect(mock.rejeitarProjeto).toHaveBeenCalledWith('proj-1', 'Falta referencial teórico');
    expect(component.pendentes()).toHaveLength(0);
  });
});

/**
 * Item 30: corpo HTML cru (ex.: 413 do nginx) nunca vai para a tela —
 * `err.error` string começando com `<` ou envelope com mensagem `<...`
 * cai na mensagem amigável.
 */
describe('Perfil (erro HTML normalizado, item 30)', () => {
  const professor = {
    nome: 'Professor Teste',
    email: 'professor.teste@cps.sp.gov.br',
    role: 'PROFESSOR',
  };
  const html413 = '<html><body><h1>413 Request Entity Too Large</h1></body></html>';

  async function configurarProfessor(mock: unknown) {
    await TestBed.configureTestingModule({
      imports: [Perfil],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { obterUsuarioLogado: vi.fn().mockResolvedValue(professor) } },
        { provide: ProjetoService, useValue: mock },
      ],
    }).compileComponents();
    const fx = TestBed.createComponent(Perfil);
    const comp = fx.componentInstance;
    await fx.whenStable();
    fx.detectChanges();
    return { fx, comp: comp as Perfil };
  }

  function baseMock() {
    return {
      obterMeusProjetos: vi.fn(),
      listarInstituicoes: vi.fn().mockReturnValue(of([])),
      obterMeusVinculos: vi.fn().mockReturnValue(of([])),
      salvarMeusVinculos: vi.fn().mockReturnValue(of([])),
      listarPendentesAprovacao: vi.fn().mockReturnValue(of([])),
      aprovarProjeto: vi.fn().mockReturnValue(of({})),
      rejeitarProjeto: vi.fn().mockReturnValue(of({})),
    };
  }

  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('carregarPendentes troca corpo HTML por mensagem amigável', async () => {
    const mock = baseMock();
    mock.listarPendentesAprovacao.mockReturnValue(
      throwError(() => ({ status: 413, error: html413 }))
    );
    const { comp } = await configurarProfessor(mock);

    comp.carregarPendentes();
    expect(comp.erroPendentes()).toBe(
      'Não foi possível carregar os projetos pendentes agora. Tente novamente.'
    );
    expect(comp.erroPendentes()).not.toContain('<html>');
  });

  it('confirmarRejeicao troca envelope com mensagem HTML por amigável', async () => {
    const mock = baseMock();
    mock.rejeitarProjeto.mockReturnValue(
      throwError(() => ({ status: 413, error: { mensagem: html413 } }))
    );
    const { comp } = await configurarProfessor(mock);

    comp.onMotivoRejeicao('proj-1', 'Falta referencial');
    comp.confirmarRejeicao('proj-1');
    expect(comp.erroPendentes()).toBe(
      'Não foi possível rejeitar o projeto agora. Tente novamente.'
    );
    expect(comp.erroPendentes()).not.toContain('<html>');
  });

  it('confirmarRejeicao troca corpo HTML por amigável', async () => {
    const mock = baseMock();
    mock.rejeitarProjeto.mockReturnValue(
      throwError(() => ({ status: 413, error: html413 }))
    );
    const { comp } = await configurarProfessor(mock);

    comp.onMotivoRejeicao('proj-1', 'Falta referencial');
    comp.confirmarRejeicao('proj-1');
    expect(comp.erroPendentes()).toBe(
      'Não foi possível rejeitar o projeto agora. Tente novamente.'
    );
    expect(comp.erroPendentes()).not.toContain('<html>');
  });

  it('salvarVinculos troca corpo HTML por amigável e mantém a seleção', async () => {
    const fatec = { id: 'a', codigoUnidade: '291', nome: 'Fatec Ipiranga', cidade: 'São Paulo', ativo: true };
    const mock = baseMock();
    mock.obterMeusVinculos.mockReturnValue(of([]));
    mock.salvarMeusVinculos.mockReturnValue(
      throwError(() => ({ status: 413, error: html413 }))
    );
    const { comp } = await configurarProfessor(mock);

    comp.adicionarVinculo(fatec as never);
    comp.salvarVinculos();
    expect(comp.erroSalvarVinculos()).toBe(
      'Não foi possível salvar os vínculos agora. Tente novamente.'
    );
    expect(comp.erroSalvarVinculos()).not.toContain('<html>');
    expect(comp.selecao().map((s) => s.id)).toEqual(['a']);
  });

  it('mantém mensagem JSON normal do servidor quando não é HTML', async () => {
    const mock = baseMock();
    mock.rejeitarProjeto.mockReturnValue(
      throwError(() => ({ status: 409, error: { mensagem: 'Projeto já avaliado' } }))
    );
    const { comp } = await configurarProfessor(mock);

    comp.onMotivoRejeicao('proj-1', 'Falta referencial');
    comp.confirmarRejeicao('proj-1');
    expect(comp.erroPendentes()).toBe('Projeto já avaliado');
  });
});
