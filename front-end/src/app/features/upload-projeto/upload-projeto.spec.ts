import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadProjeto } from './upload-projeto';

import { provideHttpClient } from '@angular/common/http';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { ProjetoService } from '../../core/services/projeto.service';

import { provideRouter } from '@angular/router';

describe('UploadProjeto', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    buscarProfessores: () => of([]),
    obterProfessorPorId: () => of({ id: 'p1', nome: 'Maria', email: 'maria@cps.sp.gov.br' }),
    uploadImagem: () => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    criarProjeto: () => of({ id: '123', titulo: 'Teste' }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

/**
 * Story 3.2 (LGPD): o autocomplete busca por nome no servidor (só id+nome),
 * o e-mail sai só no detalhe do selecionado e a digitação livre de e-mail
 * segue como "Professor Convidado" sem chamar o backend.
 */
describe('UploadProjeto (autocomplete LGPD, Story 3.2)', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  /** Margem acima do debounce de 250ms do componente (timers reais, zoneless). */
  const aguardarDebounce = () => new Promise((r) => setTimeout(r, 350));

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    buscarProfessores: vi.fn(() => of([{ id: 'p1', nome: 'Maria Silva' }])),
    obterProfessorPorId: vi.fn(() =>
      of({ id: 'p1', nome: 'Maria Silva', email: 'maria@cps.sp.gov.br' })
    ),
    uploadImagem: () => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    criarProjeto: () => of({ id: '123', titulo: 'Teste' }),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('não deve buscar no backend com termo curto (<2 chars)', async () => {
    component.professorEmail = 'm';
    component.onProfessorEmailInput();
    await aguardarDebounce();

    expect(mockProjetoService.buscarProfessores).not.toHaveBeenCalled();
    expect(component.mostrarDropdownProfessor()).toBe(false);
    expect(component.professoresFiltrados).toEqual([]);
  });

  it('deve tratar e-mail digitado como Professor Convidado sem buscar (Story 3.2 AC)', async () => {
    component.professorEmail = 'convidado@externo.com';
    component.onProfessorEmailInput();
    await aguardarDebounce();

    expect(mockProjetoService.buscarProfessores).not.toHaveBeenCalled();
    expect(component.professorStatus).toBe('novo');
    expect(component.mostrarDropdownProfessor()).toBe(false);
  });

  it('deve buscar por nome com debounce e abrir o dropdown só com id+nome', async () => {
    component.professorEmail = 'mar';
    component.onProfessorEmailInput();

    // Debounce de 250ms: nada dispara de imediato (ambiente zoneless,
    // sem fakeAsync — espera real um pouco acima do debounce).
    expect(mockProjetoService.buscarProfessores).not.toHaveBeenCalled();
    await aguardarDebounce();

    expect(mockProjetoService.buscarProfessores).toHaveBeenCalledWith('mar');
    expect(component.professoresFiltrados).toEqual([{ id: 'p1', nome: 'Maria Silva' }]);
    expect(component.mostrarDropdownProfessor()).toBe(true);
  });

  it('deve obter o e-mail só no detalhe ao selecionar (nunca em massa)', () => {
    component.selecionarProfessor({ id: 'p1', nome: 'Maria Silva' });

    expect(mockProjetoService.obterProfessorPorId).toHaveBeenCalledWith('p1');
    expect(component.professorEmail).toBe('maria@cps.sp.gov.br');
    expect(component.professorStatus).toBe('cadastrado');
    expect(component.mostrarDropdownProfessor()).toBe(false);
  });

  it('deve voltar a vazio ao limpar o campo', () => {
    component.professorEmail = 'maria@cps.sp.gov.br';
    component.professorStatus = 'cadastrado';
    component.professorEmail = '';
    component.onProfessorEmailInput();

    expect(component.professorStatus).toBe('vazio');
    expect(component.professoresFiltrados).toEqual([]);
    expect(component.mostrarDropdownProfessor()).toBe(false);
  });

  it('bloqueia o envio enquanto o detalhe do professor está em voo (anti-race)', async () => {
    const detalhePendente = new Subject<{ id: string; nome: string; email: string }>();
    mockProjetoService.obterProfessorPorId.mockReturnValueOnce(detalhePendente.asObservable());
    const criarSpy = vi.fn(() => of({ id: '123', titulo: 'Teste' }));
    (mockProjetoService as { criarProjeto: unknown }).criarProjeto = criarSpy;

    component.selecionarProfessor({ id: 'p1', nome: 'Maria Silva' });
    expect(component.carregandoProfessor()).toBe(true);

    // Formulário válido para chegar até a guarda do professor em voo.
    component.titulo = 'Projeto Teste';
    component.descricaoCurta = 'Descrição curta válida';
    component.imagemCapaUrl = '/uploads/capa.png';
    component.instituicaoId = 'inst-1';
    component.professorEmail = 'antigo@cps.sp.gov.br';
    component.integrantes = [{ id: 1, nome: 'Ana', linkedin: '' }];

    await component.enviarParaAvaliacao();

    expect(criarSpy).not.toHaveBeenCalled();
    expect(component.erroMensagem()).toContain('Aguarde a confirmação do professor');

    detalhePendente.next({ id: 'p1', nome: 'Maria Silva', email: 'maria@cps.sp.gov.br' });
    expect(component.carregandoProfessor()).toBe(false);
    expect(component.professorEmail).toBe('maria@cps.sp.gov.br');
  });

  it('ignora resposta de seleção superada (duplo clique rápido)', () => {
    const primeira = new Subject<{ id: string; nome: string; email: string }>();
    mockProjetoService.obterProfessorPorId
      .mockReturnValueOnce(primeira.asObservable())
      .mockReturnValueOnce(of({ id: 'p2', nome: 'João Souza', email: 'joao@cps.sp.gov.br' }));

    component.selecionarProfessor({ id: 'p1', nome: 'Maria Silva' });
    component.selecionarProfessor({ id: 'p2', nome: 'João Souza' });

    // A primeira resposta chega atrasada: não pode sobrescrever a seleção atual.
    primeira.next({ id: 'p1', nome: 'Maria Silva', email: 'maria@cps.sp.gov.br' });
    expect(component.professorEmail).toBe('joao@cps.sp.gov.br');
    expect(component.carregandoProfessor()).toBe(false);
  });
});

/**
 * Limites de imagem (capa + Editor.js): o front recusa antes de enviar para
 * que um arquivo grande vire mensagem acionável, e não um 413 do nginx.
 */
describe('UploadProjeto (limites de imagem)', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    buscarProfessores: () => of([]),
    obterProfessorPorId: () => of({ id: 'p1', nome: 'Maria', email: 'maria@cps.sp.gov.br' }),
    uploadImagem: vi.fn(() => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' })),
    criarProjeto: vi.fn(() => of({ id: '123', titulo: 'Teste' })),
  };

  function eventoComArquivo(file: File) {
    return { target: { files: [file], value: 'capa.png' } } as unknown as Event;
  }

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('deve recusar capa com tipo não permitido (ex.: GIF)', () => {
    const gif = new File([new ArrayBuffer(1024)], 'capa.gif', { type: 'image/gif' });

    component.onCapaSelecionada(eventoComArquivo(gif));

    expect(component.capaArquivo).toBeNull();
    expect(component.erroMensagem()).toContain('Tipo de arquivo não permitido');
  });

  it('deve recusar capa acima de 10MB antes de enviar', () => {
    const grande = new File([new ArrayBuffer(11 * 1024 * 1024)], 'capa.png', {
      type: 'image/png',
    });

    component.onCapaSelecionada(eventoComArquivo(grande));

    expect(component.capaArquivo).toBeNull();
    expect(component.erroMensagem()).toContain('10MB');
  });

  it('deve aceitar capa válida e limpar o erro', () => {
    const ok = new File([new ArrayBuffer(1024)], 'capa.png', { type: 'image/png' });

    component.onCapaSelecionada(eventoComArquivo(ok));

    expect(component.capaArquivo).toBe(ok);
    expect(component.erroMensagem()).toBeNull();
  });

  it('deve barrar o envio quando a capa burlou a seleção e é grande demais', async () => {
    component.titulo = 'Projeto Teste';
    component.descricaoCurta = 'Descrição curta';
    component.instituicaoId = 'inst-1';
    component.professorEmail = 'prof@cps.sp.gov.br';
    component.integrantes = [{ id: 1, nome: 'Aluno', linkedin: '' }];
    component.capaArquivo = new File([new ArrayBuffer(11 * 1024 * 1024)], 'capa.png', {
      type: 'image/png',
    });

    await component.enviarParaAvaliacao();

    expect(mockProjetoService.criarProjeto).not.toHaveBeenCalled();
    expect(component.erroMensagem()).toContain('10MB');
  });

  it('deve barrar o envio com capa embutida em base64 (upload falhou)', async () => {
    component.titulo = 'Projeto Teste';
    component.descricaoCurta = 'Descrição curta';
    component.instituicaoId = 'inst-1';
    component.professorEmail = 'prof@cps.sp.gov.br';
    component.integrantes = [{ id: 1, nome: 'Aluno', linkedin: '' }];
    component.capaArquivo = null;
    component.imagemCapaUrl = 'data:image/png;base64,AAAA';

    await component.enviarParaAvaliacao();

    expect(mockProjetoService.criarProjeto).not.toHaveBeenCalled();
    expect(component.erroMensagem()).toContain('embutida');
  });
});

