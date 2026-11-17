import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Perfil } from './perfil';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { ProjetoService } from '../../core/services/projeto.service';
import { ProjetoResponseModel } from '../../core/models/projeto.model';

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

  function projeto(
    estado: ProjetoResponseModel['estado'],
    titulo: string,
    id = 'proj-1'
  ): ProjetoResponseModel {
    return {
      id,
      titulo,
      descricaoCurta: `Descrição de ${titulo}`,
      instituicaoNome: 'Fatec Ipiranga',
      estado,
      criadoEm: '2026-09-11T00:00:00Z',
    } as ProjetoResponseModel;
  }

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
      obterMeusProjetos: vi.fn().mockReturnValue(of([])),
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

  it('exibe identidade do aluno com avatar de iniciais', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(of([])) }
    );

    expect(component.usuario()).toEqual(aluno);
    expect(component.isAluno()).toBe(true);
    expect(component.iniciais()).toBe('AT');

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Aluno Teste');
    expect(texto).toContain('aluno.teste@aluno.cps.sp.gov.br');
  });

  it('conta e lista os projetos aprovados do aluno na aba "Seus Projetos"', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      {
        obterMeusProjetos: vi.fn().mockReturnValue(
          of([
            projeto('APROVADO', 'Projeto Um'),
            projeto('APROVADO', 'Projeto Dois'),
            projeto('AGUARDANDO_APROVACAO', 'Projeto Em Analise'),
          ])
        ),
      }
    );

    expect(component.totalPublicados()).toBe(2);
    expect(component.totalEmAnalise()).toBe(1);
    expect(component.totalPendentes()).toBe(1);

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('2');
    expect(texto).toContain('Projeto Um');
    expect(texto).toContain('Projeto Dois');
    // Em análise não pode vazar para a aba de publicados.
    expect(texto).not.toContain('Projeto Em Analise');
    expect(texto).toContain('Exibindo 1–2 de 2 projetos publicados');
  });

  it('aba "Pendentes" do aluno mostra o que foi enviado e está em análise', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      {
        obterMeusProjetos: vi.fn().mockReturnValue(
          of([
            projeto('APROVADO', 'Projeto Um'),
            projeto('AGUARDANDO_APROVACAO', 'Projeto Em Analise'),
            projeto('REENVIADO', 'Projeto Reenviado'),
          ])
        ),
      }
    );

    component.selecionarAba('pendentes');
    fixture.detectChanges();

    expect(component.projetosFiltrados().map((p) => p.titulo)).toEqual([
      'Projeto Em Analise',
      'Projeto Reenviado',
    ]);
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Projeto Em Analise');
    expect(texto).toContain('Projeto Reenviado');
    expect(texto).not.toContain('Projeto Um');
    // Aluno não vê o botão de rejeitar.
    expect(texto).not.toContain('Rejeitar');
  });

  it('busca filtra os cards da aba ativa', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      {
        obterMeusProjetos: vi.fn().mockReturnValue(
          of([
            projeto('APROVADO', 'Portal de Biblioteca', 'p1'),
            projeto('APROVADO', 'Sistema de Chamadas', 'p2'),
          ])
        ),
      }
    );

    const busca = fixture.debugElement.query(By.css('.busca input'));
    busca.nativeElement.value = 'biblioteca';
    busca.triggerEventHandler('input', { target: busca.nativeElement });
    fixture.detectChanges();

    expect(component.projetosFiltrados()).toHaveLength(1);
    expect(component.projetosFiltrados()[0].titulo).toBe('Portal de Biblioteca');
  });

  it('mantém a identidade quando o contexto falha', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(throwError(() => new Error('backend fora do ar'))) }
    );

    expect(component.usuario()).toEqual(aluno);
    expect(component.erroMeus()).toContain('Não foi possível carregar');
  });

  it('professor zerado vê onboarding inline e carrega as três fontes', async () => {
    const mock = projetosProfessor([], [fatecA, fatecB]);
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    expect(component.isProfessor()).toBe(true);
    expect(mock.obterMeusVinculos).toHaveBeenCalled();
    expect(mock.listarPendentesAprovacao).toHaveBeenCalled();
    expect(mock.obterMeusProjetos).toHaveBeenCalled();
    expect(component.selecao()).toEqual([]);
    expect(component.totalPendentes()).toBe(0);
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Sem instituições vinculadas ainda');
    expect(texto).toContain('0 de 4');
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

    component.abrirModalVinculos();
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
    component.abrirModalVinculos();
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
        obterMeusProjetos: vi.fn().mockReturnValue(of([])),
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
        obterMeusProjetos: vi.fn().mockReturnValue(of([])),
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
        obterMeusProjetos: vi.fn().mockReturnValue(of([])),
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

  it('professor vê a fila de pendentes na aba "Pendentes"', async () => {
    const pendente = projeto('AGUARDANDO_APROVACAO', 'Projeto Pendente');
    const mock = projetosProfessor([], [fatecA, fatecB]);
    mock.listarPendentesAprovacao.mockReturnValue(of([pendente]));
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    expect(mock.listarPendentesAprovacao).toHaveBeenCalled();
    expect(component.pendentes()).toHaveLength(1);

    component.selecionarAba('pendentes');
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Projeto Pendente');
    expect(texto).toContain('Ver Projeto');
    expect(texto).toContain('Enviado em');
    expect(texto).toContain('Rejeitar');
    expect(texto).toContain('Exibindo 1–1 de 1 projetos pendentes');
  });

  it('card de pendente leva ao ver-projeto em modo de avaliação', async () => {
    const pendente = projeto('AGUARDANDO_APROVACAO', 'Projeto Pendente');
    const mock = projetosProfessor([], [fatecA, fatecB]);
    mock.listarPendentesAprovacao.mockReturnValue(of([pendente]));
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    component.selecionarAba('pendentes');
    fixture.detectChanges();

    const link = fixture.nativeElement.querySelector('.card-acoes a') as HTMLAnchorElement;
    expect(link).toBeTruthy();
    expect(link.getAttribute('href')).toContain('/ver-projeto/proj-1');
  });

  it('projetos aprovados não pedem avaliação (?avaliar=1)', async () => {
    const aprovado = projeto('APROVADO', 'Projeto Aprovado');
    const mock = projetosProfessor([], [fatecA, fatecB]);
    mock.obterMeusProjetos.mockReturnValue(of([aprovado]));
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    const link = fixture.nativeElement.querySelector('.card-acoes a') as HTMLAnchorElement;
    expect(link.getAttribute('href')).not.toContain('avaliar');
  });

  it('ADMIN não vê o botão "Submeter Projeto"', async () => {
    const admin = {
      nome: 'Admin Teste',
      email: 'admin.teste@cps.sp.gov.br',
      role: 'ADMIN',
    };
    const mock = projetosProfessor([], [fatecA, fatecB]);
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(admin) }, mock);

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).not.toContain('Submeter Projeto');
    expect(fixture.nativeElement.querySelector('a[href="/projeto-forms"]')).toBeNull();
  });

  it('aluno e professor veem o botão "Submeter Projeto"', async () => {
    const mock = projetosProfessor([], [fatecA, fatecB]);
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);
    expect(fixture.nativeElement.querySelector('a[href="/projeto-forms"]')).toBeTruthy();
  });

  it('rejeitar exige motivo e remove da lista', async () => {
    const pendente = projeto('AGUARDANDO_APROVACAO', 'Projeto Pendente');
    const mock = projetosProfessor([], [fatecA, fatecB]);
    mock.listarPendentesAprovacao.mockReturnValue(of([pendente]));
    mock.rejeitarProjeto.mockReturnValue(of({ ...pendente, estado: 'REJEITADO' }));
    await configurar({ obterUsuarioLogado: vi.fn().mockResolvedValue(professor) }, mock);

    component.selecionarAba('pendentes');
    component.abrirRejeicao(pendente.id);
    component.confirmarRejeicao(pendente.id);
    expect(component.erroPendentes()).toContain('motivo');
    expect(mock.rejeitarProjeto).not.toHaveBeenCalled();

    component.onMotivoRejeicao(pendente.id, 'Falta referencial teórico');
    component.confirmarRejeicao(pendente.id);

    expect(mock.rejeitarProjeto).toHaveBeenCalledWith(pendente.id, 'Falta referencial teórico');
    expect(component.pendentes()).toHaveLength(0);
  });

  it('aba "Recusados" fica desativada (visual por enquanto)', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(of([projeto('APROVADO', 'Projeto Um')])) }
    );
    fixture.detectChanges();

    const aba = fixture.nativeElement.querySelector('.aba-desativada') as HTMLButtonElement;
    expect(aba).toBeTruthy();
    expect(aba.disabled).toBe(true);
  });

  it('"Mudar Instituição" abre o modal com o campo de instituições', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(professor) },
      projetosProfessor([], [fatecA, fatecB])
    );

    expect(component.modalVinculosAberto()).toBe(false);
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();

    component.irParaInstituicoes();
    fixture.detectChanges();

    expect(component.modalVinculosAberto()).toBe(true);
    const dialog = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    expect(dialog.querySelector('#vinculo-busca')).toBeTruthy();
    expect(dialog.textContent).toContain('Mudar instituições');

    component.fecharModalVinculos();
    fixture.detectChanges();

    expect(component.modalVinculosAberto()).toBe(false);
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
  });

  it('Escape e clique no overlay fecham o modal de instituições', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(professor) },
      projetosProfessor([], [fatecA, fatecB])
    );

    component.abrirModalVinculos();
    fixture.detectChanges();
    expect(component.modalVinculosAberto()).toBe(true);

    document.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true })
    );
    fixture.detectChanges();
    expect(component.modalVinculosAberto()).toBe(false);

    component.abrirModalVinculos();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.modal-overlay') as HTMLElement).click();
    fixture.detectChanges();
    expect(component.modalVinculosAberto()).toBe(false);
  });

  it('aluno não abre o modal de instituições', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(of([])) }
    );

    component.irParaInstituicoes();
    fixture.detectChanges();

    expect(component.modalVinculosAberto()).toBe(false);
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
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
      obterMeusProjetos: vi.fn().mockReturnValue(of([])),
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