/**
 * Item 20: debounce com cancelamento — Subject + debounceTime(250) + switchMap.
 * Só a última digitação dispara GET /professores; resposta tardia não
 * sobrescreve a mais recente.
 */
describe('UploadProjeto (debounce com cancelamento, item 20)', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    buscarProfessores: vi.fn(() => of([{ id: 'p1', nome: 'Maria Silva' }])),
    obterProfessorPorId: vi.fn(() =>
      of({ id: 'p1', nome: 'Maria Silva', email: 'maria@cps.sp.gov.br' })
    ),
    uploadImagem: () => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    criarProjeto: () => of({ id: '123', titulo: 'Teste' }),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  let destruido = false;

  afterEach(() => {
    if (!destruido) {
      fixture.destroy();
    }
  });

  it('só a última digitação dentro da janela dispara a busca (switchMap)', async () => {
    component.professorEmail = 'mar';
    component.onProfessorEmailInput();
    await new Promise((r) => setTimeout(r, 100));
    component.professorEmail = 'mari';
    component.onProfessorEmailInput();

    expect(mockProjetoService.buscarProfessores).not.toHaveBeenCalled();
    await new Promise((r) => setTimeout(r, 350));

    expect(mockProjetoService.buscarProfessores).toHaveBeenCalledTimes(1);
    expect(mockProjetoService.buscarProfessores).toHaveBeenCalledWith('mari');
    expect(component.mostrarDropdownProfessor()).toBe(true);
  });

  it('não repete a busca para o mesmo termo em sequência (distinctUntilChanged)', async () => {
    component.professorEmail = 'maria';
    component.onProfessorEmailInput();
    await new Promise((r) => setTimeout(r, 350));
    expect(mockProjetoService.buscarProfessores).toHaveBeenCalledTimes(1);

    component.professorEmail = 'maria';
    component.onProfessorEmailInput();
    await new Promise((r) => setTimeout(r, 350));
    expect(mockProjetoService.buscarProfessores).toHaveBeenCalledTimes(1);
  });

  it('cancela a inscrição no destroy (sem busca após ngOnDestroy)', async () => {
    fixture.destroy();
    destruido = true;
    component.professorEmail = 'maria';
    component.onProfessorEmailInput();
    await new Promise((r) => setTimeout(r, 350));

    expect(mockProjetoService.buscarProfessores).not.toHaveBeenCalled();
  });
});
